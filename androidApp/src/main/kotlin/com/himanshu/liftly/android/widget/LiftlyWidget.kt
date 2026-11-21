package com.himanshu.liftly.android.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.himanshu.liftly.core.platform.WidgetSnapshot

/**
 * Home-screen widget: this week's workouts against the goal as plates, the weekly streak, and one
 * button that starts the next routine (or resumes the workout in progress) via the app's deep link.
 */
class LiftlyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetStore.read(context, System.currentTimeMillis())
        provideContent { WidgetContent(snapshot, context.packageName) }
    }
}

class LiftlyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiftlyWidget()
}

private object WidgetColors {
    val Graphite = Color(0xFF16181C)
    val Empty = Color(0xFF2A2D33)
    val Red = Color(0xFFE0262C)
    val Yellow = Color(0xFFF2C12E)
    val Muted = Color(0xFF9399A3)
    val Plates = listOf(Color(0xFFE0262C), Color(0xFF2463E8), Color(0xFFF2C12E), Color(0xFF1FA863))
}

private fun text(color: Color, size: Int, bold: Boolean = false) =
    TextStyle(color = ColorProvider(color), fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot?, packageName: String) {
    val link = if (snapshot?.workoutInProgress == true) "liftly://workout" else "liftly://start-next-workout"
    val open = Intent(Intent.ACTION_VIEW, Uri.parse(link)).setPackage(packageName)
    Column(
        modifier = GlanceModifier.fillMaxSize().background(WidgetColors.Graphite).cornerRadius(24.dp).padding(14.dp),
    ) {
        Text("THIS WEEK", style = text(WidgetColors.Yellow, 11, bold = true))
        if (snapshot == null) {
            Text("Open Liftly to set up", style = text(Color.White, 14))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${snapshot.thisWeekCount}/${snapshot.weeklyGoal}", style = text(Color.White, 26, bold = true))
                Spacer(GlanceModifier.width(10.dp))
                Text(streakLabel(snapshot.streakWeeks), style = text(WidgetColors.Muted, 12))
            }
            Spacer(GlanceModifier.height(6.dp))
            WeekPlates(snapshot)
        }
        Spacer(GlanceModifier.defaultWeight())
        Box(
            modifier = GlanceModifier.fillMaxWidth().height(40.dp).background(WidgetColors.Red).cornerRadius(20.dp)
                .clickable(actionStartActivity(open)),
            contentAlignment = Alignment.Center,
        ) {
            Text(buttonLabel(snapshot), style = text(Color.White, 14, bold = true), maxLines = 1)
        }
    }
}

@Composable
private fun WeekPlates(snapshot: WidgetSnapshot) {
    val done = snapshot.thisWeekCount.coerceAtMost(snapshot.weeklyGoal)
    Row(
        modifier = GlanceModifier.fillMaxWidth().semantics {
            contentDescription = "${snapshot.thisWeekCount} of ${snapshot.weeklyGoal} workouts this week"
        },
    ) {
        repeat(snapshot.weeklyGoal) { i ->
            if (i > 0) Spacer(GlanceModifier.width(4.dp))
            val color = if (i < done) WidgetColors.Plates[i % WidgetColors.Plates.size] else WidgetColors.Empty
            Box(GlanceModifier.defaultWeight().height(10.dp).background(color).cornerRadius(5.dp)) {}
        }
    }
}

private fun streakLabel(weeks: Int) = when (weeks) {
    0 -> "No streak yet"
    1 -> "1-week streak"
    else -> "$weeks-week streak"
}

private fun buttonLabel(snapshot: WidgetSnapshot?) = when {
    snapshot?.workoutInProgress == true -> "Resume workout"
    snapshot?.nextRoutine != null -> "Start ${snapshot.nextRoutine}"
    else -> "Start workout"
}
