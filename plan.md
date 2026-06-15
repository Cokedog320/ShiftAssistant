# Plan: Replace Season Characters with Icons

This plan describes how to replace the season characters ("春", "夏", "秋", "冬") on the calendar homepage with premium, matching Material design icons.

---

## 1. Files to Modify

### 1.1 [Palette.kt](file:///C:/Users/qiuye/Desktop/new_calender/Calendar/app/src/main/java/com/qiuye/calendarkotlin/ui/Palette.kt)
We will add an `icon: ImageVector` property to the [SeasonPalette](file:///C:/Users/qiuye/Desktop/new_calender/Calendar/app/src/main/java/com/qiuye/calendarkotlin/ui/Palette.kt#L6) data class and map the correct icons for the four seasons:
*   **Spring (春)**: `Icons.Rounded.LocalFlorist` (Flower)
*   **Summer (夏)**: `Icons.Rounded.WbSunny` (Sun)
*   **Autumn (秋)**: `Icons.Rounded.Eco` (Leaf/Nature)
*   **Winter (冬)**: `Icons.Rounded.AcUnit` (Snowflake)

### 1.2 [CalendarRoute.kt](file:///C:/Users/qiuye/Desktop/new_calender/Calendar/app/src/main/java/com/qiuye/calendarkotlin/ui/CalendarRoute.kt)
We will modify the `CenterAlignedTopAppBar` title area to render the season icon and the current month inside a centered `Row`.

---

## 2. Implementation Steps

### Step 1: Update [Palette.kt](file:///C:/Users/qiuye/Desktop/new_calender/Calendar/app/src/main/java/com/qiuye/calendarkotlin/ui/Palette.kt)

#### Imports to Add:
```kotlin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.AcUnit
```

#### Code Modifications:
Update [SeasonPalette](file:///C:/Users/qiuye/Desktop/new_calender/Calendar/app/src/main/java/com/qiuye/calendarkotlin/ui/Palette.kt#L6) definition and `seasonPalette` function:
```kotlin
data class SeasonPalette(
    val name: String, 
    val icon: ImageVector, 
    val accent: Color, 
    val background: List<Color>
)

private fun seasonPalette(monthValue: Int): SeasonPalette =
    when (monthValue) {
        in 3..5 -> SeasonPalette("春", Icons.Rounded.LocalFlorist, Color(0xFF247A5D), listOf(Color(0xFFF3FBF7), Color(0xFFDDF3E8)))
        in 6..8 -> SeasonPalette("夏", Icons.Rounded.WbSunny, Color(0xFF1769AA), listOf(Color(0xFFF2F8FF), Color(0xFFDCEEFF)))
        in 9..11 -> SeasonPalette("秋", Icons.Rounded.Eco, Color(0xFFB95C09), listOf(Color(0xFFFFF7F0), Color(0xFFFFE7CE)))
        else -> SeasonPalette("冬", Icons.Rounded.AcUnit, Color(0xFF475569), listOf(Color(0xFFF7F8FB), Color(0xFFE5EBF4)))
    }
```

---

### Step 2: Update [CalendarRoute.kt](file:///C:/Users/qiuye/Desktop/new_calender/Calendar/app/src/main/java/com/qiuye/calendarkotlin/ui/CalendarRoute.kt)

#### Code Modifications:
Find the top bar title container inside `CalendarScreen` (around line 302):
```kotlin
                        Text(
                            text = "${palette.name} · ${uiState.currentMonth}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accent,
                        )
```

Replace it with a centered `Row` containing the season icon and current month:
```kotlin
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = palette.icon,
                                contentDescription = palette.name,
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = uiState.currentMonth.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accent,
                            )
                        }
```
