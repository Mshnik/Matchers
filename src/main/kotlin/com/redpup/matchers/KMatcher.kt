package com.redpup.com.redpup.matchers

import com.redpup.com.redpup.matchers.impl.CombiningMatcher
import com.redpup.com.redpup.matchers.impl.ConstantMatcher
import com.redpup.com.redpup.matchers.impl.ValueInSetMatcher
import com.redpup.com.redpup.matchers.impl.ValueMatcher
import com.redpup.matchers.proto.Matcher
import kotlin.reflect.KClass

/** A type-safe compiled [Matcher] that accepts inputs of type [T]. */
abstract class KMatcher<in T : Any>(
  val expectedClass: KClass<in T>,
  private var matcher: Matcher? = null,
) {
  /** The proto representation of this [KMatcher], building if necessary. */
  val proto: Matcher
    get() {
      if (matcher == null) {
        matcher = buildProto()
      }
      return requireNotNull(matcher)
    }

  /** Builds the proto representation of this [KMatcher]. */
  protected open fun buildProto(): Matcher =
    throw UnsupportedOperationException("buildProto() not implemented.")

  /** Safely tests an arbitrary [value]. */
  fun match(value: Any): Boolean {
    check(expectedClass.isInstance(value)) {
      "Expected instance of ${expectedClass.simpleName}, found $value."
    }
    @Suppress("UNCHECKED_CAST")
    return matchTyped(value as T)
  }

  /** Safely tests an arbitrary [value]. */
  operator fun invoke(value: Any): Boolean = match(value)

  /** Tests a [value] of known type [T]. */
  abstract fun matchTyped(value: T): Boolean

  /** Tests a [value] of known type [T]. */
  operator fun invoke(value: T): Boolean = matchTyped(value)

  /** Safely narrows an unknown/star-projected matcher to a specific expected type. */
  inline fun <reified R : Any> KMatcher<*>.typed(): KMatcher<R> {
    check(this.expectedClass == R::class) {
      "Type mismatch: Expected Matcher<${R::class.simpleName}>," +
        " but got Matcher<${this.expectedClass.simpleName}>"
    }

    @Suppress("UNCHECKED_CAST")
    return this as KMatcher<R>
  }

  /** Transforms this Matcher by wrapping with [transform]. */
  inline fun <reified T2 : Any> transform(crossinline transform: (T2) -> T): KMatcher<T2> {
    return object : KMatcher<T2>(T2::class) {
      override fun matchTyped(value: T2): Boolean = this@KMatcher.matchTyped(transform(value))
      override fun buildProto(): Matcher = this@KMatcher.proto
    }
  }

  companion object {
    /** Compiles [matcher] into a [KMatcher]. */
    fun compile(matcher: Matcher): KMatcher<*> = when (matcher.matcherCase) {
      Matcher.MatcherCase.CONSTANT_MATCHER -> ConstantMatcher(matcher)
      Matcher.MatcherCase.VALUE_MATCHER -> ValueMatcher.compile(matcher)
      Matcher.MatcherCase.VALUE_IN_SET_MATCHER -> ValueInSetMatcher.compile(matcher)
      Matcher.MatcherCase.COMBININGMATCHER -> CombiningMatcher(matcher)
      Matcher.MatcherCase.MATCHER_NOT_SET -> throw IllegalArgumentException("Unsupported matcher: $matcher")
      null -> throw NullPointerException()
    }
  }
}
