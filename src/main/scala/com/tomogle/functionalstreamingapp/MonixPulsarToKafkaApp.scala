package com.tomogle.functionalstreamingapp

import com.tomogle.functionalstreamingapp.kafka.KafkaFeed
import com.tomogle.functionalstreamingapp.pulsar.PulsarFeed
import com.typesafe.scalalogging.Logger
import monix.eval.Task
import monix.reactive.Observable
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

// TODO Replace with another scheduler
import monix.execution.Scheduler.Implicits.global

/**
 *
 */
object MonixPulsarToKafkaApp {

  private val logger: Logger = Logger(MonixPulsarToKafkaApp.getClass)

  def main(args: Array[String]): Unit = {
    val future = setupApp().runToFuture
    Await.result(future, Duration.Inf)
    System.exit(0)
  }

  def setupApp(): Task[Unit] = {
    val kafkaProducer = KafkaFeed.producerTask()
    val pulsarConsumer = PulsarFeed.consumerTask()

    val stream =
      kafkaProducer.bracket(consumeStream(pulsarConsumer))(producer =>
        Task {
          logger.info("Closing Producer")
          producer.close()
        }.delayExecution(3 seconds)
      )

    stream
      .onErrorHandle(t => logger.error("Handling error", t))
      .doOnFinish({
        case Some(throwable) => Task(logger.error(s"App closed down with throwable", throwable))
        case _ => Task(logger.info("App closed down"))
      })
  }

  private def consumeStream(pulsarConsumer: Task[Consumer[String]])(producer: KafkaProducer[String, String]): Task[Unit] = {
    pulsarConsumer.bracket { consumer => {
      val messageStream = Observable.repeatEvalF(Task(consumer.receive()))
      val resultStream = messageStream.mapEval(consumeMessage(consumer, producer))
      resultStream.completedL
    }
    } { consumer =>
      Task {
        logger.info("Closing Pulsar Consumer")
        consumer.close()
      }.delayExecution(3 seconds)
    }
  }

  private def consumeMessage(consumer: Consumer[String], producer: KafkaProducer[String, String])(message: Message[String]): Task[Unit] = {
    for {
      _ <- Task(logger.debug("Received message " + message.getValue))
      transformedMessage <- Task(processMessage(message.getValue))
      _ <- Task(logger.debug("Transformed message " + transformedMessage))
      record <- Task(new ProducerRecord("testtopic", message.getKey, transformedMessage))
      sendResult <- Task.asyncF[(RecordMetadata, Option[Exception])](cb => Task {
        // TODO: Try out using https://github.com/monix/monix-kafka
        producer.send(record, (metadata: RecordMetadata, exception: Exception) => cb(Right(metadata, Option(exception))))
        logger.debug("Sent message async " + transformedMessage)
      })
      _ <- Task {
        println("Matching sendResult")
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
