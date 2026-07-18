package com.redpup.matchers.impl

import com.google.protobuf.Internal.EnumLite
import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.ValueMatcher.ValueCase
import kotlin.reflect.KClass

/** The implementation for [com.redpup.matchers.proto.ValueMatcher]. */
internal class KValueMatcher<in T : Any>(
  expectedClass: KClass<T>,
  proto: Matcher,
  private val expectedValue: T,
) :
  KMatcher<T>(expectedClass, proto) {

  companion object {
    /** Compiles [proto] into a [KValueMatcher] tailored exactly to [expectedClass]. */
    fun <T : Any> compile(proto: Matcher, expectedClass: KClass<T>): KMatcher<T> {
      check(proto.hasValueMatcher()) { "Expected ValueMatcher proto, found $proto" }

      val valueMatcher = proto.valueMatcher
      val case = valueMatcher.valueCase
      val matcher: KMatcher<*> = when {
        expectedClass == Boolean::class && case == ValueCase.BOOL_VALUE ->
          KValueMatcher(
            Boolean::class, proto, valueMatcher.boolValue
          )

        expectedClass == Int::class && case == ValueCase.INT32_VALUE ->
          KValueMatcher(
            Int::class, proto, valueMatcher.int32Value
          )

        expectedClass == Long::class && case == ValueCase.INT64_VALUE ->
          KValueMatcher(
            Long::class, proto, valueMatcher.int64Value
          )

        expectedClass == Float::class && case == ValueCase.FLOAT_VALUE ->
          KValueMatcher(
            Float::class, proto, valueMatcher.floatValue
          )

        expectedClass == Double::class && case == ValueCase.DOUBLE_VALUE ->
          KValueMatcher(
            Double::class, proto, valueMatcher.doubleValue
          )

        expectedClass == String::class && case == ValueCase.STRING_VALUE ->
          KValueMatcher(
            String::class, proto, valueMatcher.stringValue
          )

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

  override fun matchTyped(value: T): Boolean = value == expectedValue
}
