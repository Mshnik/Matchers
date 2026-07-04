package com.redpup.matchers.impl

import com.redpup.matchers.KMatcher
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
 * of unique elements that satisfy a corresponding set of matchers.
 *
 * This evaluates a bipartite matching assignment problem. It ensures that every sub-matcher
 * specified in the protocol can be paired with exactly one distinct element in the collection.
 *
 * To mitigate the runtime impact of expensive matching operations, this matcher utilizes a
 * lazy, memoized backtracking search algorithm. It guarantees that any single pair of
 * `(Matcher, Element)` is evaluated at most once, and short-circuits execution the moment a
 * violation or a valid path is definitively identified.
 *
 * @param T The type of elements contained within the collection.
 * @param I The collection type extending [Collection] of [T].
 * @property collectionClass The Kotlin reflection class token for the container type [I].
 * @property type The fully realized [KType] representation of the collection, containing generics.
 * @property proto The source [Matcher] configuration protocol buffer message.
 */
private class KCollectionContainsElementsMatcher<T : Any, I : Collection<T>>(
  collectionClass: KClass<in I>,
  type: KType,
  proto: Matcher,
) : KCollectionMatcher<T, I>(collectionClass, type, proto) {
  private val compiledMatchers = proto.collectionMatcher.containsElements.matchersList.map {
    compile(it, elementClass)
  }

  /**
   * Evaluates the incoming collection against the configured distinct element requirements.
   *
   * Accommodates a fail-fast sizing optimization: if the size of the input collection is less
   * than the number of required matchers, it is mathematically impossible to assign them
   * uniquely, and the function returns false immediately without performing any match evaluations.
   *
   * @param value The runtime collection instance to inspect.
   * @return `true` if a complete, unique 1-to-1 matching assignment exists; `false` otherwise.
   */
  override fun matchTyped(value: I): Boolean {
    if (compiledMatchers.size > value.size) return false
    if (compiledMatchers.isEmpty()) return true

    val elements = value.toList()

    // Memoization table: rows represent matchers, columns represent elements.
    // Coordinates initialized to null. null = un-evaluated, true = matched, false = failed.
    val memoCache = Array(compiledMatchers.size) { arrayOfNulls<Boolean>(elements.size) }
    val visitedElements = BooleanArray(elements.size)

    return canAssignUniquelyLazy(
      matcherIndex = 0,
      elements = elements,
      memoCache = memoCache,
      visited = visitedElements
    )
  }

  /**
   * Recursively discovers a valid unique matching layout using lazy backtracking and memoized lookups.
   *
   * Instead of executing an upfront combinatorial scan, match evaluations are triggered on-demand.
   * If a branch backtracks and revisits a previously evaluated pairing, the result is fetched
   * from [memoCache] in O(1) time.
   *
   * @param matcherIndex The index of the current [compiledMatchers] sub-matcher being evaluated.
   * @param elements The localized list representation of the input collection elements.
   * @param memoCache The state matrix storing the history of all computed element-to-matcher validations.
   * @param visited A tracking mask marking which element indices have been tentatively claimed by preceding matchers.
   * @return `true` if the current matcher and all subsequent matchers can be successfully assigned;
   *    `false` otherwise.
   */
  private fun canAssignUniquelyLazy(
    matcherIndex: Int,
    elements: List<T>,
    memoCache: Array<Array<Boolean?>>,
    visited: BooleanArray,
  ): Boolean {
    // Base case: All matchers have successfully secured an element partner.
    if (matcherIndex == compiledMatchers.size) {
      return true
    }

    val currentMatcher = compiledMatchers[matcherIndex]

    for (elementIndex in elements.indices) {
      if (!visited[elementIndex]) {

        // Resolve match evaluation lazily. Fall back to expensive execution only if un-cached.
        val doesMatch = memoCache[matcherIndex][elementIndex] ?: run {
          val result = currentMatcher.matchTyped(elements[elementIndex])
          memoCache[matcherIndex][elementIndex] = result // Commit to cache immediately
          result
        }

        if (doesMatch) {
          visited[elementIndex] = true // Tentatively claim the element

          // Perform a look-ahead sanity check. If the remaining unassigned matchers have
          // already exhausted their available element candidates, fail fast to avoid deep branching.
          if (canRemainingMatchersBeSatisfied(
              matcherIndex + 1,
              elements.size,
              memoCache,
              visited
            )
          ) {
            if (canAssignUniquelyLazy(matcherIndex + 1, elements, memoCache, visited)) {
              return true
            }
          }

          visited[elementIndex] = false // Backtrack: Release the element claim
        }
      }
    }

    // No valid matching found.
    return false
  }

  /**
   * Inspects unassigned matchers down the stack to detect unrecoverable dead ends early.
   *
   * If a subsequent matcher has been fully evaluated against every currently unvisited
   * element, and the [memoCache] reveals that it returned `false` for all of them, this method
   * flags the branch as a guaranteed failure, preventing further wasteful exploration.
   *
   * @param nextMatcherIndex The starting index of the remaining matchers yet to be processed.
   * @param elementCount The total number of elements in the input collection.
   * @param memoCache The shared matrix tracking computed verification results.
   * @param visited The tracking mask indicating which elements are currently locked out.
   * @return `true` if all remaining matchers still have viable candidates available; `false` if
   *    an early failure is guaranteed.
   */
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
            // A pool of un-evaluated candidate elements remains for this matcher
            fullyEvaluated = false
          } else if (cachedResult == true) {
            hasPossibleMatch = true
            break
          }
        }
      }

      // If a matcher has checked all open elements and found zero valid pairings, the search path is broken.
      if (fullyEvaluated && !hasPossibleMatch) {
        return false
      }
    }
    return true
  }
}
