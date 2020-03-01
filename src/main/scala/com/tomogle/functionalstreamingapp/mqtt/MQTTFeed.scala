package com.tomogle.functionalstreamingapp.mqtt

import com.tomogle.functionalstreamingapp.ConfigUtils
import com.typesafe.scalalogging.Logger
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.Pipe
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttTopic
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

import scala.util.Try

/**
 *
 */
object MQTTFeed {

  private val logger: Logger = Logger(MQTTFeed.getClass)

  type StartPushing = () => Unit
  type Close = () => Unit

  def main(args: Array[String]): Unit = {
    val (client, topic) = producer(ConfigUtils.readConsumerMQTTClientConfig())
    try {
      for {
        i <- 0 to 100
      } {
        logger.info(s"Publishing message $i")
        topic.publish(new MqttMessage(i.toString.getBytes("UTF-8")))
      }
    } finally {
      client.disconnect()
      client.close()
    }
    System.exit(0)
  }

  def producerTask(): Task[(MqttClient, MqttTopic)] =
    for {
      config <- Task(ConfigUtils.readProducerMQTTClientConfig())
      p <- Task(producer(config))
    } yield p

  def consumerTask()(implicit scheduler: Scheduler): Task[(Observable[MqttMessage], StartPushing, Close)] =
    for {
      config <- Task(ConfigUtils.readConsumerMQTTClientConfig())
      c <- Task(consumer(config))
    } yield c

  def producer(config: MQTTClientConfig): (MqttClient, MqttTopic) = {
    val client = getClient(config)
    val topic = client.getTopic(config.topic)

    closeClientOnShutdown(client)
    (client, topic)
  }

  def consumer(config: MQTTClientConfig)(implicit s: Scheduler): (Observable[MqttMessage], StartPushing, Close) = {
    val (observer, observable) = Pipe.publish[MqttMessage].multicast
    val client: MqttClient = getClient(config)
    client.setCallback(new MqttCallback {
      override def connectionLost(cause: Throwable): Unit = {
        logger.error("MQTT Consumer connection lost", cause)
        observer.onError(cause)
      }

      override def messageArrived(topic: String, message: MqttMessage): Unit = {
        logger.debug("Message arrived")
        observer.onNext(message)
      }

      override def deliveryComplete(token: IMqttDeliveryToken): Unit = {
        logger.debug("Delivery complete")
      }
    })
    val startPushing = () => {
      logger.info("Starting to push MQTT Consumer records")
      client.subscribe(config.topic)
    }
    val close = () => client.close()
    closeClientOnShutdown(client)
    (observable, startPushing, close)
  }

  private def getClient(config: MQTTClientConfig) = {
    val persistence = new MqttDefaultFilePersistence("/tmp")
    val client = new MqttClient(config.serverURI, MqttClient.generateClientId, persistence)
    client.connect()
    client
  }

  private def closeClientOnShutdown(client: MqttClient) = {
    sys.addShutdownHook(Try {
      logger.warn("Post shutdown hook: Disconnecting MQTT Producer")
      if (client.isConnected) client.disconnect()
      client.close()
      logger.warn("Post shutdown hook: Disconnected MQTT Producer")
    })
  }
}
case class MQTTClientConfig(serverURI: String, topic: String)
