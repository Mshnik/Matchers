package com.redpup.matchers.impl

import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.Internal.EnumLite
import com.google.protobuf.Message
import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.proto.MessageMatcher.FieldMatcher
import com.redpup.matchers.proto.MessageMatcher.FieldMatcher.FieldMatchType.*
import kotlin.reflect.KClass

/** The compiled implementation of [com.redpup.matchers.proto.MessageMatcher]. */
class KMessageMatcher<in T : Message>(
  proto: Matcher,
  expectedClass: KClass<T>,
  instance: T,
) : KMatcher<T>(expectedClass, proto) {
  private val descriptor = instance.descriptorForType
  private val fields: List<KFieldMatcher<*, T>> =
    proto.messageMatcher.fieldsList.map {
      val fieldDescriptor = it.fieldDescriptor(descriptor)
      it.toKFieldMatcher(expectedClass, fieldDescriptor)
    }

  /** Builds a [KFieldMatcher] for the given [FieldMatcher]. */
  private fun FieldMatcher.toKFieldMatcher(
    expectedClass: KClass<T>,
    fieldDescriptor: FieldDescriptor,
  ): KFieldMatcher<*, T> {
    val matcher = compile(matcher, fieldDescriptor.kClass)
    return when (matchType) {
      SINGLE_FIELD -> KSingleFieldMatcher(expectedClass, matcher, fieldDescriptor)
      REPEATED_FIELD_ANY -> KRepeatedAnyFieldMatcher(expectedClass, matcher, fieldDescriptor)
      REPEATED_FIELD_ALL -> KRepeatedAllFieldMatcher(expectedClass, matcher, fieldDescriptor)
      REPEATED_FIELD_NONE -> KRepeatedNoneFieldMatcher(expectedClass, matcher, fieldDescriptor)
      FIELD_MATCH_TYPE_UNSET, UNRECOGNIZED -> throw IllegalArgumentException("FieldMatcher has no matchType set: $proto")
      null -> throw NullPointerException()
    }
  }

  override fun matchTyped(value: T): Boolean = fields.all { it.match(value) }

  companion object {
    /** Compiles [proto] into a [KMessageMatcher] tailored exactly to [expectedClass]. */
    fun <T : Message> compile(proto: Matcher, expectedClass: KClass<T>): KMessageMatcher<T> {
      val defaultInstanceMethod = expectedClass.java.getMethod("getDefaultInstance")

      @Suppress("UNCHECKED_CAST")
      val instance = defaultInstanceMethod.invoke(null) as T
      return KMessageMatcher(proto, expectedClass, instance)
    }

    /** Compiles [proto] into a [KMessageMatcher]. */
    inline fun <reified T : Message> compile(proto: Matcher): KMessageMatcher<T> =
      compile(proto, T::class)

    /** Returns the [FieldDescriptor] for this inside [descriptor], or throws if not found. */
    private fun FieldMatcher.fieldDescriptor(descriptor: Descriptor): FieldDescriptor {
      return requireNotNull(
        if (fieldNumber > 0) {
          descriptor.findFieldByNumber(fieldNumber)
        } else if (fieldName.isNotBlank()) {
          descriptor.findFieldByName(fieldName)
        } else {
          throw IllegalArgumentException("Field $this not found in $descriptor")
        }
      )
    }

    /** Gets the corresponding [KClass] for this [FieldDescriptor]. */
    private val FieldDescriptor.kClass: KClass<*>
      get() = when (type) {
        Type.DOUBLE -> Double::class
        Type.FLOAT -> Float::class
        Type.INT64, Type.UINT64, Type.FIXED64, Type.SFIXED64, Type.SINT64 -> Long::class
        Type.INT32, Type.FIXED32, Type.UINT32, Type.SFIXED32, Type.SINT32 -> Int::class
        Type.BOOL -> Boolean::class
        Type.STRING -> String::class
        Type.BYTES -> ByteString::class

        Type.MESSAGE, Type.GROUP -> {
          try {
            Class.forName(messageType.fullName).kotlin
          } catch (e: ClassNotFoundException) {
            Message::class
          }
        }

        Type.ENUM -> {
          try {
            Class.forName(enumType.fullName).kotlin
          } catch (e: ClassNotFoundException) {
            EnumLite::class
          }
        }

        null -> throw NullPointerException("Field type is null for field: $fullName")
      }
  }
}

/** A matcher on a specific field in a [KMessageMatcher]. */
private sealed class KFieldMatcher<in T : Any, in M : Message>(
  messageClass: KClass<M>,
  val matcher: KMatcher<T>,
  val fieldDescriptor: FieldDescriptor,
) : KMatcher<M>(messageClass)

/** A single field matcher on a specific field in a [KMessageMatcher]. */
private class KSingleFieldMatcher<in T : Any, in M : Message>(
  messageClass: KClass<M>,
  matcher: KMatcher<T>,
  fieldDescriptor: FieldDescriptor,
) : KFieldMatcher<T, M>(messageClass, matcher, fieldDescriptor) {

  init {
    check(!fieldDescriptor.isRepeated) {
      "Expected non-repeated field, found $fieldDescriptor"
    }
  }

  override fun matchTyped(value: M): Boolean = matcher.match(value.getField(fieldDescriptor))
}

/** A repeated field matcher on a specific field in a [KMessageMatcher]. */
private abstract class KRepeatedFieldMatcher<in T : Any, in M : Message>(
  messageClass: KClass<M>,
  matcher: KMatcher<T>,
  fieldDescriptor: FieldDescriptor,
) : KFieldMatcher<T, M>(messageClass, matcher, fieldDescriptor) {

  init {
    check(fieldDescriptor.isRepeated) {
      "Expected repeated field, found $fieldDescriptor"
    }
  }

  override fun matchTyped(value: M): Boolean {
    @Suppress("UNCHECKED_CAST") // Repeated proto fields always are iterable.
    val values = value.getField(fieldDescriptor) as Iterable<Any>
    return matchField(values)
  }

  /** Returns if [matcher] matches the given [field] content. */
  abstract fun matchField(field: Iterable<Any>): Boolean
}

/**
 * A repeated field matcher on a specific field in a [KMessageMatcher] that matches if any
 * value matches.
 */
private class KRepeatedAnyFieldMatcher<in T : Any, in M : Message>(
  messageClass: KClass<M>,
  matcher: KMatcher<T>,
  fieldDescriptor: FieldDescriptor,
) : KRepeatedFieldMatcher<T, M>(messageClass, matcher, fieldDescriptor) {
  override fun matchField(field: Iterable<Any>): Boolean = field.any { matcher.match(it) }
}

/**
 * A repeated field matcher on a specific field in a [KMessageMatcher] that matches if all
 * values match.
 */
private class KRepeatedAllFieldMatcher<in T : Any, in M : Message>(
  messageClass: KClass<M>,
  matcher: KMatcher<T>,
  fieldDescriptor: FieldDescriptor,
) : KRepeatedFieldMatcher<T, M>(messageClass, matcher, fieldDescriptor) {
  override fun matchField(field: Iterable<Any>): Boolean = field.all { matcher.match(it) }
}

/**
 * A repeated field matcher on a specific field in a [KMessageMatcher] that matches if no
 * values match.
 */
private class KRepeatedNoneFieldMatcher<in T : Any, in M : Message>(
  messageClass: KClass<M>,
  matcher: KMatcher<T>,
  fieldDescriptor: FieldDescriptor,
) : KRepeatedFieldMatcher<T, M>(messageClass, matcher, fieldDescriptor) {
  override fun matchField(field: Iterable<Any>): Boolean = field.none { matcher.match(it) }
}
