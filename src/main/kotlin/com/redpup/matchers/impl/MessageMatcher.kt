package com.redpup.matchers.impl
//
// import com.google.protobuf.Descriptors.Descriptor
// import com.google.protobuf.Descriptors.FieldDescriptor
// import com.google.protobuf.Message
// import com.redpup.matchers.KMatcher
// import com.redpup.matchers.proto.Matcher
// import com.redpup.matchers.proto.MessageMatcher.FieldMatcher
//
// /** The compiled implementation of [com.redpup.matchers.proto.MessageMatcher]. */
// class MessageMatcher(
//   proto: Matcher,
//   instance: Message
// ) : KMatcher<Message>(Message::class, proto) {
//   private val descriptor = instance.descriptorForType
//   private val fields: Map<FieldDescriptor, KMatcher<*>> =
//     proto.messageMatcher.fieldsList.associate { it.fieldDescriptor(descriptor) to compile(it.matcher) }
//
//   override fun matchTyped(value: Message): Boolean {
//     check(value.descriptorForType.fullName == proto.messageMatcher.messageName) {
//       "Expected message type ${proto.messageMatcher.messageName}," +
//         " found $value of type ${value.descriptorForType.fullName}"
//     }
//
//     return fields.all { it.value.match(value.getField(it.key)) }
//   }
//
//   companion object {
//     fun compile(proto: Matcher): MessageMatcher {
//
//     }
//
//     /** Returns the [FieldDescriptor] for this inside [descriptor], or throws if not found. */
//     private fun FieldMatcher.fieldDescriptor(descriptor: Descriptor): FieldDescriptor {
//       return requireNotNull(
//         if (fieldNumber > 0) {
//           descriptor.findFieldByNumber(fieldNumber)
//         } else if (fieldName.isNotBlank()) {
//           descriptor.findFieldByName(fieldName)
//         } else {
//           throw IllegalArgumentException("Field $this not found in $descriptor")
//         }
//       )
//     }
//   }
// }
//
