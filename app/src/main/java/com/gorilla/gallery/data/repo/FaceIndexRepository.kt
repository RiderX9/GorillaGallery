package com.gorilla.gallery.data.repo

import com.gorilla.gallery.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FaceIndexRepository(
    database: AppDatabase,
    scope: CoroutineScope,
) {
    private val dao = database.faceIndexDao()
    private val _selfiePaths = MutableStateFlow<Set<String>>(emptySet())
    val selfiePaths: StateFlow<Set<String>> = _selfiePaths.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                refresh()
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    suspend fun refresh() {
        _selfiePaths.value = dao.selfieImagePaths()
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 10_000L
    }
}
