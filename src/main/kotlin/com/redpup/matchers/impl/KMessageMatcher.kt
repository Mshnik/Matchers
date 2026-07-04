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
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

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
    val types = fieldDescriptor.kClassAndType
    return toKFieldMatcher(expectedClass, fieldDescriptor, types.first, types.second)
  }

  /** Builds a [KFieldMatcher] for the given [FieldMatcher]. */
  private fun <F : Any> FieldMatcher.toKFieldMatcher(
    expectedClass: KClass<T>,
    fieldDescriptor: FieldDescriptor,
    fieldClass: KClass<F>,
    fieldType: KType,
  ): KFieldMatcher<F, T> {
    val matcher = compile(matcher, fieldClass, fieldType)
    return KFieldMatcher(expectedClass, matcher, fieldDescriptor)
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

    /**
     * Gets the appropriate [KClass] and [KType] representation for this field, accounting for
     * repeated elements.
     */
    val FieldDescriptor.kClassAndType: Pair<KClass<*>, KType>
      get() {
        val kClass = kClass
        val elementKType = kClass.createType(nullable = false)

        return if (isRepeated) {
          kClass to List::class.createType(
            listOf(KTypeProjection.invariant(elementKType)),
            nullable = false
          )
        } else {
          kClass to elementKType
        }
      }
  }
}

/** A field matcher on a specific field in a [KMessageMatcher]. */
private class KFieldMatcher<in T : Any, in M : Message>(
  messageClass: KClass<M>,
  private val matcher: KMatcher<T>,
  private val fieldDescriptor: FieldDescriptor,
) : KMatcher<M>(messageClass) {

  override fun matchTyped(value: M): Boolean = matcher.match(value.getField(fieldDescriptor))
}
