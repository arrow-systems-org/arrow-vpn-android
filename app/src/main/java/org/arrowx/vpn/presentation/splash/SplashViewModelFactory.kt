package org.arrowx.vpn.presentation.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.arrowx.vpn.data.repository.ArrowRepository

class SplashViewModelFactory(
    private val repository: ArrowRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SplashViewModel::class.java))
        return SplashViewModel(repository, appContext) as T
    }
}
