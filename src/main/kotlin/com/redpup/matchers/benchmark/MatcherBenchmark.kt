package com.redpup.matchers.benchmark

import com.google.protobuf.TextFormat
import com.redpup.matchers.KMatcher
import com.redpup.matchers.benchmark.proto.MatcherBenchmarks
import com.redpup.matchers.proto.Matcher
import com.redpup.matchers.testing.proto.TestMessage
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class MatcherBenchmark {

  // JMH parameter matrix mapping directly to the 'name' fields in your textproto.
  // Add or remove names here to match your target execution suite.
  @Param(
    "vm_bool",
    "vm_int32",
    "vm_int64",
    "vm_float",
    "vm_double",
    "vm_string",
    "vm_enum",
    "vis_int32",
    "vis_int64",
    "vis_float",
    "vis_double",
    "vis_string",
    "vis_enum",
    "sm_value",
    "sm_starts_with",
    "sm_ends_with",
    "sm_contains",
    "sm_pattern",
    "message_matcher_structural",
    "not_matcher_inversion",
    "cm_any",
    "cm_all",
    "cm_none",
    "cm_empty",
    "cm_size",
    "cm_contains_elements",
    "combine_any",
    "combine_all",
    "combine_none",
  )
  lateinit var benchmarkName: String

  private lateinit var sampleProto: Matcher
  private lateinit var compiledEngine: KMatcher<TestMessage>
  private lateinit var targetInputs: List<TestMessage>

  @Setup(Level.Trial)
  fun setUp() {
    // Locate and parse the textproto from the project resources
    val resourcePath = "/com/redpup/matchers/benchmark/benchmarks.textproto"
    val resourceStream = javaClass.getResourceAsStream(resourcePath)
      ?: throw IllegalArgumentException("Could not find benchmark textproto at: $resourcePath")

    val benchmarksBuilder = MatcherBenchmarks.newBuilder()
    InputStreamReader(resourceStream, Charsets.UTF_8).use { reader ->
      TextFormat.getParser().merge(reader, benchmarksBuilder)
    }

    // Filter out the specific scenario configured for this benchmark thread invocation
    val selectedCase =
      benchmarksBuilder.build().benchmarkList.firstOrNull { it.name == benchmarkName }
        ?: throw IllegalStateException("No defined textproto scenario matching parameter: '$benchmarkName'")

    // Extract rules, pre-compile engine variants, and isolate input evaluation data
    sampleProto = selectedCase.matcher
    compiledEngine = KMatcher.compile(sampleProto)
    targetInputs = selectedCase.inputList
  }

  // --- Case 1: Measuring Compile Time ---
  // Overheads of indexing and generating the KMatcher state machine for this specific rule structure
  @Benchmark
  fun benchmarkCompileTime(): KMatcher<TestMessage> {
    return KMatcher.compile(sampleProto)
  }

  // --- Case 2: Measuring Match Time ---
  // Iterates over all matching and non-matching payload variants defined inside this textproto block
  @Benchmark
  fun benchmarkMatchTime(bh: Blackhole) {
    // A standard indexed loop prevents iterator allocation overhead in the hot path
    for (element in targetInputs) {
      bh.consume(compiledEngine.matchTyped(element))
    }
  }
}
