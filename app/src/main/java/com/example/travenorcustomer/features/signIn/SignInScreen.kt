package com.example.travenorcustomer.features.signIn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.travenorcustomer.R
import com.example.travenorcustomer.features.components.TravenorIconButton
import com.example.travenorcustomer.features.components.TravenorTextButton
import com.example.travenorcustomer.features.components.TravenorTextField
import com.example.travenorcustomer.ui.theme.AppColors
import com.example.travenorcustomer.ui.theme.AppTypography
import org.koin.androidx.compose.koinViewModel

@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    onNavigationBack: () -> Unit,
    onNavigationToVerificationScreen: (String) -> Unit,
    viewModel: SignInViewModel = koinViewModel()
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is SignInEvent.NavigateToVerification ->
                    onNavigationToVerificationScreen(event.email)

                SignInEvent.NavigateBack ->
                    onNavigationBack()
            }
        }
    }

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
            text = stringResource(R.string.sign_in_sign_in_now),
            style = AppTypography.signInTitle,
        )

        Spacer(Modifier.height(15.dp))


        Text(
            text = stringResource(R.string.sign_in_please_sign_in),
            style = AppTypography.signInBody,
            color = AppColors.lightSub
        )

        Spacer(Modifier.height(60.dp))

        TravenorTextField(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            value = state.email,
            onValueChange = {
                viewModel.onAction(SignInAction.OnEmailChange(it))
            },
            placeholder = stringResource(R.string.label_email),
            errorMsg = state.error
        )

        Spacer(Modifier.height(25.dp))

        TravenorTextButton(
            onClick = {
                viewModel.onAction(SignInAction.OnSignIn)
            },
            btnText = if (state.isLoading) stringResource(R.string.button_signing) else stringResource(
                R.string.button_sign_in
            )
        )
    }

}