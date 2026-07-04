package com.redpup.matchers

import com.google.protobuf.Message
import com.redpup.matchers.impl.*
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.Matcher.MatcherCase
import com.redpup.matchers.util.KTypes.isInstance
import com.redpup.matchers.util.KTypes.isSubclassOf
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

/** A type-safe compiled [Matcher] that accepts inputs of type [T]. */
abstract class KMatcher<in T : Any>(
  val expectedClass: KClass<in T>,
  private var matcher: Matcher? = null,
  private val expectedType: KType = expectedClass.createType(nullable = false),
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
    check(value isInstance expectedType) {
      "Expected instance of $expectedType, found $value."
    }
    @Suppress("UNCHECKED_CAST")
    return matchTyped(value as T)
  }

  /** Safely tests an arbitrary [value]. */
  operator fun invoke(value: Any): Boolean = match(value)

  /** Tests a [value] of known type [T]. */
  abstract fun matchTyped(value: T): Boolean

  /** Transforms this Matcher by wrapping with [transform]. */
  inline fun <reified T2 : Any> transform(crossinline transform: (T2) -> T): KMatcher<T2> {
    return object : KMatcher<T2>(T2::class) {
      override fun matchTyped(value: T2): Boolean = this@KMatcher.matchTyped(transform(value))
      override fun buildProto(): Matcher = this@KMatcher.proto
    }
  }

  companion object {
    fun <T : Any> compile(
      matcher: Matcher,
      expectedClass: KClass<T>,
      expectedType: KType = expectedClass.createType(),
    ): KMatcher<T> =
      when (matcher.matcherCase) {
        MatcherCase.CONSTANT_MATCHER -> KConstantMatcher(matcher)
        MatcherCase.VALUE_MATCHER -> KValueMatcher.compile(matcher, expectedClass)
        MatcherCase.VALUE_IN_SET_MATCHER -> KValueInSetMatcher.compile(matcher, expectedClass)
        MatcherCase.STRING_MATCHER -> {
          check(expectedClass == String::class) {
            "Expected String class, found $expectedClass"
          }
          @Suppress("UNCHECKED_CAST") // Safe after above validation.
          KStringMatcher.compile(matcher) as KMatcher<T>
        }

        MatcherCase.MESSAGE_MATCHER -> {
          check(expectedType isSubclassOf Message::class) {
            "Expected subtype of Message, found $expectedClass"
          }
          @Suppress("UNCHECKED_CAST") // Safe after above validation.
          KMessageMatcher.compile(matcher, expectedClass as KClass<Message>) as KMatcher<T>
        }

        MatcherCase.NOT_MATCHER -> KNotMatcher(matcher, expectedClass)
        MatcherCase.COLLECTION_MATCHER -> {
          check(expectedType isSubclassOf Collection::class) {
            "Expected subtype of Iterable, found $expectedClass"
          }

          @Suppress("UNCHECKED_CAST") // Safe after above validation.
          KCollectionMatcher.compile(
            matcher,
            expectedClass as KClass<Collection<Any>>,
            expectedType
          ) as KMatcher<T>
        }

        MatcherCase.COMBINING_MATCHER -> KCombiningMatcher(matcher, expectedClass)
        MatcherCase.MATCHER_NOT_SET -> throw IllegalArgumentException("Unsupported matcher: $matcher")
        null -> throw NullPointerException()
      }

    /** Compiles [matcher] into a [KMatcher]. */
    inline fun <reified T : Any> compile(matcher: Matcher): KMatcher<T> = compile(matcher, T::class)

    /** Safely narrows an unknown/star-projected matcher to a specific expected type. */
    inline fun <reified R : Any> KMatcher<*>.typed(): KMatcher<R> {
      check(this.expectedClass == R::class) {
        "Type mismatch: Expected Matcher<${R::class.simpleName}>," +
          " but got Matcher<${this.expectedClass.simpleName}>"
      }

      @Suppress("UNCHECKED_CAST")
      return this as KMatcher<R>
    }
  }
}
