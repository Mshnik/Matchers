package com.redpup.matchers.impl

import com.google.protobuf.Internal
import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.ComparisonMatcher.Comparison
import com.redpup.matchers.proto.ComparisonMatcher.ValueCase
import com.redpup.matchers.proto.Matcher
import kotlin.reflect.KClass

/** The implementation for [com.redpup.matchers.proto.ComparisonMatcher]. */
internal class KComparisonMatcher<in T : Comparable<T>>(
  expectedClass: KClass<T>,
  proto: Matcher,
  private val comparison: Comparison,
  private val expectedValue: T,
) :
  KMatcher<T>(expectedClass, proto) {

  companion object {
    /** Compiles [proto] into a [KComparisonMatcher] tailored exactly to [expectedClass]. */
    fun <T : Comparable<T>> compile(proto: Matcher, expectedClass: KClass<T>): KMatcher<T> {
      check(proto.hasComparisonMatcher()) { "Expected ComparisonMatcher proto, found $proto" }

      val comparisonMatcher = proto.comparisonMatcher
      val case = comparisonMatcher.valueCase
      val comparison = comparisonMatcher.comparison

      val matcher: KMatcher<*> = when {
        Internal.EnumLite::class.java.isAssignableFrom(expectedClass.java)
          && case == ValueCase.ENUM_VALUE -> KComparisonMatcher(
          Int::class,
          proto,
          comparison,
          comparisonMatcher.enumValue
        ).transform<Internal.EnumLite> { it.number }

        expectedClass == Int::class && case == ValueCase.INT32_VALUE ->
          KComparisonMatcher(Int::class, proto, comparison, comparisonMatcher.int32Value)

        expectedClass == Long::class && case == ValueCase.INT64_VALUE ->
          KComparisonMatcher(Long::class, proto, comparison, comparisonMatcher.int64Value)

        expectedClass == Float::class && case == ValueCase.FLOAT_VALUE ->
          KComparisonMatcher(Float::class, proto, comparison, comparisonMatcher.floatValue)

        expectedClass == Double::class && case == ValueCase.DOUBLE_VALUE ->
          KComparisonMatcher(Double::class, proto, comparison, comparisonMatcher.doubleValue)

        expectedClass == String::class && case == ValueCase.STRING_VALUE ->
          KComparisonMatcher(String::class, proto, comparison, comparisonMatcher.stringValue)

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

    /** Compiles [proto] into a [KComparisonMatcher]. */
    inline fun <reified T : Comparable<T>> compile(proto: Matcher): KMatcher<T> =
      compile(proto, T::class)

    /** Checks if [value], [expected] adheres to this comparison. */
    private fun <T : Comparable<T>> Comparison.check(value: T, expected: T): Boolean = when (this) {
      Comparison.COMPARISON_EQ -> value == expected
      Comparison.COMPARISON_NE -> value != expected
      Comparison.COMPARISON_LT -> value < expected
      Comparison.COMPARISON_LE -> value <= expected
      Comparison.COMPARISON_GE -> value >= expected
      Comparison.COMPARISON_GT -> value > expected
      Comparison.COMPARISON_UNSET, Comparison.UNRECOGNIZED -> throw IllegalArgumentException("Unsupported comparison: $this")
    }
  }

  override fun matchTyped(value: T): Boolean = comparison.check(value, expectedValue)
}
