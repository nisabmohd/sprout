# Sprout spacing and size rule (applies to every screen)

Everything sits on a **4dp grid**. Use only the values below, so no screen feels tighter or looser than another.
When in doubt, copy the same element on the **Today** screen.

## Gaps

| Gap | Value | Where |
| --- | --- | --- |
| Screen side padding | 16dp | Every tab and full screen (Today, Habits, Journal, Insights, More, habit detail, New habit, New note) |
| Sheet side padding | 20dp (24dp for a sheet's title row) | Bottom sheets only |
| Header to content | 8dp under the fixed tab header, then the list starts | All five tabs |
| Title to filter chips | 12dp | Journal, Insights |
| Between cards | 8dp | Habit cards on Today, Week, Overall; Manage habits |
| Card title to its paragraph | 8dp | Sample habits banner, About → Free and open source |
| Between rows of a grouped list | 2dp | Journal notes of one day, Insights → By habit, every settings group (More, About, New habit switches). Never a divider line |
| Between sections without a header | 16dp | Score card → habit cards, Insights summary card, habit detail stats → calendar |
| Above a section header | 20dp (none for the first one under the screen header) | Every section header, on every screen: Journal days, Insights, More, habit detail "Notes", New habit, Manage habits "Archived" |
| Section header → its group | 8dp | "Today", "Habit breakdown", "Notes", "Backup & restore", "Icon", "Days" |
| Section header inset | 4dp from the cards' edges | Every section header |
| Legend row | 16dp above and 16dp below | Habits → Overall ("Last 26 weeks" + Done / Partial / Skipped) |
| Inside a card | 16dp padding on all sides | Every card and grouped-list row |
| Tile → text in a card | 12dp | Every habit card and row |
| Name → line under it | 2dp | Every habit card and row |
| Card header → card content | 12dp | Week marks, heatmap, note text, score bar |
| Text → switch or value on its right | at least 12dp | Every settings and form row |
| Header buttons | 8dp apart | Every header |

So a section header always has **20dp above it and 8dp below it**. Where the list adds its own gap between items (8dp on More, 2dp in Journal, 16dp on Insights), the header's padding makes up the rest; it never adds to it.

## Grouping (one rule for every screen, 2026-10-19)

The Journal's day groups are the model. In code: `ui/components/Grouping.kt` (`SectionHeader`, `groupedShape`, `GroupGap`, `SectionGapAbove`, `SectionGapBelow`). Use these; never hand-roll a header or a group.

- **Section header:** the title in `cardTitle` (16sp SemiBold, text colour) on the left; on the right an optional meta in `supporting` (14sp muted: "2 notes", "Every day") or a control ("See all", Score | Streak). 4dp in, 20dp above, 8dp below. No muted 14sp labels, no 22sp titles.
- **Group:** each row is its own white card, rows 2dp apart, 24dp corners on the outside of the group and 6dp between rows (`groupedShape`). **Never a divider line.**
- A row that stands alone is a plain 24dp card.
- Used by: Journal days, Insights (Patterns, Weekly rhythm, Habit breakdown), More (Backup & restore, Appearance, General), About links, habit detail Notes, New habit (sections and the two switches), Manage habits "Archived".

## Sizes

| Element | Size |
| --- | --- |
| Habit tile | 44dp, 14dp corners, 22dp icon. Everywhere a habit is listed: Today, Week, Overall, Journal, Insights, the filter sheet and the habit picker |
| Habit tile in the habit detail header | 56dp, 18dp corners |
| Header icon button | 48dp circle, 22dp icon |
| Touch target | at least 48dp |
| Card corners | 24dp (habit cards on Today 22dp); grouped rows 24dp outside, 6dp between |
| Sheet and dialog corners | 28dp |
| Filter chip | 32dp tall, 8dp corners |
| Day mark | 32dp in Week, 16dp next to an outcome line |
| Progress bar and score segments | 6dp tall |

## Exceptions (only these)
- The month calendar cards (habit detail, date sheets) use 12dp side padding for the same reason.
- Today's habit cards use 12dp horizontal and 8dp vertical padding with a 68dp minimum height, because the fill and the ring reach the card's edge.
- The three stat cards in habit detail use 12dp padding on every side, so "best streak" fits on one line on a 360dp-wide phone.
