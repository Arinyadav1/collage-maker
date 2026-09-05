package com.example.travenorcustomer.features.otpVerification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.travenorcustomer.R
import com.example.travenorcustomer.features.components.TravenorIconButton
import com.example.travenorcustomer.features.components.TravenorTextButton
import com.example.travenorcustomer.features.components.TravenorTextField
import com.example.travenorcustomer.ui.theme.AppColors
import com.example.travenorcustomer.ui.theme.AppTypography
import org.koin.androidx.compose.koinViewModel

@Composable
fun VerificationScreen(
    modifier: Modifier = Modifier,
    onNavigationBack: () -> Unit,
    onNavigationToHomeScreen: () -> Unit,
    viewModel: VerificationViewModel = koinViewModel()
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                VerificationEvent.OnNavigationBack -> onNavigationBack()
                VerificationEvent.OnNavigationToHomeScreen -> onNavigationToHomeScreen()
            }
        }
    }

    val focusRequesters = List(state.otpValues.size) { FocusRequester() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            TravenorIconButton(
                onClick = {
                    onNavigationBack()
                }
            )
        }

        Spacer(Modifier.height(130.dp))

        Text(
            text = stringResource(R.string.otp_verification),
            style = AppTypography.signInTitle,
        )

        Spacer(Modifier.height(15.dp))


        Text(
            text = stringResource(R.string.otp_verification_description_before_email) + " ${state.email} " + stringResource(
                R.string.otp_verification_description_after_email
            ),
            color = AppColors.lightSub,
            style = AppTypography.signInBody
        )

        Spacer(Modifier.height(40.dp))


        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            state.otpValues.forEachIndexed { index, value ->
                TravenorTextField(
                    modifier = Modifier.focusRequester(focusRequesters[index]),
                    value = value,
                    onValueChange = { newValue ->
                        if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                            viewModel.onAction(VerificationAction.OnOtpValueChange(index, newValue))

                            if (newValue.isNotEmpty() && index < state.otpValues.lastIndex) {
                                focusRequesters[index + 1].requestFocus()
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                )
            }
        }
        if (state.error != null) {
            Text(
                text = state.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Start,
                color = AppColors.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(25.dp))


        TravenorTextButton(
            onClick = { viewModel.onAction(VerificationAction.OnVerify) },
            btnText = if (state.loading) stringResource(R.string.button_verifying) else stringResource(
                R.string.button_verify
            ),
            enabled = state.otpValues[state.otpValues.lastIndex].isNotEmpty()
        )

        Spacer(Modifier.height(15.dp))

        if (state.isShowResend) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = {
                            viewModel.onAction(VerificationAction.OnResendHandle)
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (state.resendLoading) stringResource(R.string.resending) else stringResource(
                        R.string.resend
                    ),
                    color = AppColors.lightSub,
                    style = AppTypography.signInLabel
                )
                if (state.countDownTimer != null) {
                    Text(
                        text = state.countDownTimer,
                        fontFamily = FontFamily(
                            Font(R.font.sf_ui_display_regular)
                        ),
                        fontSize = 14.sp,
                        color = AppColors.lightSub,
                    )
                }
            }
        }
    }
}