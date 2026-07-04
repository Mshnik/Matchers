package com.redpup.matchers.util

import kotlin.reflect.KClass
import kotlin.reflect.KType

/** Utilities for working with [KType]s. */
object KTypes {
  /** Returns true iff this is a subclass of [interfaceTarget]. */
  infix fun KType.isSubclassOf(interfaceTarget: KClass<*>): Boolean {
    // Extract the raw KClass classifier from the KType (e.g., MyMessage from MyMessage)
    val classA = classifier as? KClass<*> ?: return false

    // Fall back to Java's hierarchy inspector to see if classA extends/implements interfaceTarget
    return interfaceTarget.java.isAssignableFrom(classA.java)
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