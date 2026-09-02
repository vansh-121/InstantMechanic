package com.instantmechanic.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette. A single confident blue for actions, a warm amber as the accent used on
// ratings and "verified" affordances, and a green reserved strictly for "open now".

internal val Blue10 = Color(0xFF001C38)
internal val Blue20 = Color(0xFF00325B)
internal val Blue30 = Color(0xFF004881)
internal val Blue40 = Color(0xFF0F5CA8)
internal val Blue80 = Color(0xFFA2C9FF)
internal val Blue90 = Color(0xFFD3E4FF)

internal val Slate10 = Color(0xFF111C2B)
internal val Slate20 = Color(0xFF273141)
internal val Slate30 = Color(0xFF3D4758)
internal val Slate40 = Color(0xFF545F70)
internal val Slate80 = Color(0xFFBCC7DC)
internal val Slate90 = Color(0xFFD8E3F8)

internal val Amber10 = Color(0xFF341100)
internal val Amber20 = Color(0xFF562600)
internal val Amber30 = Color(0xFF7B3B00)
internal val Amber40 = Color(0xFFA2521A)
internal val Amber80 = Color(0xFFFFB68A)
internal val Amber90 = Color(0xFFFFDCC2)

internal val Red10 = Color(0xFF410002)
internal val Red20 = Color(0xFF690005)
internal val Red30 = Color(0xFF93000A)
internal val Red40 = Color(0xFFBA1A1A)
internal val Red80 = Color(0xFFFFB4AB)
internal val Red90 = Color(0xFFFFDAD6)

internal val Neutral10 = Color(0xFF1A1C1E)
internal val Neutral20 = Color(0xFF2F3033)
internal val Neutral25 = Color(0xFF383A3E)
internal val Neutral90 = Color(0xFFE2E2E6)
internal val Neutral94 = Color(0xFFEAE9ED)
internal val Neutral95 = Color(0xFFF1F0F4)
internal val Neutral99 = Color(0xFFFDFCFF)

internal val NeutralVariant30 = Color(0xFF43474E)
internal val NeutralVariant50 = Color(0xFF73777F)
internal val NeutralVariant60 = Color(0xFF8D9199)
internal val NeutralVariant80 = Color(0xFFC3C6CF)
internal val NeutralVariant90 = Color(0xFFDFE2EB)

/**
 * Colours that carry meaning rather than brand, exposed through [LocalStatusColors] so a
 * composable never hard-codes "green means open".
 */
data class StatusColors(
    val open: Color,
    val onOpen: Color,
    val closed: Color,
    val onClosed: Color,
    val rating: Color,
)

internal val LightStatusColors = StatusColors(
    open = Color(0xFFD7F2DE),
    onOpen = Color(0xFF0B5A2B),
    closed = Color(0xFFEDEEF2),
    onClosed = Color(0xFF5A6069),
    rating = Color(0xFFE8930C),
)

internal val DarkStatusColors = StatusColors(
    open = Color(0xFF17402A),
    onOpen = Color(0xFF8FE0AA),
    closed = Color(0xFF31343A),
    onClosed = Color(0xFFB6BAC2),
    rating = Color(0xFFFFC048),
)
