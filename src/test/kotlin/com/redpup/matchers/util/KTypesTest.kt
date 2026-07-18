package com.redpup.matchers.util

import com.google.common.truth.Truth.assertThat
import com.redpup.matchers.util.KTypes.isComparableOfSelf
import com.redpup.matchers.util.KTypes.isInstance
import com.redpup.matchers.util.KTypes.isSubclassOf
import kotlin.reflect.typeOf
import org.junit.jupiter.api.Test

class KTypesTest {

  interface CustomInterface
  open class ParentClass : CustomInterface
  class ChildClass : ParentClass()

  // Correctly bound: SelfComparable implements Comparable<SelfComparable>
  class SelfComparable : Comparable<SelfComparable> {
    override fun compareTo(other: SelfComparable): Int = 0
  }

  // Incorrectly bound: InvalidComparable implements Comparable<String> instead of itself
  class InvalidComparable : Comparable<String> {
    override fun compareTo(other: String): Int = 0
  }

  // ============================================================================
  // 1. isSubclassOf Tests
  // ============================================================================

  @Test
  fun `isSubclassOf matches direct interfaces and parent classes`() {
    val childType = typeOf<ChildClass>()
    val parentType = typeOf<ParentClass>()

    assertThat(childType.isSubclassOf(ParentClass::class)).isTrue()
    assertThat(childType.isSubclassOf(CustomInterface::class)).isTrue()
    assertThat(parentType.isSubclassOf(CustomInterface::class)).isTrue()
  }

  @Test
  fun `isSubclassOf returns false when type is not a subclass`() {
    val stringType = typeOf<String>()
    assertThat(stringType.isSubclassOf(CustomInterface::class)).isFalse()
  }

  // ============================================================================
  // 2. isComparableOfSelf Tests
  // ============================================================================

  @Test
  fun `isComparableOfSelf matches standard platform types`() {
    assertThat(typeOf<String>().isComparableOfSelf()).isTrue()
    assertThat(typeOf<Int>().isComparableOfSelf()).isTrue()
    assertThat(typeOf<Double>().isComparableOfSelf()).isTrue()
  }

  @Test
  fun `isComparableOfSelf matches correctly implemented custom comparable`() {
    assertThat(typeOf<SelfComparable>().isComparableOfSelf()).isTrue()
  }

  @Test
  fun `isComparableOfSelf returns false for types that do not implement Comparable`() {
    assertThat(typeOf<ChildClass>().isComparableOfSelf()).isFalse()
  }

  @Test
  fun `isComparableOfSelf returns false for types implementing Comparable with a mismatched generic target`() {
    assertThat(typeOf<InvalidComparable>().isComparableOfSelf()).isFalse()
  }

  // ============================================================================
  // 3. isInstance Tests (Basic & Nullability)
  // ============================================================================

  @Test
  fun `isInstance honors nullable type definitions`() {
    val nullableString = typeOf<String?>()
    val nonNullString = typeOf<String>()

    // Null values
    assertThat(nullableString.isInstance(null)).isTrue()
    assertThat(nonNullString.isInstance(null)).isFalse()

    // Non-null values matching base class
    assertThat(nullableString.isInstance("hello")).isTrue()
    assertThat(nonNullString.isInstance("hello")).isTrue()
  }

  @Test
  fun `isInstance rejects mismatched base types`() {
    val stringType = typeOf<String>()
    assertThat(stringType.isInstance(42)).isFalse()
  }

  @Test
  fun `isInstance works identically when using the alternate inline argument ordering`() {
    val stringType = typeOf<String>()
    val input = "hello"

    // Verifies: input isInstance stringType
    assertThat(input isInstance stringType).isTrue()
  }

  // ============================================================================
  // 4. isInstance Deep Generic Inspection Tests
  // ============================================================================

  @Test
  fun `isInstance evaluates homogeneous collection contents structurally`() {
    val stringListType = typeOf<List<String>>()
    val validList = listOf("a", "b", "c")
    val invalidList = listOf("a", 42, "c")

    assertThat(stringListType.isInstance(validList)).isTrue()
    assertThat(stringListType.isInstance(invalidList)).isFalse()
  }

  @Test
  fun `isInstance evaluates map key and value conditions structurally`() {
    val stringIntMapType = typeOf<Map<String, Int>>()
    val validMap = mapOf("one" to 1, "two" to 2)
    val invalidKeyMap = mapOf(1 to 1, "two" to 2)
    val invalidValueMap = mapOf("one" to "1", "two" to 2)

    assertThat(stringIntMapType.isInstance(validMap)).isTrue()
    assertThat(stringIntMapType.isInstance(invalidKeyMap)).isFalse()
    assertThat(stringIntMapType.isInstance(invalidValueMap)).isFalse()
  }

  @Test
  fun `isInstance treats star projected arguments gracefully`() {
    // List<*> passes generic item checks via fallback logic safely
    val starListType = typeOf<List<*>>()
    assertThat(starListType.isInstance(listOf(1, "mixed", true))).isTrue()
  }
}
