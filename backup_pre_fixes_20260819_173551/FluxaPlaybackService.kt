@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import android.os.Looper
import androidx.core.net.toUri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.data.local.FluxaDatabase
import com.makkispacejam.fluxa.data.local.VideoInteractionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
class FluxaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    private var inactivityJob: Job? = null
    private var pendingPlaybackJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val timeout = 30 * 60 * 1000L

    private val httpDataSourceFactory by lazy { PlayerDataSources.buildHttpDataSourceFactory() }
    private val subtitleDataSourceFactory by lazy { PlayerDataSources.buildSubtitleDataSourceFactory() }
    private val chunkedDataSourceFactory by lazy { PlayerDataSources.buildChunkedDataSourceFactory(httpDataSourceFactory) }
    private val mediaSourceFactory by lazy { PlayerDataSources.buildMediaSourceFactory(subtitleDataSourceFactory, this) }

    companion object {
        var instance: FluxaPlaybackService? = null
            private set
    }

    private var currentLoadedUrl: String? = null
    private var currentVideoId: String = ""

    var onSkipNext: (() -> Unit)? = null
    var onSkipPrevious: (() -> Unit)? = null

    override fun onCreate() {
        instance = this
        super.onCreate()
        createNotificationChannel()
        initExoPlayer()
        startBufferingWatchdog()
    }

    private fun initExoPlayer() {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, 60_000, 2_500, 5_000)
            .setBackBuffer(0, false)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            @OptIn(ExperimentalApi::class)
            override fun buildTextRenderers(
                context: android.content.Context, output: TextOutput, outputLooper: Looper,
                extensionRendererMode: Int, out: ArrayList<Renderer>
            ) {
                super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, out)
                @Suppress("DEPRECATION")
                (out.last() as? TextRenderer)?.experimentalSetLegacyDecodingEnabled(true)
            }
        }

        exoPlayer = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setLoadControl(loadControl)
            .build()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("open_player", true)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val availablePlayerCommands = Player.Commands.Builder()
                        .addAll(super.onConnect(session, controller).availablePlayerCommands)
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .build()
                    return MediaSession.ConnectionResult.accept(
                        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
                        availablePlayerCommands
                    )
                }

                @Deprecated("Deprecated in Java")
                @Suppress("DEPRECATION")
                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    when (playerCommand) {
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                            onSkipNext?.invoke()
                            return SessionResult.RESULT_SUCCESS
                        }
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                            onSkipPrevious?.invoke()
                            return SessionResult.RESULT_SUCCESS
                        }
                    }
                    return super.onPlayerCommandRequest(session, controller, playerCommand)
                }

                override fun onAddMediaItems(
                    mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: List<MediaItem>
                ): ListenableFuture<List<MediaItem>> {
                    val resolvedItems = mediaItems.map { item ->
                        val uri = item.requestMetadata.mediaUri?.toString() ?: ""
                        if (uri.contains("|")) item.buildUpon().setUri(uri.split("|")[0]).build()
                        else item.buildUpon().setUri(uri).build()
                    }
                    return Futures.immediateFuture(resolvedItems)
                }
            })
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId("fluxa_playback_channel")
            .setChannelName(R.string.app_name)
            .build()
        notificationProvider.setSmallIcon(R.drawable.fluxa_icon)
        setMediaNotificationProvider(notificationProvider)

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) inactivityJob?.cancel() else startInactivityTimeout()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d("FluxaBuffering", "Video State: $playbackState (1=Idle, 2=Buffering, 3=Ready, 4=Ended)")
            }
            override fun onIsLoadingChanged(isLoading: Boolean) {
                Log.d("FluxaBuffering", "Video Loading: $isLoading | Buffered: ${exoPlayer.bufferedPosition / 1000}s")
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("FluxaBuffering", "Video ERROR: ${error.errorCodeName} - ${error.message}")
                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
                    Log.w("FluxaBuffering", "Decoder init failed - reiniciando stream")
                    exoPlayer.stop(); exoPlayer.prepare()
                }
            }
        })
    }

    private fun startBufferingWatchdog() {
        serviceScope.launch {
            var lastBuffered = 0L
            var stallCount = 0
            while (true) {
                val player = exoPlayer
                val currentBuffered = player.bufferedPosition
                val currentPos = player.currentPosition
                val delta = currentBuffered - lastBuffered

                if (player.playbackState == Player.STATE_BUFFERING && !player.isCurrentMediaItemLive) {
                    if (delta <= 0L) {
                        stallCount++
                        if (stallCount >= 4) {
                            Log.w("FluxaBuffering", "[WATCHDOG] Stall crítico en ${currentPos}ms - Forzando reinicio de red")
                            val posToRestore = player.currentPosition
                            player.stop(); player.prepare(); player.seekTo(posToRestore); player.play()
                            stallCount = 0
                        }
                    } else { stallCount = 0 }
                } else { stallCount = 0 }
                lastBuffered = currentBuffered
                delay(700)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("fluxa_playback_channel", "Fluxa Playback", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Fluxa Media Playback Notifications"
            setSound(null, null); setShowBadge(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun startInactivityTimeout() {
        inactivityJob?.cancel()
        inactivityJob = serviceScope.launch {
            delay(timeout)
            if (!exoPlayer.isPlaying && exoPlayer.mediaItemCount == 0) stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    fun loadUrl(
        videoUrl: String, title: String = "", channel: String = "", thumbnailUrl: String = "",
        startPositionMs: Long = 0L, subtitles: List<MediaItem.SubtitleConfiguration> = emptyList(),
        playImmediately: Boolean = false, videoId: String = "",
        queueSize: Int = 0, currentIndex: Int = 0
    ) {
        if (videoUrl.isEmpty()) return
        currentLoadedUrl = videoUrl
        currentVideoId = videoId
        inactivityJob?.cancel()
        pendingPlaybackJob?.cancel()

        val metadata = MediaMetadata.Builder()
            .setTitle(title).setArtist(channel).setAlbumArtist(channel).setDisplayTitle(title)
            .setArtworkUri(if (thumbnailUrl.isNotEmpty()) thumbnailUrl.toUri() else null).build()

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(videoUrl)
            .setMediaMetadata(metadata)
            .setSubtitleConfigurations(subtitles)

        val isHls = videoUrl.contains("manifest/hls") || videoUrl.endsWith(".m3u8")

        val finalSource = if (isHls) {
            HlsMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(mediaItemBuilder.build())
        } else if (videoUrl.contains("|")) {
            val parts = videoUrl.split("|")
            val videoSource = ProgressiveMediaSource.Factory(chunkedDataSourceFactory)
                .createMediaSource(MediaItem.Builder().setUri(parts[0]).setMediaMetadata(metadata).build())
            val audioSource = ProgressiveMediaSource.Factory(chunkedDataSourceFactory)
                .createMediaSource(MediaItem.Builder().setUri(parts[1]).setMediaMetadata(metadata).build())

            if (subtitles.isNotEmpty()) {
                val subFactory = SingleSampleMediaSource.Factory(subtitleDataSourceFactory).setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(7))
                val subSources = subtitles.map { sub -> subFactory.createMediaSource(sub, C.TIME_UNSET) }
                MergingMediaSource(false, videoSource, audioSource, *subSources.toTypedArray())
            } else { MergingMediaSource(false, videoSource, audioSource) }
        } else {
            mediaSourceFactory.createMediaSource(mediaItemBuilder.build())
        }

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        
        if (queueSize > 1) {
            val sources = mutableListOf<androidx.media3.exoplayer.source.MediaSource>()
            repeat(queueSize) { i ->
                if (i == currentIndex) {
                    sources.add(finalSource)
                } else {
                    val placeholderItem = MediaItem.Builder()
                        .setMediaId("placeholder_$i")
                        .setUri("http://placeholder.com/$i")
                        .build()
                    sources.add(ProgressiveMediaSource.Factory(httpDataSourceFactory).createMediaSource(placeholderItem))
                }
            }
            exoPlayer.setMediaSources(sources, currentIndex, startPositionMs)
        } else {
            exoPlayer.playlistMetadata = metadata
            exoPlayer.setMediaSource(finalSource, startPositionMs)
        }

        exoPlayer.prepare()

        if (playImmediately) {
            exoPlayer.playWhenReady = true
        } else {
            pendingPlaybackJob = serviceScope.launch {
                val deadline = System.currentTimeMillis() + 5_000L
                while (System.currentTimeMillis() < deadline) {
                    if (exoPlayer.bufferedPosition - startPositionMs >= 6_000L) break
                    if (exoPlayer.isCurrentMediaItemLive) break
                    delay(150)
                }
                exoPlayer.playWhenReady = true
            }
        }
    }

    fun getPlayer() = exoPlayer

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun saveProgressToDatabase() {
        if (UserPreferences.incognitoActive) return
        try {
            val videoId = currentVideoId
            if (videoId.isBlank()) return
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (dur <= 0 || pos <= 0 || pos < 12_000L) return

            val dao = FluxaDatabase.getDatabase(this).fluxaDao()
            val mediaItem = exoPlayer.currentMediaItem
            val title = mediaItem?.mediaMetadata?.title?.toString() ?: ""
            val channel = mediaItem?.mediaMetadata?.artist?.toString() ?: ""

            serviceScope.launch(Dispatchers.IO) {
                val existing = dao.getInteraction(videoId)
                if (existing != null) {
                    dao.updateProgress(videoId, pos, dur, System.currentTimeMillis())
                } else if (title.isNotBlank() || channel.isNotBlank()) {
                    dao.insertOrUpdateInteraction(VideoInteractionEntity(videoId).apply {
                        this.title = title; channelName = channel
                        viewCount = 1; progressMs = pos; durationMs = dur
                        lastWatchedAt = System.currentTimeMillis()
                    })
                }
            }
        } catch (_: Exception) {}
    }

    override fun onTaskRemoved(rootIntent: Intent?) { saveProgressToDatabase() }

    override fun onDestroy() {
        saveProgressToDatabase()
        instance = null
        inactivityJob?.cancel()
        serviceScope.cancel()
        exoPlayer.release()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
