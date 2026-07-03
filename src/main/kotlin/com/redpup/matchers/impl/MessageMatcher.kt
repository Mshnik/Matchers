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

/** The compiled implementation of [com.redpup.matchers.proto.MessageMatcher]. */
class MessageMatcher<in T : Message>(
  proto: Matcher,
  expectedClass: KClass<T>,
  instance: T,
) : KMatcher<T>(expectedClass, proto) {
  private val descriptor = instance.descriptorForType
  private val fields: Map<FieldDescriptor, KMatcher<*>> =
    proto.messageMatcher.fieldsList.associate {
      val fieldDescriptor = it.fieldDescriptor(descriptor)
      fieldDescriptor to compile(it.matcher, fieldDescriptor.kClass)
    }

  override fun matchTyped(value: T): Boolean = fields.all { it.value.match(value.getField(it.key)) }

  companion object {
    /** Compiles [proto] into a [MessageMatcher] tailored exactly to [expectedClass]. */
    fun <T : Message> compile(proto: Matcher, expectedClass: KClass<T>): MessageMatcher<T> {
      val defaultInstanceMethod = expectedClass.java.getMethod("getDefaultInstance")

      @Suppress("UNCHECKED_CAST")
      val instance = defaultInstanceMethod.invoke(null) as T
      return MessageMatcher(proto, expectedClass, instance)
    }

    /** Compiles [proto] into a [MessageMatcher]. */
    inline fun <reified T : Message> compile(proto: Matcher): MessageMatcher<T> =
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

