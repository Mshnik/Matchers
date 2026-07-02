package com.redpup

import com.google.common.truth.Truth.assertThat
import com.redpup.proto.testMessage
import kotlin.test.Test

/** Unit test to check proto compilation and creation. */
class ProtoFrameworkTest {
  @Test
  fun protoFrameworkRuns() {
    val message = testMessage { int32Value = 123 }
    assertThat(message.int32Value).isEqualTo(123)
  }
}
