package com.tomogle.functionalstreamingapp.kafka

import java.time.Duration
import java.util.Properties

import cats.effect.IO
import com.tomogle.functionalstreamingapp.ConfigUtils
import com.typesafe.scalalogging.Logger
import monix.eval.Task
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer

import scala.util.Try


/**
 *
 */
object KafkaFeed {

  private val logger: Logger = Logger(KafkaFeed.getClass)

  def producerIO(): IO[KafkaProducer[String, String]] = {
    for {
      _ <- IO(logger.info("Creating Kafka producer"))
      kafkaConfig <- IO(ConfigUtils.readKafkaProducerConfig())
      producer <- IO(KafkaFeed.producer(kafkaConfig))
    } yield producer
  }

  def producerTask(): Task[KafkaProducer[String, String]] =
    for {
      _ <- Task(logger.info("Creating Kafka producer"))
      kafkaConfig <- Task(ConfigUtils.readKafkaProducerConfig())
      producer <- Task(KafkaFeed.producer(kafkaConfig))
    } yield producer

  def producer(config: KafkaProducerConfig): KafkaProducer[String, String] = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "my-producer")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    val producer = new KafkaProducer[String, String](props)
    sys.addShutdownHook {
      logger.warn("Post shutdown hook: Flushing KafkaProducer")
      Try(producer.flush())
      logger.warn("Post shutdown hook: Closing KafkaProducer")
      Try(producer.close(Duration.ofSeconds(5)))
      logger.warn("Post shutdown hook: Finished closing KafkaProducer")
    }
    producer
  }
}
case class KafkaProducerConfig(bootstrapServers: String, clientId: String)