package com.tomogle.functionalstreamingapp

import com.tomogle.functionalstreamingapp.kafka.KafkaProducerConfig
import com.tomogle.functionalstreamingapp.mqtt.MQTTClientConfig
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

  def readKafkaProducerConfig(): KafkaProducerConfig = KafkaProducerConfig(
    bootstrapServers = "localhost:29092",
    clientId = "myClientId"
  )

  def readConsumerMQTTClientConfig(): MQTTClientConfig = MQTTClientConfig(
    serverURI = "tcp://localhost:1883",
    topic = "testtopic"
  )

  def readProducerMQTTClientConfig(): MQTTClientConfig = MQTTClientConfig(
    serverURI = "tcp://localhost:1883",
    topic = "testtopic2"
  )
}
