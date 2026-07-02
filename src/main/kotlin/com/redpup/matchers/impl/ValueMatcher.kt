package com.redpup.com.redpup.matchers.impl

import com.redpup.com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.ValueMatcher.ValueCase

/** The implementation for [com.redpup.matchers.proto.ValueMatcher]. */
sealed class ValueMatcher<in T : Any>(
  expectedClass: kotlin.reflect.KClass<T>,
  proto: Matcher,
) : KMatcher<T>(expectedClass, proto) {
  companion object {
    /** Compiles [proto] into a [ValueMatcher]. */
    fun compile(proto: Matcher): ValueMatcher<*> {
      check(proto.hasValueMatcher())
      { "Expected value proto, found $proto" }
      return when (proto.valueMatcher.valueCase) {
        ValueCase.BOOL_VALUE -> BooleanValueMatcher(proto)
        ValueCase.INT32_VALUE -> Int32ValueMatcher(proto)
        ValueCase.INT64_VALUE -> Int64ValueMatcher(proto)
        ValueCase.FLOAT_VALUE -> FloatValueMatcher(proto)
        ValueCase.DOUBLE_VALUE -> DoubleValueMatcher(proto)
        ValueCase.STRING_VALUE -> StringValueMatcher(proto)
        ValueCase.VALUE_NOT_SET -> throw IllegalArgumentException("ValueMatcher has no value set: $proto")
        null -> throw NullPointerException("ValueCase is null")
      }
    }
  }
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Booleans. */
private class BooleanValueMatcher(proto: Matcher) :
  ValueMatcher<Boolean>(Boolean::class, proto) {
  override fun matchTyped(value: Boolean): Boolean = proto.valueMatcher.boolValue == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Ints. */
private class Int32ValueMatcher(proto: Matcher) :
  ValueMatcher<Int>(Int::class, proto) {
  override fun matchTyped(value: Int): Boolean = proto.valueMatcher.int32Value == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Longs. */
private class Int64ValueMatcher(proto: Matcher) :
  ValueMatcher<Long>(Long::class, proto) {
  override fun matchTyped(value: Long): Boolean = proto.valueMatcher.int64Value == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Floats. */
private class FloatValueMatcher(proto: Matcher) :
  ValueMatcher<Float>(Float::class, proto) {
  override fun matchTyped(value: Float): Boolean = proto.valueMatcher.floatValue == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Doubles. */
private class DoubleValueMatcher(proto: Matcher) :
  ValueMatcher<Double>(Double::class, proto) {
  override fun matchTyped(value: Double): Boolean = proto.valueMatcher.doubleValue == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Strings. */
private class StringValueMatcher(proto: Matcher) :
  ValueMatcher<String>(String::class, proto) {
  override fun matchTyped(value: String): Boolean = proto.valueMatcher.stringValue == value
}
