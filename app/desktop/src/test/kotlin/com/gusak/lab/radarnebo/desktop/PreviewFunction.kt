package com.gusak.lab.radarnebo.desktop

import androidx.compose.runtime.Composer
import java.lang.reflect.Method

/** A `@Preview` function found in the shared sources. */
internal data class PreviewFunction(
    val name: String,
    val className: String,
    val functionName: String,
    val widthDp: Int?,
    val heightDp: Int?,
) {
    val id: String get() = "${className.substringAfterLast('.')}-$functionName"

    /** Composable functions compile to static methods taking (Composer, Int changed-flags). */
    fun method(): Method = Class.forName(className).declaredMethods
        .single {
            it.name == functionName &&
                it.parameterTypes.contentEquals(arrayOf(Composer::class.java, Int::class.javaPrimitiveType))
        }
        .apply { isAccessible = true }
}
