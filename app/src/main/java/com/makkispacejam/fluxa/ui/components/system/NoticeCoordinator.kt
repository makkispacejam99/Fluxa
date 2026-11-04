package com.makkispacejam.fluxa.ui.components.system

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object NoticeCoordinator {
    var activeOwner by mutableStateOf<String?>(null)
        private set

    fun tryAcquire(owner: String): Boolean {
        val current = activeOwner
        if (current == null || current == owner) {
            activeOwner = owner
            return true
        }
        return false
    }

    fun release(owner: String) {
        if (activeOwner == owner) activeOwner = null
    }
}
