package com.subzero.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subzero.app.ui.SubzeroApp
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.ThemeMode
import com.subzero.core.notifications.NotificationDeepLink
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the splash until preferences are read so onboarding never flashes behind Home.
        splash.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        enableEdgeToEdge()
        viewModel.openSubscription(NotificationDeepLink.subscriptionIdFrom(intent))
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val pendingSubscriptionId by viewModel.pendingSubscriptionId.collectAsStateWithLifecycle()
            val themeMode = (uiState as? MainUiState.Ready)?.themeMode ?: ThemeMode.SYSTEM
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            SubzeroTheme(darkTheme = darkTheme) {
                SubzeroApp(
                    uiState = uiState,
                    pendingSubscriptionId = pendingSubscriptionId,
                    onPendingSubscriptionOpened = viewModel::consumePendingSubscription,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.openSubscription(NotificationDeepLink.subscriptionIdFrom(intent))
    }
}
