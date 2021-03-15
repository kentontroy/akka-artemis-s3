package com.statisticalfx.jms

/**
 * @author Kenton Troy Davis
 * @module Base class for simple producers
 */
import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.jms.JmsProducerSettings
import akka.stream.alpakka.jms.scaladsl.JmsProducer
import akka.stream.scaladsl.{Sink, Source}
import javax.jms.ConnectionFactory
import scala.collection.immutable.Seq
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class JmsProducerBase
{
  implicit val actor: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "JmsProducerBase")
  implicit val ctx: ExecutionContext = actor.executionContext
  var jmsSink: Sink[String, Future[Done]] = null

  def this(connFactory: ConnectionFactory, queueName: String) = {
    this()
    jmsSink = JmsProducer.textSink(JmsProducerSettings(actor, connFactory).withQueue(queueName))
  }

  def enqueue(msgs: String*): Unit = {
    val finished: Future[Done] = Source(msgs.toList).runWith(jmsSink)
  }

  def enqueueSeq(content: Seq[String]) = {
    val finished: Future[Done] = Source(content).runWith(jmsSink)
  }

  def die(): Unit = {
    actor.terminate()
    Await.result(actor.whenTerminated, 1.seconds)
  }

  def wait(dur: FiniteDuration): Unit = Thread.sleep(dur.toMillis)
}

