package com.subzero.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subzero.core.designsystem.R
import com.subzero.core.designsystem.icon.SubzeroIcons
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.model.CurrencyCode
import java.util.Currency

private val CurrencyListMaxHeight = 420.dp

/** Searchable currency picker; shows code, symbol and localized name. */
@Composable
fun SubzeroCurrencySheet(
    selected: CurrencyCode,
    onSelect: (CurrencyCode) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SubzeroTheme.colors
    val spacing = SubzeroTheme.spacing
    val locale = LocalConfiguration.current.locales[0]
    var query by remember { mutableStateOf("") }
    val all = remember(locale) {
        CurrencyCode.available().map { code ->
            val currency = runCatching { Currency.getInstance(code.code) }.getOrNull()
            CurrencyOption(
                code = code,
                symbol = currency?.getSymbol(locale)?.takeIf { it != code.code } ?: "",
                name = currency?.getDisplayName(locale) ?: code.code,
            )
        }
    }
    val filtered = remember(query, all) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) all else all.filter { it.code.code.lowercase().contains(q) || it.name.lowercase().contains(q) }
    }

    SubzeroBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.core_designsystem_currency_title)) {
        SubzeroTextField(
            value = query,
            onValueChange = { query = it },
            label = stringResource(R.string.core_designsystem_currency_search),
            showClearButton = true,
        )
        Spacer(Modifier.height(spacing.sm))
        LazyColumn(modifier = Modifier.heightIn(max = CurrencyListMaxHeight)) {
            items(filtered, key = { it.code.code }) { option ->
                val isSelected = option.code == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SubzeroTheme.shapes.sm)
                        .background(if (isSelected) colors.accentContainer else colors.surfaceElevated)
                        .clickable(role = Role.Button) { onSelect(option.code) }
                        .padding(horizontal = spacing.sm, vertical = spacing.sm)
                        .testTag("currency_${option.code.code}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option.code.code,
                        style = SubzeroTheme.typography.money,
                        color = if (isSelected) colors.accent else colors.textPrimary,
                        modifier = Modifier.width(56.dp),
                    )
                    Text(text = option.symbol, style = SubzeroTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.width(48.dp))
                    Text(
                        text = option.name,
                        style = SubzeroTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) Icon(SubzeroIcons.Check, contentDescription = null, tint = colors.accent)
                }
            }
        }
    }
}

private data class CurrencyOption(val code: CurrencyCode, val symbol: String, val name: String)

