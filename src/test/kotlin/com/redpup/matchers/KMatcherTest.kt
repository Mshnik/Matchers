package com.redpup.matchers

import com.redpup.matchers.KMatcher.Companion.typed
import com.redpup.matchers.proto.Matcher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KMatcherTest {

  // A minimal concrete subclass to test base KMatcher architecture without mocks
  private class ConcreteTestMatcher(
    expected: kotlin.reflect.KClass<String>,
  ) : KMatcher<String>(expected) {
    override fun matchTyped(value: String): Boolean = value == "match"
    override fun buildProto(): Matcher = Matcher.newBuilder().setConstantMatcher(true).build()
  }

  @Test
  fun `match and invoke invoke type validation successfully`() {
    val matcher = ConcreteTestMatcher(String::class)

    assertTrue(matcher.match("match"))
    assertTrue(matcher("match"))
    assertFalse(matcher("mismatch"))
  }

  @Test
  fun `match throws IllegalStateException on runtime type mismatch`() {
    val matcher = ConcreteTestMatcher(String::class)

    val exception = assertThrows<IllegalStateException> {
      matcher.match(42) // Int passed instead of String
    }
    assertTrue(exception.message!!.contains("Expected instance of String"))
  }

  @Test
  fun `typed operator narrows types or throws on mismatch`() {
    val rawMatcher: KMatcher<*> = ConcreteTestMatcher(String::class)

    val typedMatcher: KMatcher<String> = rawMatcher.typed<String>()
    assertNotNull(typedMatcher)

    assertThrows<IllegalStateException> {
      rawMatcher.typed<Int>()
    }
  }

  @Test
  fun `transform pipes alternative types correctly through mapping logic`() {
    val baseMatcher = ConcreteTestMatcher(String::class)
    val transformedMatcher: KMatcher<Int> = baseMatcher.transform<Int> { "match" }

    assertEquals(Int::class, transformedMatcher.expectedClass)
    assertTrue(transformedMatcher.matchTyped(999))
  }

  @Test
  fun `compile fails fast on unset matcher fields`() {
    val emptyProto = Matcher.getDefaultInstance()
    assertThrows<IllegalArgumentException> {
      KMatcher.compile(emptyProto)
    }
  }
}
