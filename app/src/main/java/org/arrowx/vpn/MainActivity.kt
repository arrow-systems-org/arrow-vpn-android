package org.arrowx.vpn

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import org.arrowx.vpn.data.local.AppPreferences
import org.arrowx.vpn.data.remote.ArrowApiClient
import org.arrowx.vpn.data.repository.ArrowRepositoryImpl
import org.arrowx.vpn.presentation.main.MainScreen
import org.arrowx.vpn.presentation.main.MainUiEffect
import org.arrowx.vpn.presentation.main.MainViewModel
import org.arrowx.vpn.presentation.main.MainViewModelFactory
import org.arrowx.vpn.presentation.splash.SplashScreen
import org.arrowx.vpn.presentation.splash.SplashViewModel
import org.arrowx.vpn.presentation.splash.SplashViewModelFactory
import org.arrowx.vpn.ui.theme.ArrowVpnTheme

class MainActivity : ComponentActivity() {
    private val appVersion: String by lazy {
        runCatching {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { getString(R.string.app_version_fallback) }
    }
    private val repository by lazy {
        ArrowRepositoryImpl(
            context = applicationContext,
            apiClient = ArrowApiClient(),
            preferences = AppPreferences(applicationContext)
        )
    }

    private val splashViewModel by viewModels<SplashViewModel> {
        SplashViewModelFactory(repository, applicationContext)
    }

    private val mainViewModel by viewModels<MainViewModel> {
        MainViewModelFactory(repository, applicationContext)
    }

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            mainViewModel.onVpnPermissionResult(result.resultCode == RESULT_OK)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        configureSystemBars()
        setContent {
            ArrowVpnTheme {
                val splashState = splashViewModel.uiState.collectAsStateWithLifecycle().value
                val mainState = mainViewModel.uiState.collectAsStateWithLifecycle().value

                LaunchedEffect(splashState.startupData) {
                    splashState.startupData?.let(mainViewModel::initialize)
                }

                LaunchedEffect(Unit) {
                    mainViewModel.effects.collect { effect ->
                        when (effect) {
                            is MainUiEffect.ShowToast -> {
                                showStyledToast(
                                    message = effect.message,
                                    isError = effect.isError
                                )
                            }

                            MainUiEffect.RequestVpnPermission -> {
                                requestVpnPermission()
                            }

                            is MainUiEffect.StartVpn -> {
                                startVpnService(effect.vlessConfig)
                            }

                            MainUiEffect.StopVpn -> {
                                stopVpnService()
                                mainViewModel.onVpnStopped()
                            }

                            is MainUiEffect.CopyToClipboard -> {
                                copyToClipboard(effect.value)
                                showStyledToast(effect.successMessage, isError = false)
                            }
                        }
                    }
                }

                if (splashState.isLoading || !mainState.initialized) {
                    SplashScreen(uiState = splashState)
                } else {
                    MainScreen(
                        uiState = mainState,
                        appVersion = appVersion,
                        onPowerTapped = mainViewModel::onPowerTapped,
                        onModeSelected = mainViewModel::onModeSelected,
                        onToggleServerMenu = mainViewModel::onToggleServerMenu,
                        onSelectServer = mainViewModel::onServerSelected,
                        onRefreshPings = mainViewModel::refreshPings,
                        onOpenLogin = mainViewModel::onOpenLogin,
                        onDismissLogin = mainViewModel::onDismissLogin,
                        onLoginUuidChanged = mainViewModel::onLoginUuidChanged,
                        onLoginPasswordChanged = mainViewModel::onLoginPasswordChanged,
                        onLoginSubmit = mainViewModel::onLoginSubmit,
                        onOpenSupportPanel = mainViewModel::onOpenSupportPanel,
                        onOpenSettingsPanel = mainViewModel::onOpenSettingsPanel,
                        onOpenCredentialsPanel = mainViewModel::onOpenCredentialsPanel,
                        onBackToSettingsPanel = mainViewModel::onBackToSettingsPanel,
                        onClosePanels = mainViewModel::onClosePanels,
                        onToggleAutoConnect = mainViewModel::onToggleAutoConnect,
                        onToggleKillSwitch = mainViewModel::onToggleKillSwitch,
                        onLogout = mainViewModel::onLogout,
                        onCopySessionId = mainViewModel::onCopySessionId,
                        onCopySupportEmail = mainViewModel::onCopySupportEmail,
                        onCopySubscription = mainViewModel::onCopySubscriptionLink,
                        onCopyServerVless = mainViewModel::onCopyServerVless
                    )
                }
            }
        }
    }

    private fun configureSystemBars() {
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            mainViewModel.onVpnPermissionResult(true)
        }
    }

    private fun startVpnService(vlessConfig: String) {
        val serviceIntent = Intent(this, ArrowVpnService::class.java).apply {
            putExtra(EXTRA_VLESS_CONFIG, vlessConfig)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopVpnService() {
        val serviceIntent = Intent(this, ArrowVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun copyToClipboard(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(getString(R.string.clipboard_label), value)
        )
    }

    private fun showStyledToast(message: String, isError: Boolean) {
        val toastText = TextView(this).apply {
            text = message
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.arrow_toast_text))
            textSize = 13f
            setPadding(28, 18, 28, 18)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        if (isError) R.color.arrow_toast_error else R.color.arrow_toast_success
                    )
                )
            }
        }
        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            view = toastText
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 180)
        }.show()
    }

    companion object {
        private const val EXTRA_VLESS_CONFIG = "VLESS_CONFIG"
        private const val ACTION_DISCONNECT = "DISCONNECT"
    }
}