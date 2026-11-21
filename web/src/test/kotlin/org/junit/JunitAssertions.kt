package org.junit

// Let the Android-free regression suite run on Kotlin/JS with kotlin.test.
typealias Test = kotlin.test.Test

object Assert {
    fun <T> assertEquals(expected: T, actual: T) = kotlin.test.assertEquals(expected, actual)

    fun <T> assertEquals(message: String, expected: T, actual: T) =
        kotlin.test.assertEquals(expected, actual, message)

    fun assertEquals(expected: Float, actual: Float, delta: Float) =
        kotlin.test.assertEquals(expected, actual, delta)

    fun <T> assertNotEquals(expected: T, actual: T) = kotlin.test.assertNotEquals(expected, actual)

    fun assertTrue(actual: Boolean) = kotlin.test.assertTrue(actual)

    fun assertTrue(message: String, actual: Boolean) = kotlin.test.assertTrue(actual, message)

    fun assertFalse(actual: Boolean) = kotlin.test.assertFalse(actual)
}
