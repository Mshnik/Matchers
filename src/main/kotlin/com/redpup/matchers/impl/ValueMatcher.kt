package com.redpup.com.redpup.matchers.impl

import com.redpup.com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.ValueMatcher.ValueCase

/** The implementation for [com.redpup.matchers.proto.ValueMatcher]. */
sealed class ValueMatcher<in T : Any>(
  expectedClass: kotlin.reflect.KClass<T>,
  matcher: Matcher,
) : KMatcher<T>(expectedClass, matcher) {
  companion object {
    /** Compiles [matcher] into a [ValueMatcher]. */
    fun compile(matcher: Matcher): ValueMatcher<*> {
      check(matcher.hasValueMatcher())
      { "Expected value matcher, found $matcher" }
      return when (matcher.valueMatcher.valueCase) {
        ValueCase.BOOL_VALUE -> BooleanValueMatcher(matcher)
        ValueCase.INT32_VALUE -> Int32ValueMatcher(matcher)
        ValueCase.INT64_VALUE -> Int64ValueMatcher(matcher)
        ValueCase.FLOAT_VALUE -> FloatValueMatcher(matcher)
        ValueCase.DOUBLE_VALUE -> DoubleValueMatcher(matcher)
        ValueCase.STRING_VALUE -> StringValueMatcher(matcher)
        ValueCase.VALUE_NOT_SET -> throw IllegalArgumentException("ValueMatcher has no value set: $matcher")
        null -> throw NullPointerException("ValueCase is null")
      }
    }
  }
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Booleans. */
private class BooleanValueMatcher(private val matcher: Matcher) :
  ValueMatcher<Boolean>(Boolean::class, matcher) {
  override fun matchTyped(value: Boolean): Boolean = matcher.valueMatcher.boolValue == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Ints. */
private class Int32ValueMatcher(private val matcher: Matcher) :
  ValueMatcher<Int>(Int::class, matcher) {
  override fun matchTyped(value: Int): Boolean = matcher.valueMatcher.int32Value == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Longs. */
private class Int64ValueMatcher(private val matcher: Matcher) :
  ValueMatcher<Long>(Long::class, matcher) {
  override fun matchTyped(value: Long): Boolean = matcher.valueMatcher.int64Value == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Floats. */
private class FloatValueMatcher(private val matcher: Matcher) :
  ValueMatcher<Float>(Float::class, matcher) {
  override fun matchTyped(value: Float): Boolean = matcher.valueMatcher.floatValue == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Doubles. */
private class DoubleValueMatcher(private val matcher: Matcher) :
  ValueMatcher<Double>(Double::class, matcher) {
  override fun matchTyped(value: Double): Boolean = matcher.valueMatcher.doubleValue == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Strings. */
private class StringValueMatcher(private val matcher: Matcher) :
  ValueMatcher<String>(String::class, matcher) {
  override fun matchTyped(value: String): Boolean = matcher.valueMatcher.stringValue == value
}
