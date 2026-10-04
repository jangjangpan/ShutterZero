package io.github.hoons1994.shuttersoundzero.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hoons1994.shuttersoundzero.theme.AppThemeStyle
import io.github.hoons1994.shuttersoundzero.theme.ThemeSettings
import io.github.hoons1994.shuttersoundzero.theme.colorSchemeFor
import io.github.hoons1994.shuttersoundzero.theme.tokensFor

/** 테마 4종을 실제 색과 모서리로 미리 보여 주고, 누르면 바로 적용한다. */
@Composable
internal fun ThemePicker() {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val current = ThemeSettings.style
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppThemeStyle.entries.chunked(2).forEach { rowStyles ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowStyles.forEach { style ->
                    ThemeOption(
                        style = style,
                        dark = dark,
                        selected = style == current,
                        onClick = { ThemeSettings.select(context, style) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    style: AppThemeStyle,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = colorSchemeFor(style, dark)
    val tokens = tokensFor(style, dark)
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(if (selected) 2.dp else 1.dp, ring, RoundedCornerShape(16.dp))
            .background(scheme.background)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(tokens.fill)
                    .border(1.dp, scheme.onSurface.copy(alpha = 0.12f), CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = style.label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            if (selected) {
                Spacer(Modifier.weight(1f))
                Text("✓", color = tokens.accentText, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = style.description,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        // 이 테마의 주 버튼 모양 그대로
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 30.dp)
                .clip(RoundedCornerShape(tokens.buttonRadius))
                .background(tokens.fill),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (selected) "사용 중" else "적용",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.onFill,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(0.dp))
    }
}
