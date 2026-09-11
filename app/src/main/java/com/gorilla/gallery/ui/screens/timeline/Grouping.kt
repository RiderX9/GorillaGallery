package com.gorilla.gallery.ui.screens.timeline

import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.settings.DateGranularity
import com.gorilla.gallery.ui.components.MediaSection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
private val dayFmtYear = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/** Group a date-sorted item list into header sections per the chosen granularity. */
fun groupByDate(items: List<MediaItem>, granularity: DateGranularity, ascending: Boolean = false): List<MediaSection> {
    if (items.isEmpty()) return emptyList()
    if (granularity == DateGranularity.ALL) {
        return listOf(MediaSection(key = "all", label = "", items = items))
    }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    return items
        .groupBy { item ->
            val date = Instant.ofEpochMilli(item.dateTakenMs).atZone(zone).toLocalDate()
            when (granularity) {
                DateGranularity.DAY -> date.toString()           // yyyy-MM-dd key
                DateGranularity.MONTH -> "${date.year}-${date.monthValue}"
                DateGranularity.YEAR -> date.year.toString()
                DateGranularity.ALL -> "all"
            }
        }
        .map { (key, groupItems) ->
            val date = Instant.ofEpochMilli(groupItems.first().dateTakenMs).atZone(zone).toLocalDate()
            val label = when (granularity) {
                DateGranularity.DAY -> when {
                    date == today -> "Today"
                    date == today.minusDays(1) -> "Yesterday"
                    date.year == today.year -> date.format(dayFmt)
                    else -> date.format(dayFmtYear)
                }
                DateGranularity.MONTH -> date.format(monthFmt)
                DateGranularity.YEAR -> date.year.toString()
                DateGranularity.ALL -> ""
            }
            MediaSection(key = key, label = label, items = groupItems)
        }
        .let { groups ->
            if (ascending) {
                groups.sortedBy { it.items.first().dateTakenMs }
            } else {
                groups.sortedByDescending { it.items.first().dateTakenMs }
            }
        }
}
