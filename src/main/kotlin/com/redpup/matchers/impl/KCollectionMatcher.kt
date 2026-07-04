package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.CollectionMatcher.MatcherCase
import com.redpup.matchers.proto.Matcher
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

/** The implementation for [com.redpup.matchers.proto.CollectionMatcher]. */
sealed class KCollectionMatcher<T : Any, I : Iterable<T>>(
  iterableClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KMatcher<I>(iterableClass, proto, type) {
  @Suppress("UNCHECKED_CAST")
  internal val elementClass: KClass<T> =
    type.arguments.firstOrNull()?.type?.classifier as? KClass<T>
      ?: throw IllegalArgumentException("Invalid Collection matcher configuration.")

  companion object {
    /** Compiles [proto] into a [KCollectionMatcher] tailored exactly to [expectedClass]. */
    fun <T : Any, I : Iterable<T>> compile(
      proto: Matcher,
      iterableClass: KClass<in I>,
      type: KType,
    ): KCollectionMatcher<T, I> {
      check(proto.hasCollectionMatcher()) { "Expected CollectionMatcher proto, found $proto" }

      return when (proto.collectionMatcher.matcherCase) {
        MatcherCase.ANY -> KCollectionAnyMatcher(iterableClass, type, proto)
        MatcherCase.ALL -> KCollectionAllMatcher(iterableClass, type, proto)
        MatcherCase.NONE -> KCollectionNoneMatcher(iterableClass, type, proto)
        MatcherCase.MATCHER_NOT_SET -> throw IllegalArgumentException("CollectionMatcher has no matcher set: $proto")
        null -> throw NullPointerException()
      }
    }

    /** Compiles [proto] into a [KCollectionMatcher] tailored exactly to [iterableClass]. */
    fun <E : Any, I : Iterable<E>> compile(
      proto: Matcher,
      elementClass: KClass<E>,
      iterableClass: KClass<I>,
    ): KCollectionMatcher<*, I> = compile(
      proto, iterableClass, iterableClass.createType(
        arguments = listOf(
          KTypeProjection.covariant(elementClass.createType(nullable = false))
        )
      )
    )

    /** Compiles [proto] into a [KCollectionMatcher]. */
    inline fun <reified T : Any, reified I : Iterable<T>> compile(proto: Matcher):
      KCollectionMatcher<*, I> = compile(proto, T::class, I::class)
  }
}

/** The implementation for [com.redpup.matchers.proto.CollectionMatcher.getAny]. */
private class KCollectionAnyMatcher<T : Any, I : Iterable<T>>(
  iterableClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KCollectionMatcher<T, I>(iterableClass, type, proto) {
  private val matcher = KMatcher.compile(proto.collectionMatcher.any, elementClass)

  override fun matchTyped(value: I): Boolean = value.any { matcher.matchTyped(it) }
}

/** The implementation for [com.redpup.matchers.proto.CollectionMatcher.getAll]. */
private class KCollectionAllMatcher<T : Any, I : Iterable<T>>(
  iterableClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KCollectionMatcher<T, I>(iterableClass, type, proto) {
  private val matcher = KMatcher.compile(proto.collectionMatcher.all, elementClass)

  override fun matchTyped(value: I): Boolean = value.all { matcher.matchTyped(it) }
}

/** The implementation for [com.redpup.matchers.proto.CollectionMatcher.getNone]. */
private class KCollectionNoneMatcher<T : Any, I : Iterable<T>>(
  iterableClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KCollectionMatcher<T, I>(iterableClass, type, proto) {
  private val matcher = KMatcher.compile(proto.collectionMatcher.none, elementClass)

  override fun matchTyped(value: I): Boolean = value.none { matcher.matchTyped(it) }
}
