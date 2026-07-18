package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.ValueInSetMatcher.ValuesCase
import kotlin.reflect.KClass

/** The implementation for [com.redpup.matchers.proto.ValueInSetMatcher]. */
internal class KValueInSetMatcher<in T : Any>(
  expectedClass: KClass<T>,
  proto: Matcher,
  private val expectedValues: Set<T>,
) : KMatcher<T>(expectedClass, proto) {

  companion object {
    /** Compiles [proto] into a [KValueInSetMatcher] tailored exactly to [expectedClass]. */
    fun <T : Any> compile(proto: Matcher, expectedClass: KClass<T>): KMatcher<T> {
      check(proto.hasValueInSetMatcher()) { "Expected ValueInSetMatcher proto, found $proto" }

      val valueInSetMatcher = proto.valueInSetMatcher
      val case = valueInSetMatcher.valuesCase

      val matcher: KMatcher<*> = when {
        expectedClass == Int::class && case == ValuesCase.INT32_VALUES ->
          KValueInSetMatcher(Int::class, proto, valueInSetMatcher.int32Values.valuesList.toSet())

        expectedClass == Long::class && case == ValuesCase.INT64_VALUES ->
          KValueInSetMatcher(Long::class, proto, valueInSetMatcher.int64Values.valuesList.toSet())

        expectedClass == Float::class && case == ValuesCase.FLOAT_VALUES ->
          KValueInSetMatcher(Float::class, proto, valueInSetMatcher.floatValues.valuesList.toSet())

        expectedClass == Double::class && case == ValuesCase.DOUBLE_VALUES ->
          KValueInSetMatcher(
            Double::class,
            proto,
            valueInSetMatcher.doubleValues.valuesList.toSet()
          )

        expectedClass == String::class && case == ValuesCase.STRING_VALUES ->
          KValueInSetMatcher(
            String::class,
            proto,
            valueInSetMatcher.stringValues.valuesList.toSet()
          )

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

    /** Compiles [proto] into a [KValueInSetMatcher]. */
    inline fun <reified T : Any> compile(proto: Matcher): KMatcher<T> = compile(proto, T::class)
  }

  override fun matchTyped(value: T): Boolean = value in expectedValues
}
