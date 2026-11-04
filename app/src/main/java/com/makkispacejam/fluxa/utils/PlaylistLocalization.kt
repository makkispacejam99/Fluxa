package com.makkispacejam.fluxa.utils

import android.content.Context
import com.makkispacejam.fluxa.R

fun Context.localizedPlaylistName(name: String): String = when (name) {
    "Historial" -> getString(R.string.historial)
    "Ver más tarde" -> getString(R.string.watch_later)
    "Favoritos" -> getString(R.string.favoritos)
    else -> name
}