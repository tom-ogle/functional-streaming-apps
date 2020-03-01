package com.tomogle.functionalstreamingapp

import java.nio.charset.StandardCharsets

import com.tomogle.functionalstreamingapp.mqtt.MQTTFeed
import com.tomogle.functionalstreamingapp.mqtt.MQTTFeed.Close
import com.tomogle.functionalstreamingapp.mqtt.MQTTFeed.StartPushing
import com.typesafe.scalalogging.Logger
import monix.eval.Task
import monix.reactive.Observable
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttTopic

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

// TODO Replace with another scheduler
import monix.execution.Scheduler.Implicits.global

/**
 *
 */
object MonixMqttToMqttApp {

  private val logger: Logger = Logger(MonixMqttToMqttApp.getClass)

  def main(args: Array[String]): Unit = {
    val future = setupApp().runToFuture
    Await.result(future, Duration.Inf)
    System.exit(0)
  }

  def setupApp(): Task[Unit] = {
    val mqttProducer: Task[(MqttClient, MqttTopic)] = MQTTFeed.producerTask()

    val consumerFeed: Task[(Observable[MqttMessage], StartPushing, Close)] = MQTTFeed.consumerTask()

    mqttProducer.bracket(consumeStream(consumerFeed))(producer => {
      Task {
        val (client, _) = producer
        if(client.isConnected) {
          client.disconnect()
        }
        client.close()
        logger.info("Closed Producer")
      }
    })

  }

  private def consumeStream(consumerFeed: Task[(Observable[MqttMessage], StartPushing, Close)])(producer: (MqttClient, MqttTopic)): Task[Unit] = {
    consumerFeed.bracket(consumer => {
      val (stream, start, _) = consumer
      val (_, topicProducer) = producer
      val resultStream = stream
        .mapEval(consumeMessage(topicProducer))
        .doAfterSubscribe(Task(start()))
      resultStream.completedL
    })(consumer => Task {
      val (_, _, close) = consumer
      close()
      logger.info("Closed Consumer")
    })
  }

  private def consumeMessage(topicProducer: MqttTopic)(message: MqttMessage): Task[Unit] = {
    for {
      messageString <- Task(new String(message.getPayload, StandardCharsets.UTF_8))
      _ <- Task(logger.debug("Received message " + messageString))
      transformedMessage <- Task(processMessage(messageString))
      _ <- Task(logger.debug("Transformed message " + transformedMessage))
      _ <- Task(topicProducer.publish(new MqttMessage(transformedMessage.getBytes( "UTF-8"))))
    } yield ()
  }

  def processMessage(message: String): String = s"Hello $message!"

}
