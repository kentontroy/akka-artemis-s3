package com.statisticalfx.jms

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.actor.typed.{Behavior, DispatcherSelector, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.jms.{JmsConsumerSettings, TxEnvelope}
import akka.stream.alpakka.jms.scaladsl.{JmsConsumer, JmsConsumerControl}
import akka.stream.alpakka.s3.MultipartUploadResult
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import java.text.SimpleDateFormat
import java.util.{Calendar, UUID}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.{Failure, Success, Try}

object S3FileUtility {
  def getUniqueKey(folderName: String = "", baseName: String, extension: String = "txt"): String = {
    val time = Calendar.getInstance.getTime
    val fmt = new SimpleDateFormat(("MM-dd-yyyy-kk-mm-ss"))
    val name = s"${baseName}-asOf-${fmt.format(time)}-uuidOf-${UUID.randomUUID().toString}.${extension}"
    val key = if(folderName.length == 0) name else s"${folderName}/${name}"
    key
  }
}

sealed trait MessageToJmsConsumer
final case object WriteToS3 extends MessageToJmsConsumer

object JmsConsumerActor {
  val config = ConfigFactory.load()
  val brokerUrl = config.getString("statisticalfx.jms.broker-url")
  val queueName = config.getString("statisticalfx.jms.queue-name")
  val batchSize = config.getInt("statisticalfx.jms.consumer-batch-size")
  val sessionCount = config.getInt("statisticalfx.jms.consumer-session-count")
  val ackTimeout = config.getInt("statisticalfx.jms.consumer-ack-timeout")
  val bucket = config.getString("statisticalfx.jms.s3-sink-demo-bucket")
  val bucketFolder = config.getString("statisticalfx.jms.s3-sink-demo-bucket-folder")
  val bucketBaseKey = config.getString("statisticalfx.jms.s3-sink-demo-bucket-baseKey")

  val ackDuration = Duration(ackTimeout, SECONDS)
  val bucketUniqueKey = S3FileUtility.getUniqueKey(bucketFolder, bucketBaseKey)
  val connFactory: javax.jms.ConnectionFactory = new org.apache.activemq.ActiveMQConnectionFactory(brokerUrl)

  def apply(): Behavior[MessageToJmsConsumer] = {
    Behaviors.receive[MessageToJmsConsumer] { (context, message) =>
      implicit val ec: ExecutionContext =
        context.system.dispatchers.lookup(DispatcherSelector.fromConfig("statisticalfx.jms.blocking-io-dispatcher"))
      implicit val actorSystem = ActorSystem()

      message match {
        case WriteToS3 =>
          logger.info("listening for new messages")

          try {
            /*
            JmsConsumer.ackSource but, doesn't work for some brokers like ActiveMQ. It sometimes commits the session
            rather than the message.
            */
            val jmsSource: Source[TxEnvelope, JmsConsumerControl] = JmsConsumer.txSource(
              JmsConsumerSettings(context.system, connFactory).withQueue(queueName)
                .withSessionCount(sessionCount)
                .withAckTimeout(ackDuration)
            )

            val result: Future[Done] = jmsSource
              .take(batchSize)
              .map { txEnvelope =>
                txEnvelope
              }
              .runWith(Sink.foreach( (txEnvelope: akka.stream.alpakka.jms.TxEnvelope) => {
                val file: Source[ByteString, NotUsed] =
                  Source.single(ByteString(txEnvelope.message.toString()))

                val bucketUniqueKey = S3FileUtility.getUniqueKey(bucketFolder, bucketBaseKey)

                val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
                  S3.multipartUpload(bucket, bucketUniqueKey)

                val result: Future[MultipartUploadResult] =
                  file.runWith(s3Sink)
                /*
                 The amount of time waited on the result should be <= to the configured ack-timeout
                 statisticalfx.jms.consumer-ack-timeout. Failures to process the sink into S3 will create:
                 WARN | The TxEnvelope didn't get committed or rolled back within ack-timeout
                */
                val rUnwrapped: Try[MultipartUploadResult] = Await.ready(result, ackDuration).value.get
                val _ = rUnwrapped match {
                  case Success(t) => {
                    logger.info(t.toString())
                    txEnvelope.commit()
                    logger.info("Message committed")
                  }
                  case Failure(e) => {
                    logger.error(s"${e.getMessage()}")
                    logger.info("Message not committed")
                    txEnvelope.rollback()
                  }
                }
              }))

            Behaviors.stopped { () =>
              logger.info("#1 JmsConsumerActor shutting down")
            }

          }
          catch {
            case t: scala.concurrent.TimeoutException => {
              Behaviors.stopped { () =>
                logger.info("#2 JmsConsumerActor shutting down")
              }
            }
            case e: Throwable => {
              logger.error(s"${e.getMessage()}")
              Behaviors.stopped { () =>
                logger.info("#3 JmsConsumerActor shutting down")
              }
            }
          }
          finally {}
        }
    }.receiveSignal {
      case (context, PostStop) =>
        Behaviors.same
    }
  }
}

