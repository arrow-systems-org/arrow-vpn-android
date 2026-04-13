package org.arrowx.vpn.presentation.splash

import org.arrowx.vpn.domain.model.StartupData

data class SplashUiState(
    val title: String = "",
    val detail: String = "",
    val isLoading: Boolean = true,
    val startupData: StartupData? = null
)
