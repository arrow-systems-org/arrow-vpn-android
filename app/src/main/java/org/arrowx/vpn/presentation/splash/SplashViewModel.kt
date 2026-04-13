package org.arrowx.vpn.presentation.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.arrowx.vpn.R
import org.arrowx.vpn.data.repository.ArrowRepository

class SplashViewModel(
    private val repository: ArrowRepository,
    appContext: Context
) : ViewModel() {
    private val applicationContext = appContext.applicationContext
    private val _uiState = MutableStateFlow(
        SplashUiState(
            title = stringRes(R.string.app_name),
            detail = stringRes(R.string.splash_initializing_secure_system)
        )
    )
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        preload()
    }

    private fun preload() {
        viewModelScope.launch {
            _uiState.update { it.copy(detail = stringRes(R.string.splash_restoring_local_session)) }
            val minimumSplashTime = async { delay(MINIMUM_SPLASH_TIME_MS) }
            val startupData = runCatching {
                _uiState.update { it.copy(detail = stringRes(R.string.splash_syncing_session_servers)) }
                repository.preloadStartupData()
            }.getOrElse {
                repository.loadCachedStartupData()
            }
            minimumSplashTime.await()

            _uiState.value = SplashUiState(
                title = stringRes(R.string.app_name),
                detail = stringRes(R.string.splash_ready),
                isLoading = false,
                startupData = startupData
            )
        }
    }

    private fun stringRes(resId: Int): String = applicationContext.getString(resId)

    companion object {
        private const val MINIMUM_SPLASH_TIME_MS = 1_100L
    }
}
