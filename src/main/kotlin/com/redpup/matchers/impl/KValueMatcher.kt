package com.redpup.matchers.impl

import com.google.protobuf.Internal.EnumLite
import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.ValueMatcher.ValueCase
import kotlin.reflect.KClass

/** The implementation for [com.redpup.matchers.proto.ValueMatcher]. */
internal sealed class KValueMatcher<in T : Any>(expectedClass: KClass<T>, proto: Matcher) :
  KMatcher<T>(expectedClass, proto) {

  companion object {
    /** Compiles [proto] into a [KValueMatcher] tailored exactly to [expectedClass]. */
    fun <T : Any> compile(proto: Matcher, expectedClass: KClass<T>): KMatcher<T> {
      check(proto.hasValueMatcher()) { "Expected ValueMatcher proto, found $proto" }

      val case = proto.valueMatcher.valueCase
      val matcher: KMatcher<*> = when {
        EnumLite::class.java.isAssignableFrom(expectedClass.java)
          && case == ValueCase.ENUM_VALUE -> EnumKValueMatcher(proto).transform<EnumLite> { it.number }

        expectedClass == Boolean::class && case == ValueCase.BOOL_VALUE -> BooleanKValueMatcher(proto)
        expectedClass == Int::class && case == ValueCase.INT32_VALUE -> Int32KValueMatcher(proto)
        expectedClass == Long::class && case == ValueCase.INT64_VALUE -> Int64KValueMatcher(proto)
        expectedClass == Float::class && case == ValueCase.FLOAT_VALUE -> FloatKValueMatcher(proto)
        expectedClass == Double::class && case == ValueCase.DOUBLE_VALUE -> DoubleKValueMatcher(proto)
        expectedClass == String::class && case == ValueCase.STRING_VALUE -> StringKValueMatcher(proto)

        case == ValueCase.VALUE_NOT_SET -> throw IllegalArgumentException(
          "ValueMatcher has no value set: $proto"
        )

        else -> throw IllegalArgumentException(
          "Type mismatch or unsupported combination: Cannot match expected class " +
            "'${expectedClass.simpleName}' against serialized proto payload case '$case'."
        )
      }

      @Suppress("UNCHECKED_CAST") // Safe after above validation.
      return matcher as KMatcher<T>
    }

    /** Compiles [proto] into a [KValueMatcher]. */
    inline fun <reified T : Any> compile(proto: Matcher): KMatcher<T> = compile(proto, T::class)
  }
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Booleans. */
private class BooleanKValueMatcher(proto: Matcher) :
  KValueMatcher<Boolean>(Boolean::class, proto) {
  override fun matchTyped(value: Boolean): Boolean = proto.valueMatcher.boolValue == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Ints. */
private class Int32KValueMatcher(proto: Matcher) :
  KValueMatcher<Int>(Int::class, proto) {
  override fun matchTyped(value: Int): Boolean = proto.valueMatcher.int32Value == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Longs. */
private class Int64KValueMatcher(proto: Matcher) :
  KValueMatcher<Long>(Long::class, proto) {
  override fun matchTyped(value: Long): Boolean = proto.valueMatcher.int64Value == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Floats. */
private class FloatKValueMatcher(proto: Matcher) :
  KValueMatcher<Float>(Float::class, proto) {
  override fun matchTyped(value: Float): Boolean = proto.valueMatcher.floatValue == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Doubles. */
private class DoubleKValueMatcher(proto: Matcher) :
  KValueMatcher<Double>(Double::class, proto) {
  override fun matchTyped(value: Double): Boolean = proto.valueMatcher.doubleValue == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Strings. */
private class StringKValueMatcher(proto: Matcher) :
  KValueMatcher<String>(String::class, proto) {
  override fun matchTyped(value: String): Boolean = proto.valueMatcher.stringValue == value
}

/** The implementation for [com.redpup.matchers.proto.ValueMatcher] on Enums. */
private class EnumKValueMatcher(proto: Matcher) :
  KValueMatcher<Int>(Int::class, proto) {
  override fun matchTyped(value: Int): Boolean = proto.valueMatcher.enumValue == value
}