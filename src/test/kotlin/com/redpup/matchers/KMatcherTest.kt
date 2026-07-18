package com.redpup.matchers

import com.redpup.matchers.KMatcher.Companion.typed
import com.redpup.matchers.proto.*
import com.redpup.matchers.testing.proto.TestMessage
import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KMatcherTest {

  // A minimal concrete subclass to test base KMatcher architecture without mocks
  private class ConcreteTestMatcher(
    expected: KClass<String>,
  ) : KMatcher<String>(expected) {
    override fun matchTyped(value: String): Boolean = value == "match"
    override fun buildProto(): Matcher = matcher { constantMatcher = true }
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
    assertTrue(exception.message!!.contains("Expected instance of"))
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
      KMatcher.compile<Any>(emptyProto)
    }
  }

  @Test
  fun `compile routes constant matchers successfully regardless of target class type`() {
    val proto = matcher {
      constantMatcher = true
    }

    val matcher = KMatcher.compile<Int>(proto)

    assertEquals(Any::class, matcher.expectedClass)
    assertTrue(matcher.match(42))
  }

  @Test
  fun `compile routes string matchers successfully when targeted against string type`() {
    val proto = matcher {
      stringMatcher = stringMatcher {
        value = "target"
      }
    }

    val matcher = KMatcher.compile<String>(proto)

    assertEquals(String::class, matcher.expectedClass)
  }

  @Test
  fun `compile throws IllegalStateException when string matcher is assigned to non string targets`() {
    val proto = matcher {
      stringMatcher = stringMatcher {
        value = "target"
      }
    }

    val exception = assertThrows<IllegalStateException> {
      KMatcher.compile<Int>(proto) // Int class requested for a StringMatcher
    }
    assertTrue(exception.message!!.contains("Expected String class"))
  }

  @Test
  fun `compile routes message matchers successfully when targeted against message subtypes`() {
    val proto = matcher {
      messageMatcher = messageMatcher {
        messageTypeName = "com.redpup.TestMessage"
      }
    }

    val matcher = KMatcher.compile<TestMessage>(proto)

    assertEquals(TestMessage::class, matcher.expectedClass)
  }

  @Test
  fun `compile throws IllegalStateException when message matcher is assigned to non message targets`() {
    val proto = matcher {
      messageMatcher = messageMatcher {
        messageTypeName = "com.redpup.TestMessage"
      }
    }

    val exception = assertThrows<IllegalStateException> {
      KMatcher.compile<String>(proto) // String class requested for a MessageMatcher
    }
    assertTrue(exception.message!!.contains("Expected subtype of Message"))
  }

  @Test
  fun `compile routes combining matchers successfully and preserves expected type boundaries`() {
    val proto = matcher {
      combiningMatcher = combiningMatcher {
        combine = CombiningMatcher.Combine.COMBINE_ALL
        matchers += matcher {
          constantMatcher = true
        }
      }
    }

    val matcher = KMatcher.compile<Double>(proto)

    assertEquals(Double::class, matcher.expectedClass)
    assertTrue(matcher.match(3.14))
  }

  @Test
  fun `proto dynamic accessor correctly triggers buildProto lazily`() {
    val matcher = ConcreteTestMatcher(String::class)

    val generatedProto = matcher.proto

    assertNotNull(generatedProto)
    assertTrue(generatedProto.hasConstantMatcher())
    assertTrue(generatedProto.constantMatcher)
  }
}