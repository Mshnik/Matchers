package com.redpup.matchers.util

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.starProjectedType

/** Utilities for working with [KType]s. */
internal object KTypes {
  /** Returns true iff this is a subclass of [interfaceTarget]. */
  infix fun KType.isSubclassOf(interfaceTarget: KClass<*>): Boolean {
    // Extract the raw KClass classifier from the KType (e.g., MyMessage from MyMessage)
    val classA = classifier as? KClass<*> ?: return false

    // Fall back to Java's hierarchy inspector to see if classA extends/implements interfaceTarget
    return interfaceTarget.java.isAssignableFrom(classA.java)
  }

  /**
   * Checks if this KType implements Comparable of itself (i.e., T : Comparable<T>).
   * Returns true for types like String, Int, Instant, or custom data classes implementing Comparable.
   */
  fun KType.isComparableOfSelf(): Boolean {
    // Extract the raw class classifier (e.g., String, Int, or custom classes)
    val rawClass = this.classifier as? KClass<*> ?: return false

    // FIX: Use .javaObjectType instead of .java to ensure primitives (like int)
    // are checked via their boxed wrapper counterparts (like java.lang.Integer).
    if (!Comparable::class.java.isAssignableFrom(rawClass.javaObjectType)) {
      return false
    }

    // Scan all supertypes in Kotlin's reflection model to find the specific Comparable instantiation
    val comparableSupertype = (rawClass.allSupertypes + rawClass.starProjectedType)
      .firstOrNull { it.classifier == Comparable::class } ?: return false

    // Inspect the generic argument of Comparable<T>
    val genericArgumentType = comparableSupertype.arguments.firstOrNull()?.type ?: return false

    // Verify that the generic argument matches the original raw class type
    return genericArgumentType.classifier == rawClass
  }

  /** Checks if this is a structurally valid instance of [type]. */
  infix fun Any?.isInstance(type: KType): Boolean {
    return type isInstance this
  }

  /** Checks if [value] is a structurally valid instance of this [KType]. */
  // TODO: Improve performance, probably? Or allow disabling.
  infix fun KType.isInstance(value: Any?): Boolean {
    // 1. Nullability check
    if (value == null) return isMarkedNullable

    // 2. Base Class (Classifier) check
    val baseClass = classifier as? KClass<*> ?: return false
    if (!baseClass.isInstance(value)) return false

    // 3. Generics / Type Arguments check
    if (arguments.isEmpty()) return true

    // Handle collections (Iterable/List/Set) deep checking
    if (value is Iterable<*>) {
      val expectedElementType =
        arguments.firstOrNull()?.type ?: return true // Fallback if star-projected

      // Perform a deep scan on the collection elements
      return value.all { element -> expectedElementType.isInstance(element) }
    }

    // Handle Maps
    if (value is Map<*, *>) {
      val expectedKeyType = arguments.getOrNull(0)?.type ?: return true
      val expectedValueType = arguments.getOrNull(1)?.type ?: return true

      return value.all { (k, v) -> expectedKeyType.isInstance(k) && expectedValueType.isInstance(v) }
    }

    return true
  }
}