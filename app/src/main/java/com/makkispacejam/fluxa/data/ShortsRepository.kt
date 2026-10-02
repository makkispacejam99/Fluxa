package com.makkispacejam.fluxa.data

import android.content.Context
import android.util.Log
import com.makkispacejam.fluxa.data.local.FluxaDao
import com.makkispacejam.fluxa.data.local.FluxaDatabase
import com.makkispacejam.fluxa.data.shorts.ShortSource
import com.makkispacejam.fluxa.data.shorts.ShortsCache
import com.makkispacejam.fluxa.data.shorts.ShortsCacheManager
import com.makkispacejam.fluxa.data.shorts.ShortsClassifier
import com.makkispacejam.fluxa.data.shorts.ShortsGraph
import com.makkispacejam.fluxa.data.shorts.ShortsRanker
import com.makkispacejam.fluxa.data.shorts.ShortsSeenRegistry
import com.makkispacejam.fluxa.data.shorts.ShortsSource
import com.makkispacejam.fluxa.data.shorts.ScoredShort
import com.makkispacejam.fluxa.models.VideoModel
import com.makkispacejam.fluxa.ui.components.system.NewPipeServerException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class ShortsRepository {

    private companion object {
        const val TAG = "FluxaShorts"
        const val SUBS_PER_CHANNEL_LOAD_MORE = 16
        const val SUBS_PER_CHANNEL_SHUFFLE = 20
    }

    fun hideShort(videoId: String) {
        ShortsSessionState.hide(videoId)
    }

    suspend fun getShorts(
        context: Context,
        forceLoadMore: Boolean = false,
        forceRefresh: Boolean = false,
        onProgress: ((List<VideoModel>) -> Unit)? = null
    ): List<VideoModel> = withContext(Dispatchers.IO) {

        ShortsSeenRegistry.attach(context)
        val dao = FluxaDatabase.getDatabase(context).fluxaDao()
        val seed = ShortsSessionState.beginSession()

        if (forceRefresh) {
            ShortsSessionState.invalidateCache()
            ShortsCacheManager.clearCache()
            ShortsSessionState.clearPool()
        }

        if (!forceRefresh && !forceLoadMore) {
            ShortsSessionState.freshCache()?.let { cached ->
                val blocked = dao.getBlockedChannelIds().toSet()
                val page = cached
                    .filter { it.id !in ShortsSessionState.hiddenShorts && it.channelId !in blocked }
                    .let { ShortsSessionState.shuffleWithSession(it, seed) }
                    .take(ShortsSessionState.PAGE_SIZE)
                if (page.isNotEmpty()) return@withContext page
            }
        }

        if (forceLoadMore) {
            val ready = ShortsSessionState.drain(ShortsSessionState.PAGE_SIZE)
            if (ready.size >= ShortsSessionState.PAGE_SIZE / 2) return@withContext ready
        }

        val signals = readSignals(dao)
        val session = FeedSession(dao, signals, onProgress)

        session.add(session.instantCache())

        val subsJob = async(Dispatchers.IO) {
            runCatching {
                ShortsSource.getSubscriptionShorts(
                    dao = dao,
                    blockedChannelIds = signals.blockedChannelIds,
                    onBatch = { batch ->
                        batch.forEach { item ->
                            ShortsClassifier.keywordTokens(item.video.title).forEach { token ->
                                signals.subscriptionVocabulary.add(token)
                            }
                        }
                        session.add(batch)
                    }
                )
            }.getOrElse {
                Log.d(TAG, "shorts de suscripciones fallo: ${it.message}")
                emptyList()
            }
        }

        val relatedJob = async(Dispatchers.IO) {
            runCatching {
                ShortsGraph.expand(
                    seeds = signals.seeds,
                    maxLevel2PerSeed = 3,
                    maxLevel2Total = 10,
                    maxConcurrent = 6,
                    onLevel1 = { level1 -> session.add(level1) },
                    accept = { item -> session.acceptsRelated(item) }
                )
            }.getOrElse {
                Log.d(TAG, "expansion de related fallo: ${it.message}")
                emptyList()
            }
        }

        val topicsJob = async(Dispatchers.IO) {
            runCatching {
                ShortsSource.getTopicShorts(
                    queries = buildQueries(signals),
                    blockedChannelIds = signals.blockedChannelIds,
                    dislikedChannelNames = signals.dislikedChannelNames
                )
            }.getOrElse {
                Log.d(TAG, "busquedas por tema fallaron: ${it.message}")
                emptyList()
            }
        }

        awaitAll(subsJob, relatedJob, topicsJob)

        if (session.size() < ShortsSessionState.PAGE_SIZE && !signals.hasSubscriptions) {
            runCatching {
                ShortsSource.getDiscoveryShorts(
                    blockedChannelIds = signals.blockedChannelIds,
                    dislikedChannelNames = signals.dislikedChannelNames
                )
            }.getOrElse { emptyList() }.let { session.add(it) }
        }

        finalize(dao, session, signals, forceLoadMore)
    }

    suspend fun loadMore(
        context: Context,
        exclude: Set<String> = emptySet(),
        size: Int = ShortsSessionState.PAGE_SIZE
    ): List<VideoModel> = withContext(Dispatchers.IO) {
        ShortsSeenRegistry.attach(context)
        ShortsSessionState.beginSession()

        val ready = ShortsSessionState.drain(size + exclude.size).filterNot { it.id in exclude }
        if (ready.size >= size / 2) {
            val page = ready.take(size)
            ShortsSessionState.refill(ready.drop(size))
            return@withContext page
        }

        val dao = FluxaDatabase.getDatabase(context).fluxaDao()
        val signals = readSignals(dao)
        val session = FeedSession(dao, signals, null)
        session.addUnchecked(ready.map { ScoredShort(it, 0.0, ShortSource.SESSION_POOL) })

        val subs = signals.subscribedChannelIds

        coroutineScope {
            val jobs = mutableListOf<kotlinx.coroutines.Deferred<List<ScoredShort>>>()

            if (signals.hasSubscriptions) {
                jobs += async(Dispatchers.IO) {
                    runCatching {
                        ShortsSource.getSubscriptionShorts(
                            dao = dao,
                            blockedChannelIds = signals.blockedChannelIds,
                            perChannel = SUBS_PER_CHANNEL_LOAD_MORE,
                            minTotal = size * 2,
                            onBatch = { batch ->
                                batch.forEach { item ->
                                    ShortsClassifier.keywordTokens(item.video.title).forEach { token ->
                                        signals.subscriptionVocabulary.add(token)
                                    }
                                }
                                session.add(batch)
                            }
                        )
                    }.getOrElse {
                        Log.d(TAG, "shorts de suscripciones fallo: ${it.message}")
                        emptyList()
                    }
                }
            }

            jobs += async(Dispatchers.IO) {
                runCatching {
                    ShortsGraph.expand(
                        seeds = signals.seeds,
                        maxLevel2PerSeed = 3,
                        maxLevel2Total = 10,
                        maxConcurrent = 6,
                        accept = { item ->
                            val id = VideoExtractor.cleanVideoId(item.url ?: "")
                            ShortsGraph.isUsableRelated(item) &&
                                id !in exclude &&
                                id !in ShortsSessionState.hiddenShorts &&
                                !ShortsSeenRegistry.isServedWithin(
                                    id, ShortsSeenRegistry.WINDOW_SHADOW
                                )
                        }
                    )
                }.getOrElse { emptyList() }.also { session.addUnchecked(it) }
            }

            jobs += async(Dispatchers.IO) {
                runCatching {
                    ShortsSource.getTopicShorts(
                        queries = buildQueries(signals),
                        blockedChannelIds = signals.blockedChannelIds,
                        dislikedChannelNames = signals.dislikedChannelNames
                    )
                }.getOrElse { emptyList() }.also { session.addUnchecked(it) }
            }

            awaitAll(*jobs.toTypedArray())
        }

        val ranked = ShortsRanker.rank(
            session.poolSnapshot(),
            signals.taste,
            sessionSeed = ShortsSessionState.sessionSeed
        )
        var batch = ShortsRanker
            .select(
                ranked,
                size,
                subscribedChannelIds = subs,
                sessionSeed = ShortsSessionState.sessionSeed
            )
            .map { it.video }
        if (batch.isEmpty()) {
            batch = cacheFallback(dao, signals, size, exclude)
        }
        if (batch.isNotEmpty()) {
            val batchIds = batch.mapTo(HashSet()) { it.id }
            ShortsSessionState.refill(
                ShortsRanker
                    .select(
                        ranked,
                        ShortsSessionState.POOL_TARGET,
                        subscribedChannelIds = subs,
                        sessionSeed = ShortsSessionState.sessionSeed
                    )
                    .map { it.video }
                    .filterNot { it.id in batchIds }
            )
            ShortsSeenRegistry.markServed(batch)
            ShortsCacheManager.markAsSeen(batch)
            ShortsCache.saveToDiskCache(dao, batch)
        }
        batch
    }

    suspend fun shuffleShorts(context: Context, size: Int = ShortsSessionState.PAGE_SIZE): List<VideoModel> =
        withContext(Dispatchers.IO) {
            ShortsSeenRegistry.attach(context)
            ShortsSessionState.beginSession()
            ShortsSessionState.invalidateCache()

            val ready = ShortsSessionState.drain(size * 2)
            if (ready.size >= size) {
                val page = ready.take(size)
                ShortsSessionState.refill(ready.drop(size))
                return@withContext page
            }

            val dao = FluxaDatabase.getDatabase(context).fluxaDao()
            val signals = readSignals(dao)
            val session = FeedSession(dao, signals, null)
            session.add(ready.map { video ->
                ScoredShort(
                    video = video,
                    score = 0.0,
                    source = ShortSource.SESSION_POOL,
                    originChannelId = video.channelId.orEmpty(),
                    originTitle = video.title
                )
            })

            val jobs = mutableListOf<kotlinx.coroutines.Deferred<List<ScoredShort>>>()
            jobs.add(async(Dispatchers.IO) {
                runCatching {
                    ShortsSource.getSubscriptionShorts(
                        dao = dao,
                        blockedChannelIds = signals.blockedChannelIds,
                        maxChannels = 30,
                        perChannel = SUBS_PER_CHANNEL_SHUFFLE,
                        timeoutMs = 5000,
                        overallTimeoutMs = 9_000,
                        minTotal = size * 2
                    )
                }.getOrElse { emptyList() }.also { session.addUnchecked(it) }
            })
            jobs.add(async(Dispatchers.IO) {
                runCatching {
                    ShortsSource.getTopicShorts(
                        queries = buildQueries(signals).take(3),
                        blockedChannelIds = signals.blockedChannelIds,
                        dislikedChannelNames = signals.dislikedChannelNames,
                        timeoutMs = 3500
                    )
                }.getOrElse { emptyList() }.also { session.addUnchecked(it) }
            })

            kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                jobs.awaitAll()
            }

            val subs = signals.subscribedChannelIds
            val seed = ShortsSessionState.sessionSeed
            val ranked = ShortsRanker.rank(session.poolSnapshot(), signals.taste, sessionSeed = seed)
            var page = ShortsRanker
                .select(ranked, size, subscribedChannelIds = subs, sessionSeed = seed)
                .map { it.video }
            if (page.isEmpty()) {
                val relaxed = ShortsRanker.rankRelaxed(session.poolSnapshot(), signals.taste, seed)
                page = ShortsRanker
                    .select(relaxed, size, subscribedChannelIds = subs, sessionSeed = seed)
                    .map { it.video }
            }
            if (page.isEmpty()) {
                page = ready.drop((ready.size - size).coerceAtLeast(0))
            }
            if (page.isEmpty()) {
                page = cacheFallback(dao, signals, size, emptySet())
            }
            if (page.isEmpty()) return@withContext emptyList()

            val pageIds = page.mapTo(HashSet()) { it.id }
            ShortsSessionState.refill(
                ShortsRanker
                    .select(ranked, size * 4, subscribedChannelIds = subs, sessionSeed = seed)
                    .map { it.video }
                    .filterNot { it.id in pageIds }
            )
            ShortsSeenRegistry.markServed(page)
            ShortsCacheManager.markAsSeen(page)
            ShortsCache.saveToDiskCache(dao, page)
            page
        }

    private suspend fun finalize(
        dao: FluxaDao,
        session: FeedSession,
        signals: Signals,
        forceLoadMore: Boolean
    ): List<VideoModel> {
        val subs = signals.subscribedChannelIds
        val seed = ShortsSessionState.sessionSeed
        Log.d(TAG, "finalize: ${session.summary()} subs=${subs.size} pagina=${ShortsSessionState.PAGE_SIZE}")
        val ranked = ShortsRanker.rank(session.poolSnapshot(), signals.taste, sessionSeed = seed)
        var page = ShortsRanker
            .select(
                ranked,
                ShortsSessionState.PAGE_SIZE,
                subscribedChannelIds = subs,
                sessionSeed = seed
            )
            .map { it.video }

        if (page.isEmpty()) {
            val relaxed = ShortsRanker.rankRelaxed(session.poolSnapshot(), signals.taste, seed)
            page = ShortsRanker
                .select(
                    relaxed,
                    ShortsSessionState.PAGE_SIZE,
                    subscribedChannelIds = subs,
                    sessionSeed = seed
                )
                .map { it.video }
        }
        if (page.isEmpty()) {
            page = session.poolSnapshot().shuffled().take(ShortsSessionState.PAGE_SIZE).map { it.video }
        }
        if (page.isEmpty()) {
            page = cacheFallback(dao, signals, ShortsSessionState.PAGE_SIZE, emptySet())
        }
        if (page.isEmpty()) {
            Log.w(TAG, "finalize: sin resultados, subs=${subs.size} poolVacio=${session.size() == 0}")
            throw NewPipeServerException()
        }

        val pageIds = page.mapTo(HashSet()) { it.id }
        val rest = ShortsRanker
            .select(
                ranked,
                ShortsSessionState.POOL_TARGET,
                subscribedChannelIds = subs,
                sessionSeed = seed
            )
            .map { it.video }
            .filterNot { it.id in pageIds }

        ShortsSessionState.refill(rest)
        ShortsSeenRegistry.markServed(page)
        ShortsCacheManager.markAsSeen(page)

        if (!forceLoadMore) {
            ShortsSessionState.storeCache(page + rest)
        }
        ShortsCache.saveToDiskCache(dao, page + rest)
        return page
    }

    private fun cacheFallback(
        dao: FluxaDao,
        signals: Signals,
        size: Int,
        exclude: Set<String>
    ): List<VideoModel> {
        val cached = runCatching { dao.getRecentCachedVideos(120) }.getOrElse { emptyList() }
        if (cached.isEmpty()) return emptyList()
        val windows = longArrayOf(
            ShortsSeenRegistry.WINDOW_SHADOW,
            ShortsSeenRegistry.WINDOW_3_DAYS,
            ShortsSeenRegistry.WINDOW_HARD
        )
        for (window in windows) {
            val page = cached
                .filter { entity ->
                    val id = entity.videoId
                    val channelId = ShortsClassifier.normalizeChannelId(entity.channelId)
                    id.isNotBlank() &&
                        id !in exclude &&
                        id !in signals.excludedIds &&
                        id !in ShortsSessionState.hiddenShorts &&
                        (channelId.isEmpty() || channelId !in signals.blockedChannelIds) &&
                        entity.channelName?.trim()?.lowercase() !in signals.dislikedNames &&
                        !ShortsSeenRegistry.isServedWithin(id, window)
                }
                .shuffled()
                .take(size)
                .map { entity -> ShortsCache.toVideoModel(entity) }
            if (page.isNotEmpty()) return page
        }
        return emptyList()
    }
}
