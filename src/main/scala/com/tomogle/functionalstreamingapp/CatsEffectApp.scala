package com.tomogle.functionalstreamingapp

import cats.effect.IO
import com.tomogle.functionalstreamingapp.kafka.KafkaFeed
import com.tomogle.functionalstreamingapp.pulsar.PulsarFeed
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.pulsar.client.api.Consumer

import cats.implicits._

object CatsEffectApp {

  private val logger: Logger = Logger(CatsEffectApp.getClass)

  def main(args: Array[String]): Unit = {
    setupApp().unsafeRunSync()
  }

  private def setupApp(): IO[Unit] = {
    val kafkaProducer: IO[KafkaProducer[String, String]] = KafkaFeed.producerIO()
    val pulsarConsumer: IO[Consumer[String]] = PulsarFeed.consumerIO()

    (pulsarConsumer, kafkaProducer)
      .tupled
        .bracket { resources => {
          val (consumer, producer) = resources
          inToOut(consumer, producer).foreverM
        }}
        { resources =>
          IO {
            val (consumer, producer) = resources
            logger.info("Closing Consumer")
            consumer.close()
            logger.info("Closed Consumer")
            logger.info("Closing Producer")
            producer.close()
            logger.info("Closed Producer")
          }
        }
  }

  def inToOut(consumer: Consumer[String], producer: KafkaProducer[String, String]): IO[Unit] = {
    for {
      message <- IO(consumer.receive())
      _ <- IO(logger.debug("Received message " + message.getValue))
      transformedMessage <- IO(processMessage(message.getValue))
      _ <- IO(logger.debug("Transformed message " + transformedMessage))
      record <- IO(new ProducerRecord("testtopic", message.getKey, transformedMessage))
      sendResult <- IO.async[(RecordMetadata, Option[Exception])](cb => {
        producer.send(record, (metadata: RecordMetadata, exception: Exception) => cb(Right((metadata, Option(exception)))))
      })
      _ <- IO {
        sendResult match {
          case (metaData, Some(exception)) =>
            logger.error(s"Failed message send MessageID: ${message.getMessageId} , Key: ${message.getKey}, Message: $transformedMessage, Exception message: ${exception.getMessage}", exception)
            consumer.negativeAcknowledge(message)
          case (metaData, None) =>
            logger.debug(s"Pulsar message: ${message.getMessageId} acknowledged on kafka at offset: ${metaData.offset()} and partition: ${metaData.partition()}, Message: $transformedMessage")
            consumer.acknowledge(message)
        }
      }
    } yield ()
  }

  def processMessage(message: String): String = s"Hello $message!"

}
