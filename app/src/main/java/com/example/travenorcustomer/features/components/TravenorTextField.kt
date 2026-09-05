package com.example.travenorcustomer.features.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travenorcustomer.R
import com.example.travenorcustomer.ui.theme.AppColors
import com.example.travenorcustomer.ui.theme.AppTypography

@Composable
fun TravenorTextField(
    modifier: Modifier = Modifier,
    value: String,
    textAlign: TextAlign = TextAlign.Center,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    placeholder: String? = null,
    errorMsg: String? = null,
) {

    TextField(
        value = value,
        onValueChange = {
            onValueChange(it)
        },
        keyboardOptions = keyboardOptions,
        singleLine = true,
        modifier = modifier
            .width(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = AppColors.lightGrey,
            unfocusedContainerColor = AppColors.lightGrey,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent
        ),
        textStyle = AppTypography.signInBody.copy(
            textAlign = textAlign,
            color = AppColors.customBlack
        ),
        placeholder = {
            placeholder?.let {
                Text(
                    text = it,
                )
            }
        },
        supportingText = {
            errorMsg?.let {
                Text(
                    text = it,
                    color = Color.Red
                )
            }
        },
    )
}