package com.redpup.com.redpup.matchers.impl

import com.google.protobuf.ProtocolMessageEnum
import com.redpup.com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.ValueInSetMatcher.ValuesCase

/** The implementation for [com.redpup.matchers.proto.ValueInSetMatcher]. */
sealed class ValueInSetMatcher<in T : Any>(
  expectedClass: kotlin.reflect.KClass<T>,
  proto: Matcher,
) : KMatcher<T>(expectedClass, proto) {
  companion object {
    /** Compiles [proto] into a [ValueInSetMatcher]. */
    fun compile(proto: Matcher): KMatcher<*> {
      check(proto.hasValueInSetMatcher()) { "Expected value proto, found $proto" }

      return when (proto.valueInSetMatcher.valuesCase) {
        ValuesCase.INT32_VALUES -> Int32ValueInSetMatcher(proto)
        ValuesCase.INT64_VALUES -> Int64ValueInSetMatcher(proto)
        ValuesCase.FLOAT_VALUES -> FloatValueInSetMatcher(proto)
        ValuesCase.DOUBLE_VALUES -> DoubleValueInSetMatcher(proto)
        ValuesCase.STRING_VALUES -> StringValueInSetMatcher(proto)
        ValuesCase.VALUES_NOT_SET -> throw IllegalArgumentException("ValueInSetMatcher has no value set: $proto")
        ValuesCase.ENUM_VALUES -> Int32ValueInSetMatcher(proto).transform<ProtocolMessageEnum> { it.number }
        null -> throw NullPointerException("ValuesCase is null")
      }
    }
  }
}

/** The implementation for [com.redpup.matchers.proto.ValueInSetMatcher] on Ints. */
private class Int32ValueInSetMatcher(proto: Matcher) : ValueInSetMatcher<Int>(Int::class, proto) {
  private val set: Set<Int> = proto.valueInSetMatcher.int32Values.valuesList.toSet()

  override fun matchTyped(value: Int): Boolean = set.contains(value)
}

/** The implementation for [com.redpup.matchers.proto.ValueInSetMatcher] on Longs. */
private class Int64ValueInSetMatcher(proto: Matcher) : ValueInSetMatcher<Long>(Long::class, proto) {
  private val set: Set<Long> = proto.valueInSetMatcher.int64Values.valuesList.toSet()

  override fun matchTyped(value: Long): Boolean = set.contains(value)
}

/** The implementation for [com.redpup.matchers.proto.ValueInSetMatcher] on Floats. */
private class FloatValueInSetMatcher(proto: Matcher) :
  ValueInSetMatcher<Float>(Float::class, proto) {
  private val set: Set<Float> = proto.valueInSetMatcher.floatValues.valuesList.toSet()

  override fun matchTyped(value: Float): Boolean = set.contains(value)
}

/** The implementation for [com.redpup.matchers.proto.ValueInSetMatcher] on Double. */
private class DoubleValueInSetMatcher(proto: Matcher) :
  ValueInSetMatcher<Double>(Double::class, proto) {
  private val set: Set<Double> = proto.valueInSetMatcher.doubleValues.valuesList.toSet()

  override fun matchTyped(value: Double): Boolean = set.contains(value)
}

/** The implementation for [com.redpup.matchers.proto.ValueInSetMatcher] on String. */
private class StringValueInSetMatcher(proto: Matcher) :
  ValueInSetMatcher<String>(String::class, proto) {
  private val set: Set<String> = proto.valueInSetMatcher.stringValues.valuesList.toSet()

  override fun matchTyped(value: String): Boolean = set.contains(value)
}
