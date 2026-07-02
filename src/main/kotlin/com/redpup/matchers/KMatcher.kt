package com.redpup.com.redpup.matchers

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

  /** Tests a [value] of known type [T]. */
  abstract fun matchTyped(value: T): Boolean

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
}
