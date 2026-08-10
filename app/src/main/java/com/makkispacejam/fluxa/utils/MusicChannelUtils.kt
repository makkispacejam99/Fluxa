package com.makkispacejam.fluxa.utils

object MusicChannelUtils {

    fun isMusicChannel(channelName: String): Boolean {
        val lower = channelName.lowercase()
        return lower.contains("topic") ||
            lower.contains("vevo") ||
            lower.contains("records") ||
            lower.contains("recordings") ||
            lower.contains("lyrics") ||
            lower.contains("music")
    }

    private val musicTitleRegex = Regex(
        """(?i)\b(""" +
            """official\s+(?:video|audio|music|lyric)|""" +
            """lyric\s+video|""" +
            """live\s+performance|""" +
            """full\s+(?:album|set|live)|""" +
            """radio\s+edit|""" +
            """bonus\s+track|""" +
            """a\s+cappella|""" +
            """remix|mashup|bootleg|cover|acoustic|instrumental|karaoke|""" +
            """tribute|visualizer|premiere|session|unplugged|performance|""" +
            """remaster|deluxe|single|album|audio|lyrics|""" +
            """feat\.?|ft\.?|vip|""" +
            """edit|live|ep|id""" +
            """)\b"""
    )

    private val multiArtistRegex = Regex(
        """(?i)\b\w[\w\s.&'-]*,\s*\w[\w\s.&'-]*\s+-\s+\w"""
    )

    fun isMusicTitle(title: String): Boolean {
        return musicTitleRegex.containsMatchIn(title) || multiArtistRegex.containsMatchIn(title)
    }

    fun isMusicContent(channelName: String, title: String): Boolean {
        return isMusicChannel(channelName) || isMusicTitle(title)
    }
}
