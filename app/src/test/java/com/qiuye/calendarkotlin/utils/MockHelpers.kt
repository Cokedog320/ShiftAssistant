package com.qiuye.calendarkotlin.utils

import io.mockk.MockKAnnotations

/**
 * 包含 MockK 相关的辅助方法。
 */
object MockHelpers {
    /**
     * 快捷初始化类中的 @MockK 注解属性，并默认 relaxUnitFun = true（即不需要手动 mock 返回 Unit 的方法）
     */
    fun initMocks(testClass: Any) {
        MockKAnnotations.init(testClass, relaxUnitFun = true)
    }
}
