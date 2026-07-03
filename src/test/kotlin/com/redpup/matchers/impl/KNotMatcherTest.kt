package com.redpup.matchers.impl

import com.google.common.truth.Truth.assertThat
import com.redpup.matchers.KMatcher
import com.redpup.matchers.proto.Matcher
import org.junit.jupiter.api.Test

class KNotMatcherTest {

  @Test
  fun `Inverts wrapped matcher result`() {
    val proto = Matcher.newBuilder().setNotMatcher(
      Matcher.newBuilder().setConstantMatcher(true).build()
    ).build()

    val matcher = KMatcher.compile<Int>(proto)
    assertThat(matcher.match(1)).isFalse()
  }
}
