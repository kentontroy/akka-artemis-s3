package com.statisticalfx.jms

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, SECONDS}

object ProcessJmsQueueApp extends App {
  val config = ConfigFactory.load()
  val ackTimeout = config.getInt("statisticalfx.jms.consumer-ack-timeout")
  val ackDuration = Duration(ackTimeout, SECONDS)
  val pullInterval = config.getInt("statisticalfx.jms.connection-refresh-interval")
  val pullDuration = Duration(pullInterval, SECONDS)

  val guardianActor: ActorSystem[MessageToGuardian] = ActorSystem(
    GuardianActor(),
    "GuardianActor"
  )

  implicit val ec : ExecutionContext = guardianActor.executionContext

  //https://doc.akka.io/docs/akka/current/scheduler.html
  guardianActor.scheduler.scheduleWithFixedDelay(ackDuration, pullDuration)(new Runnable {
    override def run(): Unit = guardianActor ! LaunchJmsConsumer
  })

  var isRunning = true
  val mThread = Thread.currentThread()

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run = {
      isRunning = false
      guardianActor ! StopGuarding
      guardianActor.terminate()
      mThread.join()
    }
  })

}

