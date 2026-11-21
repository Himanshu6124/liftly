package com.himanshu.liftly.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Immutable
data class TabItem(val icon: ImageVector, val selectedIcon: ImageVector, val label: String)

/**
 * Extra bottom padding a top-level screen adds to its scrolling content so the last item can
 * scroll clear of the floating tab bar (0 when the bar isn't shown, e.g. rail/drawer layouts).
 */
val LocalFloatingBarInset = staticCompositionLocalOf { 0.dp }

/** Height of [FloatingTabBar] plus its gap above the home indicator, excluding system insets. */
val FloatingTabBarHeight = 64.dp + 12.dp

/**
 * iOS-style floating tab bar: a rounded pill hovering above the content with a sliding
 * indicator behind the selected tab. Tabs are a selectable group, so screen readers announce
 * "Tab, n of 5, selected".
 */
@Composable
fun FloatingTabBar(
    items: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.35f),
                )
                .background(colors.surfaceContainerHigh, shape)
                .border(0.5.dp, colors.outlineVariant, shape)
                .padding(6.dp),
        ) {
            val tabWidth = maxWidth / items.size
            val indicatorX by animateDpAsState(
                tabWidth * selectedIndex,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "tabIndicator",
            )
            Box(
                Modifier
                    .offset(x = indicatorX)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .background(colors.primary.copy(alpha = 0.14f), RoundedCornerShape(26.dp)),
            )
            Row(Modifier.fillMaxSize().selectableGroup()) {
                items.forEachIndexed { i, item ->
                    Tab(item, selected = i == selectedIndex, onClick = { onSelect(i) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Tab(item: TabItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabTint",
    )
    Column(
        modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Scaffold content insets for a top-level screen: keeps its FAB, snackbars and list end above
 * the floating tab bar when it is shown, otherwise the usual system-bar insets.
 */
@Composable
fun floatingBarAwareInsets(): WindowInsets {
    val inset = LocalFloatingBarInset.current
    val defaults = ScaffoldDefaults.contentWindowInsets
    return if (inset > 0.dp) {
        defaults.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal).union(WindowInsets(bottom = inset))
    } else {
        defaults
    }
}
