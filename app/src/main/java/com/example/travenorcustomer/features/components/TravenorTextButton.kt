package com.example.travenorcustomer.features.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.travenorcustomer.ui.theme.AppColors
import com.example.travenorcustomer.ui.theme.AppTypography

@Composable
fun TravenorTextButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    btnText: String,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
    ) {
        TextButton(
            enabled = enabled,
            onClick = { onClick() },
            colors = ButtonDefaults.textButtonColors(
                containerColor = AppColors.primaryBlue,
                contentColor = AppColors.customWhite,
                disabledContentColor = AppColors.grey,
                disabledContainerColor = AppColors.lightGrey,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = btnText,
                style = AppTypography.textButton
            )
        }
    }
}