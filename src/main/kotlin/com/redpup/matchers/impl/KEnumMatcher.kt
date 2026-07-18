package com.redpup.matchers.impl

import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Internal.EnumLite
import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import kotlin.reflect.KClass

/**
 * Common base class for evaluating structural matching rules against runtime Enum types.
 */
internal sealed class KEnumMatcher<E : Any>(
  enumClass: KClass<E>,
  private val enumTypeName: String?,
) : KMatcher<E>(enumClass) { // Initializing the primary constructor of KMatcher here

  override fun matchTyped(value: E): Boolean {
    // 1. If an explicit type name validation constraint exists, enforce it
    if (!enumTypeName.isNullOrEmpty()) {
      if (resolveEnumTypeName(value) != enumTypeName) {
        return false
      }
    }
    // 2. Delegate extraction and inner validation to the specialized strategy subclass
    return matchInternal(value)
  }

  /** Tests if this matcher matches [value]. */
  protected abstract fun matchInternal(value: Any): Boolean

  /** Resolves the full type name of [value]. */
  private fun resolveEnumTypeName(value: Any): String {
    return when (value) {
      is EnumValueDescriptor -> value.type.fullName
      is EnumLite -> value.javaClass.canonicalName ?: value.javaClass.name
      is Enum<*> -> value.javaClass.canonicalName ?: value.javaClass.name
      else -> value.javaClass.name
    }
  }

  /** Evaluates an enum element based on its numeric number (Proto) or ordinal (Java/Kotlin). */
  private class NumberEnumMatcher<E : Any>(
    enumClass: KClass<E>,
    enumTypeName: String?,
    private val delegate: KMatcher<Int>,
  ) : KEnumMatcher<E>(enumClass, enumTypeName) {
    override fun matchInternal(value: Any): Boolean {
      val numericValue = when (value) {
        is EnumLite -> value.number
        is Enum<*> -> value.ordinal
        else -> throw IllegalArgumentException(
          "Value of type '${value.javaClass.name}' is not a recognized Enum target."
        )
      }
      return delegate.matchTyped(numericValue)
    }
  }

  /** Evaluates an enum element based on its exact string identifier name. */
  private class NameEnumMatcher<E : Any>(
    enumClass: KClass<E>,
    enumTypeName: String?,
    private val delegate: KMatcher<String>,
  ) : KEnumMatcher<E>(enumClass, enumTypeName) {
    override fun matchInternal(value: Any): Boolean {
      val stringName = when (value) {
        is EnumValueDescriptor -> value.name
        is EnumLite -> (value as Enum<*>).name // Proto enums in JVM implement both EnumLite and native Enum
        is Enum<*> -> value.name
        else -> throw IllegalArgumentException(
          "Value of type '${value.javaClass.name}' is not a recognized Enum target."
        )
      }
      return delegate.matchTyped(stringName)
    }
  }

  companion object {
    /**
     * Compiles an `enum_matcher` rule configuration branch into a specialized [KEnumMatcher] strategy.
     */
    fun <E : Any> compile(
      proto: Matcher,
      targetClass: KClass<E>,
    ): KEnumMatcher<E> {
      require(proto.hasEnumMatcher()) {
        "Provided Matcher proto payload does not contain an enum_matcher block."
      }

      val enumProto = proto.enumMatcher
      val enumTypeName = enumProto.enumTypeName.takeIf { it.isNotEmpty() }

      return when {
        enumProto.hasNumberMatcher() -> {
          val innerMatcher = KMatcher.compile(enumProto.numberMatcher, Int::class)
          NumberEnumMatcher(targetClass, enumTypeName, innerMatcher)
        }

        enumProto.hasNameMatcher() -> {
          val innerMatcher = KMatcher.compile(enumProto.nameMatcher, String::class)
          NameEnumMatcher(targetClass, enumTypeName, innerMatcher)
        }

        else -> throw IllegalArgumentException(
          "EnumMatcher must configure either a number_matcher or a name_matcher condition."
        )
      }
    }

    /**
     * Compiles an `enum_matcher` rule configuration branch into a specialized [KEnumMatcher] strategy.
     */
    inline fun <reified E : Any> compile(proto: Matcher) = compile(proto, E::class)
  }
}