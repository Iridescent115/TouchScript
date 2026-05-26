package com.lulucloud.touchscript.feature.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight

class DslSyntaxHighlightTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val content = text.text
        val builder = AnnotatedString.Builder(content)

        KEYWORDS.forEach { keyword ->
            keyword.toRegex().findAll(content).forEach { match ->
                builder.addStyle(
                    SpanStyle(
                        color = Color(0xFFFFC857),
                        fontWeight = FontWeight.SemiBold
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }

        "\"[^\"]*\"".toRegex().findAll(content).forEach { match ->
            builder.addStyle(
                SpanStyle(color = Color(0xFF6ED3CF)),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        "\\b\\d+\\b".toRegex().findAll(content).forEach { match ->
            builder.addStyle(
                SpanStyle(color = Color(0xFFF28F3B)),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private companion object {
        val KEYWORDS = listOf(
            "点击",
            "长按",
            "滑动",
            "等待",
            "启动应用",
            "记录",
            "识图",
            "设",
            "无限循环",
            "循环",
            "结束循环",
            "如果",
            "否则",
            "结束如果",
            "返回",
            "主页"
        )
    }
}
