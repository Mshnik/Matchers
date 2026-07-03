package com.redpup.matchers.impl

import com.google.protobuf.Internal.EnumLite
import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.ValueInSetMatcher.ValuesCase
import kotlin.reflect.KClass

/** The implementation for [com.redpup.matchers.proto.ValueInSetMatcher]. */
internal sealed class ValueInSetMatcher<in T : Any>(expectedClass: KClass<T>, proto: Matcher) :
  KMatcher<T>(expectedClass, proto) {

  companion object {
    /** Compiles [proto] into a [ValueInSetMatcher] tailored exactly to [expectedClass]. */
    fun <T : Any> compile(proto: Matcher, expectedClass: KClass<T>): KMatcher<T> {
      check(proto.hasValueInSetMatcher()) { "Expected ValueInSetMatcher proto, found $proto" }

      val case = proto.valueInSetMatcher.valuesCase

      val matcher: KMatcher<*> = when {
        EnumLite::class.java.isAssignableFrom(expectedClass.java)
          && case == ValuesCase.ENUM_VALUES -> EnumValueInSetMatcher(proto).transform<EnumLite> { it.number }

        expectedClass == Int::class
          && case == ValuesCase.INT32_VALUES -> Int32ValueInSetMatcher(proto)

        expectedClass == Long::class
          && case == ValuesCase.INT64_VALUES -> Int64ValueInSetMatcher(proto)

        expectedClass == Float::class
          && case == ValuesCase.FLOAT_VALUES -> FloatValueInSetMatcher(proto)

        expectedClass == Double::class
          && case == ValuesCase.DOUBLE_VALUES -> DoubleValueInSetMatcher(proto)

        expectedClass == String::class
          && case == ValuesCase.STRING_VALUES -> StringValueInSetMatcher(proto)

        case == ValuesCase.VALUES_NOT_SET ->
          throw IllegalArgumentException("ValueInSetMatcher has no values set: $proto")

        else -> throw IllegalArgumentException(
          "Type mismatch or unsupported combination: Cannot match expected class set " +
            "'${expectedClass.simpleName}' against serialized proto payload case '$case'."
        )
      }

      @Suppress("UNCHECKED_CAST") // Safe after above validation.
      return matcher as KMatcher<T>
    }

    /** Compiles [proto] into a [ValueInSetMatcher]. */
    inline fun <reified T : Any> compile(proto: Matcher): KMatcher<T> = compile(proto, T::class)
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

/** The implementation for [com.redpup.matchers.proto.ValueInSetMatcher] on Enums. */
private class EnumValueInSetMatcher(proto: Matcher) : ValueInSetMatcher<Int>(Int::class, proto) {
  private val set: Set<Int> = proto.valueInSetMatcher.enumValues.valuesList.toSet()

  override fun matchTyped(value: Int): Boolean = set.contains(value)
}
