package com.tomogle.functionalstreamingapp

import com.tomogle.functionalstreamingapp.kafka.KafkaProducerConfig
import com.tomogle.functionalstreamingapp.pulsar.PulsarConsumerConfig
import com.tomogle.functionalstreamingapp.pulsar.PulsarProducerConfig

/**
 *
 */
object ConfigUtils {
  // Just hardcoded stubs for now

  def readPulsarConsumerConfig(): PulsarConsumerConfig = PulsarConsumerConfig(
    serviceUrl = "pulsar://localhost:6650",
    topic = "test-topic",
    subscriptionName = "test-subscription"
  )

  def readPulsarProducerConfig(): PulsarProducerConfig = PulsarProducerConfig(
    serviceUrl = "pulsar://localhost:6650",
    topic = "test-topic"
  )

  def readKafkaOutletConfig(): KafkaProducerConfig = KafkaProducerConfig(
    bootstrapServers = "localhost:29092",
    clientId = "myClientId"
  )
}
