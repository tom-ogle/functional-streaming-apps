# Functional Streaming Apps Spike

## Introduction

Working example apps built using Functional programming libraries, such as Monix and Cats Effect.
The apps were developed as exploratory spikes without tests and are not production ready.
They read some messages from a message broker, do some toy transformations and write the messages to a message broker.
The logging is extra verbose so that you can see what is happening.

There is a `docker-compose.yml` file include din the repo to allow you to run the app main methods against real message brokers.

## Applications
 
### MonixPulsarToKafkaApp and MonixPulsarToKafkaApp2

`MonixPulsarToKafkaApp` is a Monix app that consumes using Pulsar and publishes to Kafka. 
It uses Monix `Task` and `Observable`.
`MonixPulsarToKafkaApp2` is similar to `MonixPulsarToKafkaApp` but uses Observable for resources rather than Task. 

To see one of these apps in action:

* Run `docker-compose up` to spin up the message brokers
* Run `MonixPulsarToKafkaApp#main` or `MonixPulsarToKafkaApp2` to run the app
* Run `PulsarFeed#main` to publish some test messages
* Watch the logs as the messages are processed
 
### MonixMqttToMqttApp
 
A Monix app that consumes using MQTT and publishes using MQTT.
It uses Monix Pipe to allow use of Observable with a callback push based API.

To see it in action:

* Run `docker-compose up` to spin up the message brokers
* Run `MonixMqttToMqttApp#main` to run the app
* Run `MQTTFeed#main` to publish some test messages
* Watch the logs as the messages are processed

### CatsEffectApp

An app that consumes using Pulsar and publishes to Kafka, using Cats Effect. 

* Run `docker-compose up` to spin up the message brokers
* Run `CatsEffectApp#main` to run the app
* Run `PulsarFeed#main` to publish some test messages
* Watch the logs as the messages are processed