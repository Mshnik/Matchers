package com.redpup.matchers

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.MessageMatcher
import com.redpup.matchers.proto.MessageMatcher.FieldMatcher
import com.redpup.matchers.proto.MessageMatcher.FieldMatcher.FieldMatchType

@DslMarker
annotation class MatcherDsl

/**
 * Integrates the descriptor-driven [MessageMatcher] directly into any standard [Matcher.Builder].
 */
inline fun Matcher.Builder.messageMatcher(
  descriptor: Descriptor,
  crossinline block: MessageMatcherBuilder.() -> Unit,
): Matcher.Builder {
  return setMessageMatcher(MatcherFactory.messageMatcher(descriptor, block))
}

/** Methods for building [Matcher] protos. */
@MatcherDsl
object MatcherFactory {

  /** Constructs a [MessageMatcher] proto message given a root [Descriptor]. */
  inline fun messageMatcher(
    descriptor: Descriptor,
    block: MessageMatcherBuilder.() -> Unit,
  ): MessageMatcher {
    val builder = MessageMatcherBuilder(descriptor)
    builder.block()
    return builder.build()
  }
}

/** A companion builder wrapper that scopes operations around a specific [Descriptor]. */
@MatcherDsl
class MessageMatcherBuilder(private val descriptor: Descriptor) {
  private val builder = MessageMatcher.newBuilder().setMessageName(descriptor.fullName)

  /** Adds a [FieldMatcher] to this builder on a single field for this field. */
  fun FieldDescriptor.matches(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.SINGLE_FIELD, block)

  /** Adds a [FieldMatcher] to this builder on a single field for this field. */
  fun String.matches(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.SINGLE_FIELD, block)

  /** Adds a [FieldMatcher] to this builder on a single field for this field. */
  fun Int.matches(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.SINGLE_FIELD, block)

  /** Adds a [FieldMatcher] to this builder on a repeated any field for this field. */
  fun FieldDescriptor.any(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.REPEATED_FIELD_ANY, block)

  /** Adds a [FieldMatcher] to this builder on a repeated any field for this field. */
  fun String.any(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.REPEATED_FIELD_ANY, block)

  /** Adds a [FieldMatcher] to this builder on a repeated any field for this field. */
  fun Int.any(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.REPEATED_FIELD_ANY, block)

  /** Adds a [FieldMatcher] to this builder on a repeated all field for this field. */
  fun FieldDescriptor.all(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.REPEATED_FIELD_ALL, block)

  /** Adds a [FieldMatcher] to this builder on a repeated all field for this field. */
  fun String.all(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.REPEATED_FIELD_ALL, block)

  /** Adds a [FieldMatcher] to this builder on a repeated all field for this field. */
  fun Int.all(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.REPEATED_FIELD_ALL, block)

  /** Adds a [FieldMatcher] to this builder on a repeated none field for this field. */
  fun FieldDescriptor.none(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.REPEATED_FIELD_NONE, block)

  /** Adds a [FieldMatcher] to this builder on a repeated none field for this field. */
  fun String.repeatedFieldNone(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.REPEATED_FIELD_NONE, block)

  /** Adds a [FieldMatcher] to this builder on a repeated none field for this field. */
  fun Int.repeatedFieldNone(block: Matcher.Builder.() -> Unit) =
    addFieldRule(this, FieldMatchType.REPEATED_FIELD_NONE, block)


  /** Adds a [FieldMatcher] to this builder on the given [fieldDescriptor] with the given [matchType]. */
  private fun addFieldRule(
    fieldDescriptor: FieldDescriptor,
    matchType: FieldMatchType,
    matcherBlock: Matcher.Builder.() -> Unit,
  ) {
    check(fieldDescriptor.containingType == descriptor) {
      "Field '${fieldDescriptor.fullName}' does not belong to message structure '${descriptor.fullName}'"
    }

    val innerMatcher = Matcher.newBuilder().apply(matcherBlock).build()

    val fieldMatcher = FieldMatcher.newBuilder()
      .setFieldNumber(fieldDescriptor.number)
      .setFieldName(fieldDescriptor.name)
      .setMatchType(matchType)
      .setMatcher(innerMatcher)
      .build()

    builder.addFields(fieldMatcher)
  }

  /** Adds a [FieldMatcher] to this builder on the given [fieldName] with the given [matchType]. */
  private fun addFieldRule(
    fieldName: String,
    matchType: FieldMatchType,
    matcherBlock: Matcher.Builder.() -> Unit,
  ) {
    val fieldDescriptor = requireNotNull(descriptor.findFieldByName(fieldName)) {
      "Field '$fieldName' not found inside descriptor '${descriptor.fullName}'"
    }
    addFieldRule(fieldDescriptor, matchType, matcherBlock)
  }

  /** Adds a [FieldMatcher] to this builder on the given [fieldNumber] with the given [matchType]. */
  private fun addFieldRule(
    fieldNumber: Int,
    matchType: FieldMatchType,
    matcherBlock: Matcher.Builder.() -> Unit,
  ) {
    val fieldDescriptor = requireNotNull(descriptor.findFieldByNumber(fieldNumber)) {
      "Field with number $fieldNumber not found inside descriptor '${descriptor.fullName}'"
    }
    addFieldRule(fieldDescriptor, matchType, matcherBlock)
  }

  /** Builds this into a [MessageMatcher]. */
  fun build(): MessageMatcher = builder.build()
}
