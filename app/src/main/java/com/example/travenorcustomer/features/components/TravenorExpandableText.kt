package com.example.travenorcustomer.features.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.sp
import com.example.travenorcustomer.R
import com.example.travenorcustomer.ui.theme.AppColors
import com.example.travenorcustomer.ui.theme.AppTypography

@Composable
fun TravenorExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    lineLimit: Int = 3
) {
    var expanded by remember { mutableStateOf(false) }
    var cutText by remember { mutableStateOf<String?>(null) }


    val readMoreStyle =
        TextLinkStyles(
            SpanStyle(
                color = AppColors.orange,
                fontFamily = FontFamily(
                    Font(R.font.sf_ui_display_medium)
                ),
                fontSize = 13.sp,
            )
        )

    Box {

        val annotatedString = buildAnnotatedString {
            if (expanded) {
                append(text)
                append(" ")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "toggle",
                        styles = readMoreStyle,
                        linkInteractionListener = { expanded = false })
                ) { append(stringResource(R.string.read_less)) }
            } else {
                append(cutText ?: text)
                if (cutText != null) {
                    append("... ")
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "toggle",
                            styles = readMoreStyle,
                            linkInteractionListener = { expanded = true })
                    ) { append(stringResource(R.string.read_more)) }
                }
            }
        }

        Text(
            modifier = modifier,
            text = annotatedString,
            color = AppColors.lightSub,
            style = AppTypography.detailSmallBody,
            maxLines = if (expanded) Int.MAX_VALUE else lineLimit,
            onTextLayout = { layoutResult ->
                if (layoutResult.hasVisualOverflow && !expanded) {
                    val lastCharIndex = layoutResult.getLineEnd(lineLimit - 1, visibleEnd = true)

                    val newCut = text.substring(0, (lastCharIndex - 15).coerceAtLeast(0))
                    if (cutText != newCut) {
                        cutText = newCut
                    }
                }
            })
    }
}