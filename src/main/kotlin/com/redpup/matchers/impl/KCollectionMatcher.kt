package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.CollectionMatcher.DistinctElementsMatcher.MatchType
import com.redpup.matchers.proto.CollectionMatcher.MatcherCase
import com.redpup.matchers.proto.Matcher
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

/** The implementation for [com.redpup.matchers.proto.CollectionMatcher]. */
sealed class KCollectionMatcher<T : Any, I : Collection<T>>(
  collectionClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KMatcher<I>(collectionClass, proto, type) {
  @Suppress("UNCHECKED_CAST")
  internal val elementClass: KClass<T> =
    type.arguments.firstOrNull()?.type?.classifier as? KClass<T>
      ?: throw IllegalArgumentException("Invalid Collection matcher configuration.")

  companion object {
    /** Compiles [proto] into a [KCollectionMatcher] tailored exactly to [expectedClass]. */
    fun <T : Any, I : Collection<T>> compile(
      proto: Matcher,
      collectionClass: KClass<in I>,
      type: KType,
    ): KCollectionMatcher<T, I> {
      check(proto.hasCollectionMatcher()) { "Expected CollectionMatcher proto, found $proto" }

      return when (proto.collectionMatcher.matcherCase) {
        MatcherCase.ANY -> KCollectionAnyMatcher(collectionClass, type, proto)
        MatcherCase.ALL -> KCollectionAllMatcher(collectionClass, type, proto)
        MatcherCase.NONE -> KCollectionNoneMatcher(collectionClass, type, proto)
        MatcherCase.EMPTY -> KCollectionEmptyMatcher(collectionClass, type, proto)
        MatcherCase.SIZE -> KCollectionSizeMatcher(collectionClass, type, proto)
        MatcherCase.CONTAINS_ELEMENTS -> KCollectionContainsElementsMatcher(
          collectionClass,
          type,
          proto
        )

        MatcherCase.MATCHER_NOT_SET -> throw IllegalArgumentException("CollectionMatcher has no matcher set: $proto")
        null -> throw NullPointerException()
      }
    }

    /** Compiles [proto] into a [KCollectionMatcher] tailored exactly to [collectionClass]. */
    fun <E : Any, I : Collection<E>> compile(
      proto: Matcher,
      elementClass: KClass<E>,
      collectionClass: KClass<I>,
    ): KCollectionMatcher<*, I> = compile(
      proto, collectionClass, collectionClass.createType(
        arguments = listOf(
          KTypeProjection.covariant(elementClass.createType(nullable = false))
        )
      )
    )

    /** Compiles [proto] into a [KCollectionMatcher]. */
    inline fun <reified T : Any, reified I : Collection<T>> compile(proto: Matcher):
      KCollectionMatcher<*, I> = compile(proto, T::class, I::class)
  }
}

/** The implementation for [com.redpup.matchers.proto.CollectionMatcher.getAny]. */
private class KCollectionAnyMatcher<T : Any, I : Collection<T>>(
  collectionClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KCollectionMatcher<T, I>(collectionClass, type, proto) {
  private val matcher = compile(proto.collectionMatcher.any, elementClass)

  override fun matchTyped(value: I): Boolean = value.any { matcher.matchTyped(it) }
}

/** The implementation for [com.redpup.matchers.proto.CollectionMatcher.getAll]. */
private class KCollectionAllMatcher<T : Any, I : Collection<T>>(
  collectionClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KCollectionMatcher<T, I>(collectionClass, type, proto) {
  private val matcher = compile(proto.collectionMatcher.all, elementClass)

  override fun matchTyped(value: I): Boolean = value.all { matcher.matchTyped(it) }
}

/** The implementation for [com.redpup.matchers.proto.CollectionMatcher.getNone]. */
private class KCollectionNoneMatcher<T : Any, I : Collection<T>>(
  collectionClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KCollectionMatcher<T, I>(collectionClass, type, proto) {
  private val matcher = compile(proto.collectionMatcher.none, elementClass)

  override fun matchTyped(value: I): Boolean = value.none { matcher.matchTyped(it) }
}

/** The implementation for [com.redpup.matchers.proto.CollectionMatcher.getEmpty]. */
private class KCollectionEmptyMatcher<T : Any, I : Collection<T>>(
  collectionClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KCollectionMatcher<T, I>(collectionClass, type, proto) {
  override fun matchTyped(value: I): Boolean = value.isEmpty()
}

/** The implementation for [com.redpup.matchers.proto.CollectionMatcher.getSize]. */
private class KCollectionSizeMatcher<T : Any, I : Collection<T>>(
  collectionClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KCollectionMatcher<T, I>(collectionClass, type, proto) {
  private val matcher = compile(proto.collectionMatcher.size, Int::class)

  override fun matchTyped(value: I): Boolean = matcher.matchTyped(value.size)
}

/**
 * An implementation of [KCollectionMatcher] that validates whether a collection contains a set
 * of unique elements that satisfy a corresponding set of matchers based on the chosen [MatchType].
 */
private class KCollectionContainsElementsMatcher<T : Any, I : Collection<T>>(
  collectionClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KCollectionMatcher<T, I>(collectionClass, type, proto) {

  private val distinctProto = proto.collectionMatcher.containsElements
  private val matchType = distinctProto.matchType
  private val compiledMatchers = distinctProto.matchersList.map {
    compile(it, elementClass)
  }

  init {
    check(matchType != MatchType.MATCH_TYPE_UNKNOWN) {
      "Unsupported matchType: $matchType"
    }
  }

  override fun matchTyped(value: I): Boolean {
    val matchersSize = compiledMatchers.size
    val elementsSize = value.size

    // Fail-fast sizing checks based on strategy type
    when (matchType) {
      MatchType.MATCH_TYPE_SUPERSET_ELEMENTS -> {
        if (matchersSize > elementsSize) return false
      }

      MatchType.MATCH_TYPE_EXACT -> {
        if (matchersSize != elementsSize) return false
      }

      MatchType.MATCH_TYPE_SUPERSET_MATCHERS -> {
        if (elementsSize > matchersSize) return false
      }

      else -> throw UnsupportedOperationException()
    }

    if (matchersSize == 0 && elementsSize == 0) return true
    if (matchersSize == 0 || (elementsSize == 0 && matchType != MatchType.MATCH_TYPE_SUPERSET_MATCHERS)) return false

    val elements = value.toList()

    // Matrix track layout initialization
    val memoCache = Array(matchersSize) { arrayOfNulls<Boolean>(elementsSize) }
    val visitedElements = BooleanArray(elementsSize)

    return canAssignUniquelyLazy(
      matcherIndex = 0,
      elements = elements,
      memoCache = memoCache,
      visited = visitedElements,
      totalElementsVisited = 0
    )
  }

  /**
   * Discovers a valid matching assignments path using lazy backtracking matrix loops.
   */
  private fun canAssignUniquelyLazy(
    matcherIndex: Int,
    elements: List<T>,
    memoCache: Array<Array<Boolean?>>,
    visited: BooleanArray,
    totalElementsVisited: Int,
  ): Boolean {
    // Strategy Base Case Evaluators
    when (matchType) {
      MatchType.MATCH_TYPE_SUPERSET_ELEMENTS, MatchType.MATCH_TYPE_EXACT -> {
        if (matcherIndex == compiledMatchers.size) return true
      }

      MatchType.MATCH_TYPE_SUPERSET_MATCHERS -> {
        // Successful if every input element found a matching partner code down the line
        if (totalElementsVisited == elements.size) return true
        // Out of matchers options but elements remain unvisited
        if (matcherIndex == compiledMatchers.size) return false
      }

      else -> return false
    }

    val currentMatcher = compiledMatchers[matcherIndex]

    for (elementIndex in elements.indices) {
      if (!visited[elementIndex]) {

        val doesMatch = memoCache[matcherIndex][elementIndex] ?: run {
          val result = currentMatcher.matchTyped(elements[elementIndex])
          memoCache[matcherIndex][elementIndex] = result
          result
        }

        if (doesMatch) {
          visited[elementIndex] = true

          // Early pruning skip logic for remaining indices layout
          if (matchType != MatchType.MATCH_TYPE_SUPERSET_MATCHERS) {
            if (!canRemainingMatchersBeSatisfied(
                matcherIndex + 1,
                elements.size,
                memoCache,
                visited
              )
            ) {
              visited[elementIndex] = false
              continue
            }
          }

          if (canAssignUniquelyLazy(
              matcherIndex + 1,
              elements,
              memoCache,
              visited,
              totalElementsVisited + 1
            )
          ) {
            return true
          }

          visited[elementIndex] = false // Backtrack path release
        }
      }
    }

    // For MATCH_TYPE_SUPERSET_MATCHERS, a matcher is allowed to match *nothing* // if there are surplus matchers left over. Skip it and move to the next sub-matcher.
    if (matchType == MatchType.MATCH_TYPE_SUPERSET_MATCHERS) {
      if (canAssignUniquelyLazy(
          matcherIndex + 1,
          elements,
          memoCache,
          visited,
          totalElementsVisited
        )
      ) {
        return true
      }
    }

    return false
  }

  private fun canRemainingMatchersBeSatisfied(
    nextMatcherIndex: Int,
    elementCount: Int,
    memoCache: Array<Array<Boolean?>>,
    visited: BooleanArray,
  ): Boolean {
    for (m in nextMatcherIndex until compiledMatchers.size) {
      var fullyEvaluated = true
      var hasPossibleMatch = false

      for (e in 0 until elementCount) {
        if (!visited[e]) {
          val cachedResult = memoCache[m][e]
          if (cachedResult == null) {
            fullyEvaluated = false
          } else if (cachedResult == true) {
            hasPossibleMatch = true
            break
          }
        }
      }

      if (fullyEvaluated && !hasPossibleMatch) {
        return false
      }
    }
    return true
  }
}
