package com.gorilla.gallery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.gorilla.gallery.AppContainer
import com.gorilla.gallery.GalleryApp

/** Pulls the [AppContainer] out of CreationExtras for ViewModel factories. */
val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as GalleryApp).container

/** Convenience for building a factory from a single creator lambda. */
inline fun <reified T : ViewModel> viewModelFactory(
    crossinline creator: (AppContainer) -> T,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <VM : ViewModel> create(
        modelClass: Class<VM>,
        extras: CreationExtras,
    ): VM {
        @Suppress("UNCHECKED_CAST")
        return creator(extras.container) as VM
    }
}
