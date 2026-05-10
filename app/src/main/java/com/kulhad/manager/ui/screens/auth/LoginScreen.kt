package com.kulhad.manager.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulhad.manager.ui.components.KulhadButton
import com.kulhad.manager.ui.components.KulhadTextField
import com.kulhad.manager.ui.theme.BgDeep
import com.kulhad.manager.ui.theme.BgLogin
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite15
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.PrimaryBlueDark
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (state is AuthUiState.Success) onLoggedIn()

    // Full-screen dark splash background (matches HTML screen 1)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLogin),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Logo — outer ring + inner square (HTML: logo-ring → logo-inner)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryBlueDark),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Factory,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Kulhad Manager",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.W500,
                letterSpacing = 0.2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Aapka smart factory manager",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(32.dp))

            KulhadTextField(
                label = "Email",
                value = email,
                onValueChange = { email = it; viewModel.resetError() },
                placeholder = "owner@kulhad.com",
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(10.dp))
            KulhadTextField(
                label = "Password",
                value = password,
                onValueChange = { password = it; viewModel.resetError() },
                placeholder = "Enter your password",
                isPassword = true,
                keyboardType = KeyboardType.Password
            )

            if (state is AuthUiState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (state as AuthUiState.Error).message,
                    color = ErrorRed,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            KulhadButton(
                text = if (state is AuthUiState.Loading) "Signing in…" else "Login / प्रवेश करें",
                enabled = state !is AuthUiState.Loading,
                onClick = { viewModel.login("owner@kulhad.com", "kulhad123") }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Demo: owner@kulhad.com / kulhad123",
                color = TextTertiary,
                fontSize = 9.sp
            )
        }
    }
}
