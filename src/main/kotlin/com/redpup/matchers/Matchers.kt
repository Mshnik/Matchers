package com.redpup.matchers

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Empty
import com.redpup.matchers.proto.*
import com.redpup.matchers.proto.CollectionMatcherKt.distinctElementsMatcher
import com.redpup.matchers.proto.CombiningMatcher.Combine
import com.redpup.matchers.proto.MessageMatcher.FieldMatcher
import com.redpup.matchers.proto.StringMatcher.CaseSensitivity
import com.redpup.matchers.proto.ValueInSetMatcherKt.doubleValueSet
import com.redpup.matchers.proto.ValueInSetMatcherKt.floatValueSet
import com.redpup.matchers.proto.ValueInSetMatcherKt.int32ValueSet
import com.redpup.matchers.proto.ValueInSetMatcherKt.int64ValueSet
import com.redpup.matchers.proto.ValueInSetMatcherKt.stringValueSet

@DslMarker
annotation class TypedMatcherDsl

// ============================================================================
// 1. Core Type-Safety Guard & Entry Points
// ============================================================================

/**
 * A compile-time phantom type wrapper to enforce strict type-safety boundaries on [Matcher.Builder].
 * It stops developers from mixing incompatible rules (e.g., placing string matchers inside integer blocks).
 */
@TypedMatcherDsl
class TypedMatcherBuilder<T>(val delegate: Matcher.Builder)

/** Instantiates a raw, top-level standalone matcher explicitly locked to a type [T]. */
inline fun <reified T> typedMatcher(crossinline block: TypedMatcherBuilder<T>.() -> Unit): Matcher {
  val builder = Matcher.newBuilder()
  TypedMatcherBuilder<T>(builder).block()
  return builder.build()
}

/** Integrates descriptor-driven [MessageMatcher] directly into any typed or untyped [Matcher.Builder]. */
inline fun Matcher.Builder.messageMatcher(
  descriptor: Descriptor,
  crossinline block: MessageMatcherBuilder.() -> Unit,
): Matcher.Builder = setMessageMatcher(MessageMatcherBuilder.build(descriptor, block))

inline fun MatcherKt.Dsl.messageMatcher(
  descriptor: Descriptor,
  crossinline block: MessageMatcherBuilder.() -> Unit,
) {
  this.messageMatcher = MessageMatcherBuilder.build(descriptor, block)
}

inline fun <T> TypedMatcherBuilder<T>.typedMessageMatcher(
  descriptor: Descriptor,
  crossinline block: MessageMatcherBuilder.() -> Unit,
): TypedMatcherBuilder<T> {
  delegate.messageMatcher(descriptor, block)
  return this
}

// ============================================================================
// 2. Type-Locked Primitive Extensions
// ============================================================================

// --- Int Type Allocations ---
@JvmName("valueInt")
fun TypedMatcherBuilder<Int>.value(target: Int): Matcher.Builder =
  delegate.setValueMatcher(valueMatcher { int32Value = target })

@JvmName("inSetInt")
fun TypedMatcherBuilder<Int>.inSet(elements: Iterable<Int>): Matcher.Builder =
  delegate.setValueInSetMatcher(valueInSetMatcher {
    int32Values = int32ValueSet { values += elements }
  })

@JvmName("anyOfInt")
fun TypedMatcherBuilder<Int>.anyOf(vararg elements: Int): Matcher.Builder = inSet(elements.toList())

// --- Long Type Allocations ---
@JvmName("valueLong")
fun TypedMatcherBuilder<Long>.value(target: Long): Matcher.Builder =
  delegate.setValueMatcher(valueMatcher { int64Value = target })

@JvmName("inSetLong")
fun TypedMatcherBuilder<Long>.inSet(elements: Iterable<Long>): Matcher.Builder =
  delegate.setValueInSetMatcher(valueInSetMatcher {
    int64Values = int64ValueSet { values += elements }
  })

@JvmName("anyOfLong")
fun TypedMatcherBuilder<Long>.anyOf(vararg elements: Long): Matcher.Builder =
  inSet(elements.toList())

// --- Float Type Allocations ---
@JvmName("valueFloat")
fun TypedMatcherBuilder<Float>.value(target: Float): Matcher.Builder =
  delegate.setValueMatcher(valueMatcher { floatValue = target })

@JvmName("inSetFloat")
fun TypedMatcherBuilder<Float>.inSet(elements: Iterable<Float>): Matcher.Builder =
  delegate.setValueInSetMatcher(valueInSetMatcher {
    floatValues = floatValueSet { values += elements }
  })

@JvmName("anyOfFloat")
fun TypedMatcherBuilder<Float>.anyOf(vararg elements: Float): Matcher.Builder =
  inSet(elements.toList())

// --- Double Type Allocations ---
@JvmName("valueDouble")
fun TypedMatcherBuilder<Double>.value(target: Double): Matcher.Builder =
  delegate.setValueMatcher(valueMatcher { doubleValue = target })

@JvmName("inSetDouble")
fun TypedMatcherBuilder<Double>.inSet(elements: Iterable<Double>): Matcher.Builder =
  delegate.setValueInSetMatcher(valueInSetMatcher {
    doubleValues = doubleValueSet { values += elements }
  })

@JvmName("anyOfDouble")
fun TypedMatcherBuilder<Double>.anyOf(vararg elements: Double): Matcher.Builder =
  inSet(elements.toList())

// --- Boolean Type Allocations ---
@JvmName("valueBoolean")
fun TypedMatcherBuilder<Boolean>.value(target: Boolean): Matcher.Builder =
  delegate.setValueMatcher(valueMatcher { boolValue = target })

// --- String Type Allocations ---
@JvmName("valueString")
fun TypedMatcherBuilder<String>.value(target: String): Matcher.Builder =
  delegate.setValueMatcher(valueMatcher { stringValue = target })

@JvmName("inSetString")
fun TypedMatcherBuilder<String>.inSet(elements: Iterable<String>): Matcher.Builder =
  delegate.setValueInSetMatcher(valueInSetMatcher {
    stringValues = stringValueSet { values += elements }
  })

@JvmName("anyOfString")
fun TypedMatcherBuilder<String>.anyOf(vararg elements: String): Matcher.Builder =
  inSet(elements.toList())

fun TypedMatcherBuilder<String>.textEquals(
  text: String,
  case: CaseSensitivity = CaseSensitivity.CASE_SENSITIVE,
): Matcher.Builder = delegate.setStringMatcher(stringMatcher {
  caseSensitive = case
  value = text
})

fun TypedMatcherBuilder<String>.startsWith(
  prefix: String,
  case: CaseSensitivity = CaseSensitivity.CASE_SENSITIVE,
): Matcher.Builder =
  delegate.setStringMatcher(stringMatcher {
    caseSensitive = case
    startsWith = prefix
  })

fun TypedMatcherBuilder<String>.endsWith(
  suffix: String,
  case: CaseSensitivity = CaseSensitivity.CASE_SENSITIVE,
): Matcher.Builder =
  delegate.setStringMatcher(stringMatcher {
    caseSensitive = case
    endsWith = suffix
  })

fun TypedMatcherBuilder<String>.contains(
  substring: String,
  case: CaseSensitivity = CaseSensitivity.CASE_SENSITIVE,
): Matcher.Builder =
  delegate.setStringMatcher(stringMatcher {
    caseSensitive = case
    contains = substring
  })

fun TypedMatcherBuilder<String>.matchesRegex(patternString: String): Matcher.Builder =
  delegate.setStringMatcher(stringMatcher { pattern = patternString })

// --- Generic Constant Fallback ---
fun <T> TypedMatcherBuilder<T>.always(value: Boolean): Matcher.Builder =
  delegate.setConstantMatcher(value)

// ============================================================================
// 3. Type-Safe Logical Combiners & Collections
// ============================================================================

inline fun <T> TypedMatcherBuilder<T>.not(crossinline block: TypedMatcherBuilder<T>.() -> Unit): Matcher.Builder =
  delegate.setNotMatcher(
    Matcher.newBuilder().apply { TypedMatcherBuilder<T>(this).block() }.build()
  )

inline fun <T> TypedMatcherBuilder<T>.anyOf(crossinline block: TypedCombiningMatcherBuilder<T>.() -> Unit): Matcher.Builder =
  delegate.setCombiningMatcher(combiningMatcher {
    combine = Combine.COMBINE_ANY
    matchers += TypedCombiningMatcherBuilder<T>().apply(block).build()
  })

inline fun <T> TypedMatcherBuilder<T>.allOf(crossinline block: TypedCombiningMatcherBuilder<T>.() -> Unit): Matcher.Builder =
  delegate.setCombiningMatcher(combiningMatcher {
    combine = Combine.COMBINE_ALL
    matchers += TypedCombiningMatcherBuilder<T>().apply(block).build()
  })

inline fun <T> TypedMatcherBuilder<T>.noneOf(crossinline block: TypedCombiningMatcherBuilder<T>.() -> Unit): Matcher.Builder =
  delegate.setCombiningMatcher(combiningMatcher {
    combine = Combine.COMBINE_NONE
    matchers += TypedCombiningMatcherBuilder<T>().apply(block).build()
  })

inline fun <E> TypedMatcherBuilder<out Collection<E>>.collectionMatcher(
  crossinline block: TypedCollectionMatcherBuilder<E>.() -> Unit,
): TypedMatcherBuilder<out Collection<E>> {
  val protoCollectionBuilder = CollectionMatcher.newBuilder()
  TypedCollectionMatcherBuilder<E>(protoCollectionBuilder).block()
  delegate.setCollectionMatcher(protoCollectionBuilder.build())
  return this
}

// ============================================================================
// 4. Scoped Message Matcher Builder (Descriptor Routing Engine)
// ============================================================================

/** Scopes field validations directly against an explicit reflective Protobuf structural [Descriptor]. */
@TypedMatcherDsl
class MessageMatcherBuilder @PublishedApi internal constructor(
  @PublishedApi internal val descriptor: Descriptor,
) {
  @PublishedApi internal val builder: MessageMatcher.Builder =
    MessageMatcher.newBuilder().setMessageName(descriptor.fullName)

  companion object {
    inline fun build(
      descriptor: Descriptor,
      block: MessageMatcherBuilder.() -> Unit,
    ): MessageMatcher {
      return MessageMatcherBuilder(descriptor).apply(block).builder.build()
    }
  }

  // --- Dynamic Raw Block Configuration Hooks ---
  infix fun <F> FieldDescriptor.matches(block: TypedMatcherBuilder<F>.() -> Unit) =
    addFieldRule(this, block)

  infix fun <F> String.matches(block: TypedMatcherBuilder<F>.() -> Unit) = addFieldRule(this, block)
  infix fun <F> Int.matches(block: TypedMatcherBuilder<F>.() -> Unit) = addFieldRule(this, block)

  // --- Type-Inferred Field Shorthands ---
  infix fun FieldDescriptor.value(value: Int) = matches { value(value) }
  infix fun FieldDescriptor.value(value: String) = matches { value(value) }
  infix fun FieldDescriptor.value(value: Boolean) = matches { value(value) }

  infix fun String.value(value: Int) = matches { value(value) }
  infix fun String.value(value: String) = matches { value(value) }
  infix fun String.value(value: Boolean) = matches { value(value) }

  // --- Hierarchical Messaging Nested Layout Pass-throughs ---
  inline fun FieldDescriptor.matchesMessage(crossinline block: MessageMatcherBuilder.() -> Unit) {
    matches<Any> {
      delegate.messageMatcher(this@matchesMessage.messageType, block)
    }
  }

  inline infix fun String.matchesMessage(crossinline block: MessageMatcherBuilder.() -> Unit) {
    val descriptorForField = this@matchesMessage.toDescriptor()
    matches<Any> {
      delegate.messageMatcher(descriptorForField.messageType, block)
    }
  }

  // --- Type-Locked Collection Forwarding Blocks ---
  inline fun <E> FieldDescriptor.matchesCollection(crossinline block: TypedCollectionMatcherBuilder<E>.() -> Unit) {
    matches<Collection<E>> {
      val protoCollectionBuilder = CollectionMatcher.newBuilder()
      TypedCollectionMatcherBuilder<E>(protoCollectionBuilder).block()
      delegate.setCollectionMatcher(protoCollectionBuilder.build())
    }
  }

  inline infix fun <E> String.matchesCollection(crossinline block: TypedCollectionMatcherBuilder<E>.() -> Unit) {
    val descriptorForField = this.toDescriptor()
    descriptorForField.matchesCollection(block)
  }

  // --- Internal Wiring Infrastructures ---
  @PublishedApi
  internal fun <F> addFieldRule(
    fieldDescriptor: FieldDescriptor,
    matcherBlock: TypedMatcherBuilder<F>.() -> Unit,
  ) {
    check(fieldDescriptor.containingType == descriptor) { "Field '${fieldDescriptor.fullName}' belongs to a different schema descriptor context than '${descriptor.fullName}'" }
    val childMatcher =
      Matcher.newBuilder().apply { TypedMatcherBuilder<F>(this).matcherBlock() }.build()
    builder.addFields(
      FieldMatcher.newBuilder().setFieldNumber(fieldDescriptor.number)
        .setFieldName(fieldDescriptor.name).setMatcher(childMatcher)
    )
  }

  @PublishedApi
  internal fun <F> addFieldRule(
    fieldName: String,
    matcherBlock: TypedMatcherBuilder<F>.() -> Unit,
  ) {
    val descriptorForField = fieldName.toDescriptor()
    addFieldRule(descriptorForField, matcherBlock)
  }

  @PublishedApi
  internal fun <F> addFieldRule(fieldNumber: Int, matcherBlock: TypedMatcherBuilder<F>.() -> Unit) {
    val fieldDescriptor =
      requireNotNull(descriptor.findFieldByNumber(fieldNumber)) { "Field code #$fieldNumber is invalid inside ${descriptor.fullName}" }
    addFieldRule(fieldDescriptor, matcherBlock)
  }

  @PublishedApi
  internal fun String.toDescriptor(): FieldDescriptor =
    requireNotNull(descriptor.findFieldByName(this)) { "Field identifier string name '$this' is absent inside ${descriptor.fullName}" }
}

// ============================================================================
// 5. Context Implementation Classes
// ============================================================================

@TypedMatcherDsl
class TypedCombiningMatcherBuilder<T> {
  @PublishedApi internal val nestedList = mutableListOf<Matcher>()

  inline fun matches(crossinline block: TypedMatcherBuilder<T>.() -> Unit) {
    nestedList += Matcher.newBuilder().apply { TypedMatcherBuilder<T>(this).block() }.build()
  }

  fun build(): List<Matcher> = nestedList
}

@TypedMatcherDsl
class TypedCollectionMatcherBuilder<E> @PublishedApi internal constructor(
  @PublishedApi internal val target: CollectionMatcher.Builder,
) {
  inline fun any(crossinline block: TypedMatcherBuilder<E>.() -> Unit) {
    target.any = Matcher.newBuilder().apply { TypedMatcherBuilder<E>(this).block() }.build()
  }

  inline fun all(crossinline block: TypedMatcherBuilder<E>.() -> Unit) {
    target.all = Matcher.newBuilder().apply { TypedMatcherBuilder<E>(this).block() }.build()
  }

  inline fun none(crossinline block: TypedMatcherBuilder<E>.() -> Unit) {
    target.none = Matcher.newBuilder().apply { TypedMatcherBuilder<E>(this).block() }.build()
  }

  fun isEmpty() {
    target.empty = Empty.getDefaultInstance()
  }

  inline fun sizeMatches(crossinline block: TypedMatcherBuilder<Int>.() -> Unit) {
    target.size = Matcher.newBuilder().apply { TypedMatcherBuilder<Int>(this).block() }.build()
  }

  inline fun containsDistinct(crossinline block: TypedCombiningMatcherBuilder<E>.() -> Unit) {
    target.containsElements =
      distinctElementsMatcher { matchers += TypedCombiningMatcherBuilder<E>().apply(block).build() }
  }
}
