package com.subzero.app.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.component.ChartPoint
import com.subzero.core.designsystem.component.MoneyTextSize
import com.subzero.core.designsystem.component.SectionHeader
import com.subzero.core.designsystem.component.SubzeroBarChart
import com.subzero.core.designsystem.component.SubzeroButton
import com.subzero.core.designsystem.component.SubzeroButtonStyle
import com.subzero.core.designsystem.component.SubzeroCard
import com.subzero.core.designsystem.component.SubzeroCardStyle
import com.subzero.core.designsystem.component.SubzeroChip
import com.subzero.core.designsystem.component.SubzeroDialog
import com.subzero.core.designsystem.component.SubzeroEmptyState
import com.subzero.core.designsystem.component.SubzeroErrorState
import com.subzero.core.designsystem.component.SubzeroHeroSkeleton
import com.subzero.core.designsystem.component.SubzeroInsightCard
import com.subzero.core.designsystem.component.SubzeroLineChart
import com.subzero.core.designsystem.component.SubzeroMoneyText
import com.subzero.core.designsystem.component.SubzeroSubscriptionRow
import com.subzero.core.designsystem.component.SubzeroSubscriptionRowSkeleton
import com.subzero.core.designsystem.component.SubzeroSwitch
import com.subzero.core.designsystem.component.SubzeroTextField
import com.subzero.core.designsystem.component.SubzeroTopBar
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.Money

/**
 * Debug-only gallery of every design-system component with live interactions, for visual QA
 * against the reference on a real device. Not shipped in release builds.
 */
class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dark by remember { mutableStateOf(true) }
            SubzeroTheme(darkTheme = dark) {
                Gallery(dark = dark, onToggleTheme = { dark = !dark })
            }
        }
    }
}

@Composable
private fun Gallery(dark: Boolean, onToggleTheme: () -> Unit) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    var heroMinor by remember { mutableLongStateOf(8748) }
    var chipIndex by remember { mutableStateOf(0) }
    var switchOn by remember { mutableStateOf(true) }
    var fieldValue by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        SubzeroTopBar(title = "Gallery", subtitle = if (dark) "Dark" else "Light", actions = {
            SubzeroSwitch(checked = dark, onCheckedChange = { onToggleTheme() })
        })

        SectionHeader(title = "Hero")
        SubzeroCard(style = SubzeroCardStyle.Hero, onClick = { heroMinor = if (heroMinor == 8748L) 7230 else 8748 }) {
            Text("Monthly spending", style = SubzeroTheme.typography.label, color = colors.textSecondary)
            SubzeroMoneyText(Money(heroMinor, CurrencyCode.USD), size = MoneyTextSize.Hero)
            Text("Tap to change the value", style = SubzeroTheme.typography.caption, color = colors.textTertiary)
        }

        SectionHeader(title = "Buttons")
        SubzeroButton("Primary", onClick = {}, modifier = Modifier.fillMaxWidth(), leadingIcon = SubzeroIcons.Add)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            SubzeroButton("Secondary", onClick = {}, style = SubzeroButtonStyle.Secondary, compact = true)
            SubzeroButton("Ghost", onClick = {}, style = SubzeroButtonStyle.Ghost, compact = true)
            SubzeroButton("Danger", onClick = { showDialog = true }, style = SubzeroButtonStyle.Danger, compact = true)
        }

        SectionHeader(title = "Chips and switch")
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            listOf("All", "Monthly", "Yearly").forEachIndexed { i, label ->
                SubzeroChip(text = label, selected = chipIndex == i, onClick = { chipIndex = i })
            }
            Spacer(Modifier.weight(1f))
            SubzeroSwitch(checked = switchOn, onCheckedChange = { switchOn = it })
        }

        SectionHeader(title = "Text field")
        SubzeroTextField(value = fieldValue, onValueChange = { fieldValue = it }, label = "Service name", placeholder = "e.g. Netflix", showClearButton = true)
        SubzeroTextField(value = "", onValueChange = {}, label = "Price", error = "Enter a price")

        SectionHeader(title = "Rows")
        SubzeroSubscriptionRow("Netflix", Money(1549, CurrencyCode.USD), "/ month", "Next: Sep 16", onClick = {})
        SubzeroSubscriptionRow("Adobe Creative Cloud All Apps", Money(59988, CurrencyCode.USD), "/ year", "Next: Oct 1", onClick = {})
        SubzeroSubscriptionRow("Spotify", Money(1199, CurrencyCode.USD), "/ month", "Paused", statusBadge = "Paused", muted = true)

        SectionHeader(title = "Insights")
        SubzeroInsightCard("You spend \$1,049 / year on subscriptions.", "Across 6 active subscriptions.", SubzeroIcons.TrendUp, onClick = {})
        SubzeroInsightCard("Potential savings", "You told us you rarely use Spotify. That is \$143.88 a year.", SubzeroIcons.Savings, tone = SubzeroTone.Positive, actionLabel = "Review", onAction = {})

        SectionHeader(title = "Charts")
        val points = listOf(
            ChartPoint("Apr", 72f), ChartPoint("May", 74f), ChartPoint("Jun", 80f),
            ChartPoint("Jul", 80f), ChartPoint("Aug", 81f), ChartPoint("Sep", 87f, highlighted = true),
        )
        SubzeroCard { SubzeroBarChart(points = points, contentDescription = "Bar chart") }
        SubzeroCard { SubzeroLineChart(points = points, contentDescription = "Line chart") }

        SectionHeader(title = "Loading")
        SubzeroHeroSkeleton()
        SubzeroCard(contentPadding = 0.dp) { SubzeroSubscriptionRowSkeleton() }

        SectionHeader(title = "States")
        SubzeroCard { SubzeroEmptyState("Your recurring spending starts here.", "Add your first subscription and SUBZERO will build your overview.", actionLabel = "Add subscription", onAction = {}) }
        SubzeroCard { SubzeroErrorState(description = "Your subscription could not be saved.", onRetry = {}) }
        Spacer(Modifier.height(spacing.huge))
    }

    if (showDialog) {
        SubzeroDialog(
            title = "Delete Netflix?",
            message = "This removes the subscription and its history.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
}
