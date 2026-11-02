package com.example.batterypulse.ui

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.batterypulse.BatteryPulseApplication
import com.example.batterypulse.data.AppLanguage
import com.example.batterypulse.data.AppPreferences
import com.example.batterypulse.data.AppThemeChoice
import com.example.batterypulse.data.ChargingSessionEntity
import com.example.batterypulse.domain.BatteryStatus

private data class Palette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val accent: Color,
    val accentDark: Color,
    val text: Color,
    val secondaryText: Color,
    val track: Color
)

private val DarkPalette = Palette(
    background = Color(0xFF081310), surface = Color(0xFF12231F), surfaceElevated = Color(0xFF19312A),
    accent = Color(0xFF35E89A), accentDark = Color(0xFF0F6D4C), text = Color(0xFFF5FBF8),
    secondaryText = Color(0xFFA1B5AF), track = Color(0xFF29443A)
)
private val LightPalette = Palette(
    background = Color(0xFFF3F7F5), surface = Color(0xFFFFFFFF), surfaceElevated = Color(0xFFE6F3ED),
    accent = Color(0xFF087F55), accentDark = Color(0xFFBDEBD5), text = Color(0xFF14221C),
    secondaryText = Color(0xFF64756D), track = Color(0xFFD6E5DD)
)

class MainActivity : ComponentActivity() {
    private val viewModel: DashboardViewModel by viewModels {
        val app = application as BatteryPulseApplication
        DashboardViewModelFactory(app.batteryRepository, app.preferencesRepository)
    }
    private var lastAutomaticRefreshMillis = 0L
    private val batteryChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastAutomaticRefreshMillis >= AUTOMATIC_REFRESH_MIN_INTERVAL_MILLIS) {
                lastAutomaticRefreshMillis = now
                viewModel.refresh()
            }
        }
    }
    private val notificationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.startMonitoring()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            batteryChangedReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        viewModel.refresh()
    }

    override fun onStop() {
        unregisterReceiver(batteryChangedReceiver)
        super.onStop()
    }

    companion object {
        private const val AUTOMATIC_REFRESH_MIN_INTERVAL_MILLIS = 1_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            var selectedTab by remember { mutableStateOf(0) }
            val palette = if (preferences.theme == AppThemeChoice.DARK) DarkPalette else LightPalette
            MaterialTheme {
                Surface(color = palette.background, modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            when (selectedTab) {
                                1 -> AnalysisScreen(viewModel, preferences, palette)
                                2 -> HistoryScreen(viewModel, preferences, palette)
                                3 -> SettingsScreen(viewModel, preferences, palette)
                                else -> BatteryHomeScreen(
                                    viewModel,
                                    preferences,
                                    palette,
                                    onAlerts = { selectedTab = 3 },
                                    onMonitor = {
                                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            notificationPermissionRequest.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.startMonitoring()
                                        }
                                    }
                                )
                            }
                        }
                        NavigationBar(containerColor = palette.surface) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text(if (preferences.language == AppLanguage.ENGLISH) "Home" else "Trang chủ") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                                label = { Text(if (preferences.language == AppLanguage.ENGLISH) "Analysis" else "Phân tích") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                label = { Text(if (preferences.language == AppLanguage.ENGLISH) "History" else "Lịch sử") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text(if (preferences.language == AppLanguage.ENGLISH) "Settings" else "Cài đặt") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryHomeScreen(viewModel: DashboardViewModel, preferences: AppPreferences, palette: Palette, onAlerts: () -> Unit, onMonitor: () -> Unit) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.background).padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { Header(status, palette, preferences.language) }
        item { BatteryHeroCard(status, palette, preferences.language) }
        item { QuickActions(viewModel, palette, preferences.language, onMonitor, onAlerts) }
        item { LiveMetrics(status, palette, preferences.language) }
        item { RecentStatus(status, palette, preferences.language) }
    }
}

@Composable
private fun HistoryScreen(viewModel: DashboardViewModel, preferences: AppPreferences, palette: Palette) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val english = preferences.language == AppLanguage.ENGLISH
    val completed = sessions.filter { it.endedAtMillis != null }
    val equivalentCycles = completed.sumOf { it.energyPercentGained }.toFloat() / 100f
    val samples by viewModel.samples.collectAsStateWithLifecycle()
    val designCapacity = preferences.designCapacityMah
    val chargedMah = remember(samples) {
        samples.zipWithNext().sumOf { (previous, current) ->
            val elapsedHours = (current.timestampMillis - previous.timestampMillis).coerceIn(0L, 120_000L) / 3_600_000f
            if (previous.status.chargingState == "Đang sạc" && current.status.chargingState == "Đang sạc") {
                ((kotlin.math.abs(previous.status.currentAmps) + kotlin.math.abs(current.status.currentAmps)) * 500f * elapsedHours).toDouble()
            } else 0.0
        }.toFloat()
    }
    val estimatedHealth = if (designCapacity > 0) (chargedMah / designCapacity * 100f).coerceIn(0f, 100f) else null
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.background).padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text(if (english) "Charging history" else "Lịch sử sạc", color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard(Modifier.weight(1f), if (english) "Equivalent cycles" else "Chu kỳ tương đương", "%.2f".format(equivalentCycles), palette)
                SummaryCard(Modifier.weight(1f), if (english) "Estimated health" else "Tình trạng ước lượng", estimatedHealth?.let { "%.0f%%".format(it) } ?: "—", palette)
            }
        }
        item {
            Text(
                if (english) "Estimated charge in recorded samples: %.0f mAh".format(chargedMah) else "Điện lượng ước tính trong các mẫu đã ghi: %.0f mAh".format(chargedMah),
                color = palette.secondaryText,
                fontSize = 12.sp
            )
        }
        if (sessions.isEmpty()) {
            item { Text(if (english) "No charging sessions recorded yet." else "Chưa có phiên sạc nào được ghi nhận.", color = palette.secondaryText, fontSize = 13.sp) }
        } else {
            items(sessions, key = { it.id }) { session -> SessionCard(session, english, palette) }
        }
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, title: String, value: String, palette: Palette) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = palette.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = palette.secondaryText, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = palette.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SessionCard(session: ChargingSessionEntity, english: Boolean, palette: Palette) {
    val start = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(session.startedAtMillis))
    val end = session.endedAtMillis?.let { java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(it)) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = palette.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(if (session.endedAtMillis == null) if (english) "Active session" else "Phiên đang hoạt động" else if (english) "Charging session" else "Phiên sạc", color = palette.text, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(if (english) "$start → ${end ?: "..."}" else "$start → ${end ?: "..."}", color = palette.secondaryText, fontSize = 11.sp)
            Text(if (english) "${session.startPercent}% → ${session.endPercent ?: "..."}% · +${session.energyPercentGained}%" else "${session.startPercent}% → ${session.endPercent ?: "..."}% · tăng ${session.energyPercentGained}%", color = palette.text, fontSize = 13.sp)
            Text(if (english) "Peak temperature: %.1f°C".format(session.maxTemperatureCelsius) else "Nhiệt độ cao nhất: %.1f°C".format(session.maxTemperatureCelsius), color = palette.secondaryText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: DashboardViewModel, preferences: AppPreferences, palette: Palette) {
    val english = preferences.language == AppLanguage.ENGLISH
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.background).padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { Text(if (english) "Settings" else "Cài đặt", color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item { SettingsCard(viewModel, preferences, palette) }
    }
}

@Composable
private fun AnalysisScreen(viewModel: DashboardViewModel, preferences: AppPreferences, palette: Palette) {
    val samples by viewModel.samples.collectAsStateWithLifecycle()
    val english = preferences.language == AppLanguage.ENGLISH
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.background).padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text(if (english) "Battery analysis" else "Phân tích pin", color = palette.text, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        item {
            Text(
                if (english) "${samples.size} readings · collected on app refresh and during monitoring" else "${samples.size} mẫu · thu thập khi làm mới hoặc theo dõi nền",
                color = palette.secondaryText,
                fontSize = 12.sp
            )
        }
        item { AnalysisChartCard(if (english) "Battery level" else "Mức pin", "%", palette, samples.map { it.status.percent.toFloat() }, english) }
        item { AnalysisChartCard(if (english) "Temperature" else "Nhiệt độ", "°C", palette, samples.map { it.status.temperatureCelsius }, english) }
        item { AnalysisChartCard(if (english) "Voltage" else "Điện áp", "mV", palette, samples.map { it.status.voltageMillivolts.toFloat() }, english) }
        item { AnalysisChartCard(if (english) "Current" else "Dòng điện", "mA", palette, samples.map { it.status.currentAmps * 1000f }, english) }
        item { AnalysisChartCard(if (english) "Power" else "Công suất", "W", palette, samples.map { it.status.powerWatts }, english) }
    }
}

@Composable
private fun AnalysisChartCard(title: String, unit: String, palette: Palette, values: List<Float>, english: Boolean) {
    val displayed = values.takeLast(120)
    val current = displayed.lastOrNull() ?: 0f
    val min = displayed.minOrNull() ?: 0f
    val max = displayed.maxOrNull() ?: 0f
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = palette.surface)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("%.1f %s".format(current, unit), color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Canvas(Modifier.fillMaxWidth().height(112.dp)) {
                val left = 4.dp.toPx()
                val right = size.width - left
                val top = 5.dp.toPx()
                val bottom = size.height - 5.dp.toPx()
                repeat(3) { index ->
                    val y = top + (bottom - top) * index / 2f
                    drawLine(palette.track, androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(right, y), 1.dp.toPx())
                }
                if (displayed.size > 1) {
                    val span = (max - min).takeIf { it > 0.001f } ?: 1f
                    val path = Path()
                    displayed.forEachIndexed { index, value ->
                        val x = left + (right - left) * index / displayed.lastIndex.toFloat()
                        val y = bottom - ((value - min) / span) * (bottom - top)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, palette.accent, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx()))
                } else if (displayed.size == 1) {
                    drawCircle(palette.accent, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset((left + right) / 2f, (top + bottom) / 2f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (english) "Min %.1f".format(min) else "Thấp nhất %.1f".format(min), color = palette.secondaryText, fontSize = 10.sp)
                Text(
                    if (english) {
                        if (values.size > 120) "Last 120 samples" else "${values.size} samples"
                    } else {
                        if (values.size > 120) "120 mẫu gần nhất" else "${values.size} mẫu"
                    },
                    color = palette.secondaryText,
                    fontSize = 10.sp
                )
                Text(if (english) "Max %.1f".format(max) else "Cao nhất %.1f".format(max), color = palette.secondaryText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun Header(status: BatteryStatus, palette: Palette, language: AppLanguage) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(if (language == AppLanguage.ENGLISH) "Battery status" else "Theo dõi pin", color = palette.secondaryText, fontSize = 14.sp)
            Text("Battery Pulse", color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(palette.surfaceElevated), contentAlignment = Alignment.Center) {
            Text("${status.percent}%", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BatteryHeroCard(status: BatteryStatus, palette: Palette, language: AppLanguage) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryChargingFull, null, tint = palette.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (language == AppLanguage.ENGLISH) "Live battery status" else "Trạng thái pin", color = palette.text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(localizedCharging(status, language), color = palette.accent, fontSize = 12.sp)
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${status.percent}", color = palette.text, fontSize = 54.sp, fontWeight = FontWeight.Bold)
                Text("%", color = palette.accent, fontSize = 24.sp, modifier = Modifier.padding(bottom = 8.dp))
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (language == AppLanguage.ENGLISH) "Charging power" else "Công suất sạc", color = palette.secondaryText, fontSize = 11.sp)
                    Text("%.1f W".format(status.powerWatts), color = palette.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(palette.track)) {
                Box(modifier = Modifier.fillMaxWidth(status.percent.coerceIn(0, 100) / 100f).height(7.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(palette.accentDark, palette.accent))))
            }
            Spacer(Modifier.height(10.dp))
            Text(if (language == AppLanguage.ENGLISH) "Battery care target: 80%" else "Mục tiêu bảo vệ pin: 80%", color = palette.secondaryText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun QuickActions(viewModel: DashboardViewModel, palette: Palette, language: AppLanguage, onMonitor: () -> Unit, onAlerts: () -> Unit) {
    Column {
        Text(if (language == AppLanguage.ENGLISH) "Quick actions" else "Thao tác nhanh", color = palette.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            ActionCard(Modifier.weight(1f), if (language == AppLanguage.ENGLISH) "Monitor" else "Theo dõi", Icons.Default.Power, palette, onMonitor)
            ActionCard(Modifier.weight(1f), if (language == AppLanguage.ENGLISH) "Refresh" else "Làm mới", Icons.Default.Refresh, palette) { viewModel.refresh() }
            ActionCard(Modifier.weight(1f), if (language == AppLanguage.ENGLISH) "Alerts" else "Cảnh báo", Icons.Default.NotificationsActive, palette, onAlerts)
        }
    }
}

@Composable
private fun ActionCard(modifier: Modifier, label: String, icon: ImageVector, palette: Palette, onClick: () -> Unit) {
    Card(modifier = modifier.height(92.dp), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = palette.surface), onClick = onClick) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(palette.accentDark), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = palette.accent, modifier = Modifier.size(18.dp))
            }
            Text(label, color = palette.text, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun LiveMetrics(status: BatteryStatus, palette: Palette, language: AppLanguage) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (language == AppLanguage.ENGLISH) "Live insights" else "Thông số trực tiếp", color = palette.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(if (language == AppLanguage.ENGLISH) "Now" else "Hiện tại", color = palette.accent, fontSize = 11.sp)
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(Modifier.weight(1f), if (language == AppLanguage.ENGLISH) "Temperature" else "Nhiệt độ", "%.1f°C".format(status.temperatureCelsius), Icons.Default.DeviceThermostat, palette)
            MetricCard(Modifier.weight(1f), if (language == AppLanguage.ENGLISH) "Voltage" else "Điện áp", "${status.voltageMillivolts} mV", Icons.Default.Bolt, palette)
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(Modifier.weight(1f), if (language == AppLanguage.ENGLISH) "Current" else "Dòng điện", "%.0f mA".format(status.currentAmps * 1000), Icons.Default.Speed, palette)
            MetricCard(Modifier.weight(1f), if (language == AppLanguage.ENGLISH) "Power" else "Công suất", "%.1f W".format(status.powerWatts), Icons.Default.Power, palette)
        }
    }
}

@Composable
private fun MetricCard(modifier: Modifier, title: String, value: String, icon: ImageVector, palette: Palette) {
    Card(modifier = modifier.height(112.dp), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = palette.surface)) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = title, tint = palette.accent, modifier = Modifier.size(19.dp))
            Column {
                Text(title, color = palette.secondaryText, fontSize = 11.sp)
                Text(value, color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RecentStatus(status: BatteryStatus, palette: Palette, language: AppLanguage) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = palette.surface)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(palette.accentDark), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, null, tint = palette.accent)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (language == AppLanguage.ENGLISH) "Current session" else "Phiên hiện tại", color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(if (language == AppLanguage.ENGLISH) "${localizedCharging(status, language)} · live data" else "${localizedCharging(status, language)} · dữ liệu trực tiếp", color = palette.secondaryText, fontSize = 11.sp)
            }
            Text(localizedCharging(status, language), color = palette.accent, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SettingsCard(viewModel: DashboardViewModel, preferences: AppPreferences, palette: Palette) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = palette.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (preferences.language == AppLanguage.ENGLISH) "Settings" else "Cài đặt", color = palette.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(10.dp))
            PreferenceRow(title = if (preferences.language == AppLanguage.ENGLISH) "Language" else "Ngôn ngữ", icon = Icons.Default.Language, palette = palette) {
                ChoiceChip("English", preferences.language == AppLanguage.ENGLISH, palette) { viewModel.setLanguage(AppLanguage.ENGLISH) }
                ChoiceChip("Tiếng Việt", preferences.language == AppLanguage.VIETNAMESE, palette) { viewModel.setLanguage(AppLanguage.VIETNAMESE) }
            }
            Spacer(Modifier.height(8.dp))
            PreferenceRow(title = if (preferences.language == AppLanguage.ENGLISH) "Appearance" else "Giao diện", icon = Icons.Default.Settings, palette = palette) {
                ChoiceChip(if (preferences.language == AppLanguage.ENGLISH) "Dark" else "Tối", preferences.theme == AppThemeChoice.DARK, palette) { viewModel.setTheme(AppThemeChoice.DARK) }
                ChoiceChip(if (preferences.language == AppLanguage.ENGLISH) "Light" else "Sáng", preferences.theme == AppThemeChoice.LIGHT, palette) { viewModel.setTheme(AppThemeChoice.LIGHT) }
            }
            Spacer(Modifier.height(16.dp))
            Text(if (preferences.language == AppLanguage.ENGLISH) "Alerts" else "Cảnh báo", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            PreferenceToggle(if (preferences.language == AppLanguage.ENGLISH) "Notify at ${preferences.batteryAlertPercent}% while charging" else "Báo khi đang sạc đạt ${preferences.batteryAlertPercent}%", preferences.batteryAlertEnabled, palette) { viewModel.setBatteryAlertEnabled(it) }
            OutlinedTextField(
                value = preferences.batteryAlertPercent.toString(),
                onValueChange = { it.toIntOrNull()?.let(viewModel::setBatteryAlertPercent) },
                label = { Text(if (preferences.language == AppLanguage.ENGLISH) "Charging alert level (%)" else "Ngưỡng cảnh báo sạc (%)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            PreferenceToggle(if (preferences.language == AppLanguage.ENGLISH) "Overheat warning at ${preferences.temperatureAlertCelsius}°C" else "Cảnh báo quá nhiệt từ ${preferences.temperatureAlertCelsius}°C", preferences.temperatureAlertEnabled, palette) { viewModel.setTemperatureAlertEnabled(it) }
            OutlinedTextField(
                value = preferences.temperatureAlertCelsius.toString(),
                onValueChange = { it.toFloatOrNull()?.let(viewModel::setTemperatureAlertCelsius) },
                label = { Text(if (preferences.language == AppLanguage.ENGLISH) "Temperature alert level (°C)" else "Ngưỡng cảnh báo nhiệt độ (°C)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = if (preferences.designCapacityMah == 0) "" else preferences.designCapacityMah.toString(),
                onValueChange = { text ->
                    if (text.isEmpty()) viewModel.setDesignCapacityMah(0)
                    else text.toIntOrNull()?.takeIf { it in 500..20_000 }?.let(viewModel::setDesignCapacityMah)
                },
                label = { Text(if (preferences.language == AppLanguage.ENGLISH) "Design battery capacity (mAh)" else "Dung lượng thiết kế pin (mAh)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PreferenceToggle(title: String, checked: Boolean, palette: Palette, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = palette.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PreferenceRow(title: String, icon: ImageVector, palette: Palette, choices: @Composable RowScope.() -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = choices)
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, palette: Palette, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(label, color = if (selected) palette.accent else palette.secondaryText, fontSize = 12.sp) }
}

private fun localizedCharging(status: BatteryStatus, language: AppLanguage): String = when {
    language == AppLanguage.ENGLISH && status.chargingState == "Đang sạc" -> "Charging"
    language == AppLanguage.ENGLISH && status.chargingState == "Đang xả" -> "Discharging"
    language == AppLanguage.ENGLISH && status.chargingState == "Đã đầy" -> "Full"
    language == AppLanguage.ENGLISH && status.chargingState == "Chờ" -> "Idle"
    language == AppLanguage.VIETNAMESE && status.chargingState == "Charging" -> "Đang sạc"
    language == AppLanguage.VIETNAMESE && status.chargingState == "Discharging" -> "Đang xả"
    language == AppLanguage.VIETNAMESE && status.chargingState == "Full" -> "Đã đầy"
    else -> status.chargingState
}
