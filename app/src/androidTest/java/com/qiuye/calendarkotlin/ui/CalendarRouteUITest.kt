package com.qiuye.calendarkotlin.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.qiuye.calendarkotlin.MainActivity
import org.junit.Rule
import org.junit.Test

class CalendarRouteUITest {

    // 启动 MainActivity，它会自动渲染包含我们刚修改过胶囊样式的 CalendarRoute
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun topAppBar_isDisplayed_withCapsuleStyle() {
        // 验证我们刚刚修改的顶部标题栏（胶囊样式）节点是否存在并且可见
        composeTestRule.onNodeWithTag("title_app_bar", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
