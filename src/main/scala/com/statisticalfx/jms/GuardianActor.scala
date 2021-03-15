package com.statisticalfx.jms

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

sealed trait MessageToGuardian
final case object LaunchJmsConsumer extends MessageToGuardian
final case object StopGuarding extends MessageToGuardian

object GuardianActor {
  def apply(): Behavior[MessageToGuardian] =
    Behaviors.setup { context: ActorContext[MessageToGuardian] =>
      Behaviors.receiveMessage { message: MessageToGuardian =>
        message match {
          case LaunchJmsConsumer =>
            val jmsConsumer: ActorRef[MessageToJmsConsumer] = context.spawn(
              JmsConsumerActor(), name="JmsConsumerActor"
            )
            jmsConsumer ! WriteToS3
            Behaviors.same
          case StopGuarding =>
            context.log.info("Guardian Actor shutting down")
            Behaviors.stopped
        }
      }
    }
}

