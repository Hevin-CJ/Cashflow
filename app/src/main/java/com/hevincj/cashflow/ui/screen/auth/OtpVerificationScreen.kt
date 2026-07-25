package com.hevincj.cashflow.ui.screen.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hevincj.cashflow.ui.screen.state.AuthState
import com.hevincj.cashflow.ui.screen.viewmodel.AuthViewModel
import com.hevincj.cashflow.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun OtpVerificationScreen(
    navController: NavController,
    flowType: String,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    var timerSeconds by remember { mutableStateOf(60) }
    var isTimerActive by remember { mutableStateOf(true) }

    LaunchedEffect(isTimerActive, timerSeconds) {
        if (isTimerActive && timerSeconds > 0) {
            delay(1000L)
            timerSeconds--
        } else if (timerSeconds == 0) {
            isTimerActive = false
        }
    }

    LaunchedEffect(uiState.authState) {
        when (uiState.authState) {
            is AuthState.LoginSuccess, is AuthState.RegisterSuccess -> {
                Toast.makeText(context, "Verification Successful!", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is AuthState.Error -> {
                Toast.makeText(context, (uiState.authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Verify OTP",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We have sent a verification code to",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Text(
                text = uiState.username,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = uiState.otp,
                onValueChange = { if (it.length <= 6) viewModel.onOtpChange(it) },
                label = { Text("6-Digit OTP Code") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (flowType == "login") viewModel.verifyLogin() else viewModel.verifyRegister()
                    }
                ),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTimerActive) {
                    Text(
                        text = "Resend code in 0:${timerSeconds.toString().padStart(2, '0')}",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                } else {
                    TextButton(
                        onClick = {
                            timerSeconds = 60
                            isTimerActive = true
                            if (flowType == "login") viewModel.initiateLogin() else viewModel.initiateRegister()
                        }
                    ) {
                        Text("Resend OTP", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (flowType == "login") viewModel.verifyLogin() else viewModel.verifyRegister()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryGradient),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                enabled = uiState.authState != AuthState.Loading && uiState.otp.length == 6
            ) {
                if (uiState.authState == AuthState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Verify & Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = {
                    navController.popBackStack()
                }
            ) {
                Text("Back", color = TextSecondary)
            }
        }
    }
}
