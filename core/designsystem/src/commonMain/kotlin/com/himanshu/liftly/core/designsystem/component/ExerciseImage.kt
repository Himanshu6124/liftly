package com.himanshu.liftly.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.himanshu.liftly.core.resources.resources.Res
import kotlinx.coroutines.delay

/** Bundled paths ("files/...") resolve to Compose resources; anything with a scheme loads as a URL. */
fun exerciseImageModel(path: String): String = if ("://" in path) path else Res.getUri(path)

@Composable
fun ExerciseThumbnail(
    imagePaths: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val path = imagePaths.firstOrNull()
        if (path == null) {
            PlaceholderIcon()
        } else {
            SubcomposeAsyncImage(
                model = exerciseImageModel(path),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = { PlaceholderIcon() },
            )
        }
    }
}

/**
 * Loops between the start and end position photos so users see the movement.
 * Works on every platform (no GIF decoder needed).
 */
@Composable
fun ExerciseDemo(
    imagePaths: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    frameMillis: Long = 1_200,
) {
    var frame by remember(imagePaths) { mutableIntStateOf(0) }
    if (imagePaths.size > 1) {
        LaunchedEffect(imagePaths) {
            while (true) {
                delay(frameMillis)
                frame = (frame + 1) % imagePaths.size
            }
        }
    }
    Box(
        modifier = modifier
            .aspectRatio(3f / 2f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imagePaths.isEmpty()) {
            PlaceholderIcon()
        } else {
            Crossfade(targetState = frame, animationSpec = tween(400), label = "exercise-demo") { index ->
                SubcomposeAsyncImage(
                    model = exerciseImageModel(imagePaths[index.coerceIn(imagePaths.indices)]),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    error = { PlaceholderIcon() },
                )
            }
        }
    }
}

@Composable
private fun PlaceholderIcon() {
    Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}
