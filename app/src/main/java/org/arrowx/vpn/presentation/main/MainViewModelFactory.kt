package org.arrowx.vpn.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.arrowx.vpn.data.repository.ArrowRepository

class MainViewModelFactory(
    private val repository: ArrowRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java))
        return MainViewModel(repository, appContext) as T
    }
}
