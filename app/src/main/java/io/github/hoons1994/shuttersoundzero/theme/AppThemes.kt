package io.github.hoons1994.shuttersoundzero.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * oh-my-design.kr 의 DESIGN.md 레퍼런스에서 옮긴 테마 4종.
 * 라이트 값은 각 디자인 시스템의 토큰을 그대로 쓰고, 다크 값은 같은 주색에 맞춰 만든 것이다.
 */
enum class AppThemeStyle(val label: String, val description: String) {
    TOSS("Toss", "블루 · 넉넉한 둥근 모서리"),
    KARROT("당근", "오렌지 · 가볍고 친근한"),
    ONE_UI("One UI", "블랙 · 갤럭시 기본 느낌"),
    KAKAO("카카오", "옐로 · 또렷한 대비"),
}

/** Material ColorScheme 에 담기지 않는 테마별 값. */
@Immutable
data class ThemeTokens(
    /** 흰 배경 위 글자로 쓸 강조색. 카카오처럼 주색이 밝으면 주색과 다르다. */
    val accentText: Color,
    /** 진행 중·현재 단계 표시처럼 작은 면적의 강조색. */
    val accentMark: Color,
    /** 채워진 주 버튼 색. 카카오 라이트처럼 글자 강조색(primary)과 다를 수 있다. */
    val fill: Color,
    val onFill: Color,
    val cardRadius: Dp,
    val buttonRadius: Dp,
)

val LocalThemeTokens = staticCompositionLocalOf {
    ThemeTokens(Color(0xFF3182F6), Color(0xFF3182F6), Color(0xFF3182F6), Color.White, 24.dp, 16.dp)
}

private data class Palette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val accentText: Color,
    val accentMark: Color,
)

private fun Palette.toLight(): ColorScheme = lightColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = accentText, onSecondary = onPrimary,
    background = background, onBackground = onSurface,
    surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    surfaceContainer = surface, surfaceContainerHigh = surfaceVariant,
    surfaceContainerLowest = background,
)

private fun Palette.toDark(): ColorScheme = darkColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = accentText, onSecondary = onPrimary,
    background = background, onBackground = onSurface,
    surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    surfaceContainer = surface, surfaceContainerHigh = surfaceVariant,
    surfaceContainerLowest = background,
)

private fun palette(style: AppThemeStyle, dark: Boolean): Palette = when (style) {
    // Toss: primary #3182f6, surface #f2f4f6, foreground #191f28, body #4e5968
    AppThemeStyle.TOSS -> if (!dark) Palette(
        Color(0xFF3182F6), Color.White, Color(0xFFE8F3FF), Color(0xFF1B64DA),
        Color(0xFFF2F4F6), Color.White, Color(0xFFE5E8EB), Color(0xFF191F28), Color(0xFF4E5968),
        Color(0xFF1B64DA), Color(0xFF3182F6),
    ) else Palette(
        Color(0xFF4593FC), Color.White, Color(0xFF1C2D4A), Color(0xFFC9E2FF),
        Color(0xFF101013), Color(0xFF1C1C21), Color(0xFF2C2C35), Color(0xFFEDEDF0), Color(0xFF9E9EA4),
        Color(0xFF6AABFF), Color(0xFF4593FC),
    )
    // Karrot (SEED): primary #ff6f0f, background #f2f3f6, foreground #212124, muted #868b94
    AppThemeStyle.KARROT -> if (!dark) Palette(
        Color(0xFFFF6F0F), Color.White, Color(0xFFFFF5F0), Color(0xFFC24400),
        Color(0xFFF2F3F6), Color.White, Color(0xFFEAEBEE), Color(0xFF212124), Color(0xFF5E636C),
        Color(0xFFD45200), Color(0xFFFF6F0F),
    ) else Palette(
        Color(0xFFFF7E36), Color(0xFF1A0A00), Color(0xFF3A2011), Color(0xFFFFD9C2),
        Color(0xFF121214), Color(0xFF1E1F22), Color(0xFF2C2D31), Color(0xFFEDEEF1), Color(0xFF9A9FA8),
        Color(0xFFFF9E66), Color(0xFFFF7E36),
    )
    // Samsung / One UI: primary #000000, surface #f7f7f7, muted #707070, accent #0381fe
    AppThemeStyle.ONE_UI -> if (!dark) Palette(
        Color(0xFF000000), Color.White, Color(0xFFEAF3FF), Color(0xFF0060C7),
        Color(0xFFF7F7F7), Color.White, Color(0xFFDDDDDD), Color(0xFF000000), Color(0xFF707070),
        Color(0xFF006BEA), Color(0xFF0381FE),
    ) else Palette(
        Color(0xFFFFFFFF), Color.Black, Color(0xFF0D2440), Color(0xFFCFE4FF),
        Color(0xFF000000), Color(0xFF171717), Color(0xFF2A2A2A), Color(0xFFFAFAFA), Color(0xFFA0A0A0),
        Color(0xFF3E9BFF), Color(0xFF3E9BFF),
    )
    // Kakao: login #fee500 with #191919 ink, surface #eeeeee, foreground #333333, muted #888888
    // 라이트에서는 흰 배경 위 노란 글씨가 읽히지 않으므로 primary 는 잉크색, 노랑은 채운 버튼(fill)에만 쓴다.
    AppThemeStyle.KAKAO -> if (!dark) Palette(
        Color(0xFF191919), Color.White, Color(0xFFFFF7B3), Color(0xFF3D3700),
        Color(0xFFF5F5F5), Color.White, Color(0xFFEEEEEE), Color(0xFF191919), Color(0xFF666666),
        Color(0xFF191919), Color(0xFFD9C400),
    ) else Palette(
        Color(0xFFFEE500), Color(0xFF191919), Color(0xFF3A3510), Color(0xFFFFF3A0),
        Color(0xFF111111), Color(0xFF1D1D1D), Color(0xFF2E2E2E), Color(0xFFF2F2F2), Color(0xFF9A9A9A),
        Color(0xFFFEE500), Color(0xFFFEE500),
    )
}

internal fun colorSchemeFor(style: AppThemeStyle, dark: Boolean): ColorScheme =
    palette(style, dark).let { if (dark) it.toDark() else it.toLight() }

internal fun tokensFor(style: AppThemeStyle, dark: Boolean): ThemeTokens {
    val p = palette(style, dark)
    // Radii follow each system's own scale: Toss button-xlarge 16, SEED md 8, Samsung pill 20, Kakao login 12.
    val (card, button) = when (style) {
        AppThemeStyle.TOSS -> 24.dp to 16.dp
        AppThemeStyle.KARROT -> 12.dp to 8.dp
        AppThemeStyle.ONE_UI -> 26.dp to 20.dp
        AppThemeStyle.KAKAO -> 16.dp to 12.dp
    }
    val kakaoLight = style == AppThemeStyle.KAKAO && !dark
    val fill = if (kakaoLight) Color(0xFFFEE500) else p.primary
    val onFill = if (kakaoLight) Color(0xFF191919) else p.onPrimary
    return ThemeTokens(p.accentText, p.accentMark, fill, onFill, card, button)
}

/** 선택한 테마. 화면이 바로 바뀌도록 Compose State 로 들고 있는다. */
object ThemeSettings {
    private const val PREFS = "theme_prefs"
    private const val KEY_STYLE = "style"

    var style by mutableStateOf(AppThemeStyle.TOSS)
        private set
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val saved = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STYLE, null)
        style = AppThemeStyle.entries.firstOrNull { it.name == saved } ?: AppThemeStyle.TOSS
    }

    fun select(context: Context, newStyle: AppThemeStyle) {
        style = newStyle
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_STYLE, newStyle.name).apply()
    }
}
