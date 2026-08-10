package com.makkispacejam.fluxa.models

import com.makkispacejam.fluxa.R

// Pantallas de navegación principal
enum class Screen(val titleRes: Int) {
    Home(R.string.nav_home),
    Shorts(R.string.nav_shorts),
    Library(R.string.nav_library),
    Settings(R.string.nav_settings)
}

enum class DetailView {
    None, Player, Channel, Playlist, Subscriptions, History
}
