package org.arrowx.vpn.presentation.main

sealed interface MainUiEffect {
    data class ShowToast(val message: String, val isError: Boolean = false) : MainUiEffect
    data object RequestVpnPermission : MainUiEffect
    data class StartVpn(val vlessConfig: String) : MainUiEffect
    data object StopVpn : MainUiEffect
    data class CopyToClipboard(val value: String, val successMessage: String) : MainUiEffect
}
