package com.tomogle.functionalstreamingapp.pulsar

import cats.effect.IO
import com.tomogle.functionalstreamingapp.ConfigUtils
import com.typesafe.scalalogging.Logger
import monix.eval.Task
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.StringSchema

object PulsarFeed {

  private val logger: Logger = Logger(PulsarFeed.getClass)

  def main(args: Array[String]): Unit = {
    val pulsarProducer = producer(ConfigUtils.readPulsarProducerConfig())
    for {
      i <- 0 to 100
    } pulsarProducer.send(i.toString)
    pulsarProducer.flush()
    pulsarProducer.close()
    System.exit(0)
  }

  def consumerIO(): IO[Consumer[String]] =
    for {
      _ <- IO(logger.info("Creating Pulsar consumer"))
      pulsarConfig <- IO(ConfigUtils.readPulsarConsumerConfig())
      consumer <- IO(PulsarFeed.consumer(pulsarConfig))
    } yield consumer

  def consumerTask(): Task[Consumer[String]] =
    for {
      _ <- Task(logger.info("Creating Pulsar consumer"))
      pulsarConfig <- Task(ConfigUtils.readPulsarConsumerConfig())
      consumer <- Task(PulsarFeed.consumer(pulsarConfig))
    } yield consumer

  def consumer(config: PulsarConsumerConfig): Consumer[String] = {
    val client = PulsarClient.builder()
      .serviceUrl(config.serviceUrl)
      .build()

    val consumer = client.newConsumer(StringSchema.utf8())
      .topic(config.topic)
      .subscriptionName(config.subscriptionName)
      .subscribe()

    sys.addShutdownHook {
      if (consumer.isConnected) {
        logger.warn("Post shutdown hook: Closing Pulsar Consumer")
        consumer.close()
        logger.warn("Post shutdown hook: Finished closing Pulsar Consumer")
      }
    }
    consumer
  }

  def producer(config: PulsarProducerConfig): Producer[String] = {
    val client = PulsarClient.builder()
      .serviceUrl(config.serviceUrl)
      .build()

    val producer = client.newProducer(StringSchema.utf8())
      .topic(config.topic)
      .create()
    sys.addShutdownHook {
      if(producer.isConnected) {
        logger.warn("Post shutdown hook: Flushing Pulsar Producer")
        producer.flush()
        logger.warn("Post shutdown hook: Closing Pulsar Producer")
        producer.close()
        logger.warn("Post shutdown hook: Finished closing Pulsar Producer")
      }
    }
    producer
  }

}
case class PulsarConsumerConfig(serviceUrl: String, topic: String, subscriptionName: String)
case class PulsarProducerConfig(serviceUrl: String, topic: String)