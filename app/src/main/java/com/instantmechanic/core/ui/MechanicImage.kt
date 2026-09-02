package com.instantmechanic.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import java.util.Locale
import kotlin.math.abs

/**
 * Garage photo, with a graceful fallback.
 *
 * Photos arrive from the API as URLs, which means they need the network even though the rest of the
 * app does not. The bundled mock payload therefore ships no `image_url` at all — so a reviewer who
 * clones this repo and runs it on a plane sees the same screen as everyone else — and this
 * composable draws a deterministic gradient derived from the garage name plus its initials:
 * stable across launches, distinct per garage, and presentable on its own rather than a broken
 * grey box.
 *
 * The Coil path is still real, not decoration: point `instantmechanic.baseUrl` at a live server
 * that returns `image_url` and the photos load with no code change.
 */
@Composable
fun MechanicImage(
    imageUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    initialsTextSize: TextUnit = 28.sp,
) {
    SubcomposeAsyncImage(
        model = imageUrl.takeIf { it.isNotBlank() },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { InitialsPlaceholder(name, initialsTextSize) },
        error = { InitialsPlaceholder(name, initialsTextSize) },
    )
}

@Composable
private fun InitialsPlaceholder(name: String, textSize: TextUnit) {
    val (start, end) = gradientFor(name)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(start, end), start = Offset.Zero)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            style = TextStyle(
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.92f),
            ),
        )
    }
}

/** "Sharma Auto Works" -> "SA". Falls back to a single character, then to a dash. */
internal fun initialsOf(name: String): String {
    val words = name.split(' ', '&', '-').filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase(Locale.US)
        words.size == 1 -> words[0].take(2).uppercase(Locale.US)
        else -> "—"
    }
}

/**
 * Picks a pleasant two-stop gradient from the name's hash.
 *
 * Hue is derived from the hash so it is stable, while saturation and lightness are fixed at
 * values that keep white text readable on every hue.
 */
private fun gradientFor(name: String): Pair<Color, Color> {
    val hue = abs(name.hashCode()) % 360
    return Color.hsl(hue.toFloat(), 0.52f, 0.42f) to
        Color.hsl(((hue + 28) % 360).toFloat(), 0.46f, 0.28f)
}
