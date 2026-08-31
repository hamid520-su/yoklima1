package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserRole
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.GroupLeadScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AttendanceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AttendanceViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val themePreset by viewModel.themePreset.collectAsState()

            MyApplicationTheme(
                darkTheme = isDarkMode,
                themePreset = themePreset
            ) {
                // Uyghur and Arabic both use Right-to-Left (RTL) typography and layout direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AttendanceApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceApp(viewModel: AttendanceViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()

    when (val user = currentUser) {
        null -> {
            LoginScreen(viewModel = viewModel)
        }
        else -> {
            if (user.role == UserRole.ADMIN) {
                AdminDashboardScreen(user = user, viewModel = viewModel)
            } else {
                GroupLeadScreen(user = user, viewModel = viewModel)
            }
        }
    }
}
