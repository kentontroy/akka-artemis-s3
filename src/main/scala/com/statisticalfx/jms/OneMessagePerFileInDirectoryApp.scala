package com.statisticalfx.jms

/**
 * @author Kenton Troy Davis
 * @module Main class to initialize app
 */
import akka.NotUsed
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import com.typesafe.config.ConfigFactory
import java.nio.file.{FileSystems, Path}
import scala.collection.immutable.Seq
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success, Try}

object OneMessagePerFileInDirectoryApp
{
  def main(args : Array[String]): Unit = {
    val config = ConfigFactory.load()
    val brokerUrl = config.getString("statisticalfx.jms.broker-url")
    val queueName = config.getString("statisticalfx.jms.queue-name")
    val dirSource = config.getString("statisticalfx.jms.directory-source")

    val connFactory: javax.jms.ConnectionFactory = new org.apache.activemq.ActiveMQConnectionFactory(brokerUrl)
    val jmsProducer: JmsProducerBase = new JmsProducerBase(connFactory, queueName)

    implicit val materializer: ActorMaterializer = ActorMaterializer()(jmsProducer.actor.classicSystem)

    jmsProducer.actor.log.info(brokerUrl)
    jmsProducer.actor.log.info(queueName)
    jmsProducer.actor.log.info(dirSource)

    /*
    TODO: Does not yet support nested subdirectories
    TODO: Does not tail any new files in the directory or keep track of content already processed
    Best use case for now is to replay or test producer response
     */
    val files: Source[Path, NotUsed] = Directory.ls(FileSystems.getDefault().getPath(dirSource))
    val fileContent: Source[String, NotUsed] =
       files.flatMapConcat(path => {
         FileIO.fromPath(path).reduce((a, b) => a ++ b)
     })
     .map(_.utf8String)

    val s: Future[Seq[String]] = fileContent.runWith(Sink.seq)
    /*
    The code below materializes the Seq[String] from the Future using the implicits above
    Avoid using the enqueueSeq() method due to the buffering in memory and data copy
    passed to the JmsProducer. Use one-by-one approach below instead.
    */
    val contents: Try[Seq[String]] = Await.ready(s, Duration.Inf).value.get
    val _ = contents match {
      case Success(t) => {
        // jmsProducer.enqueueSeq(contents.get)
        contents.get.foreach(d => jmsProducer.enqueue(d))
      }
      case Failure(e) => jmsProducer.actor.log.info("Failed to get file contents from directory")
    }

    jmsProducer.wait(30.second)
    jmsProducer.die()
  }

}
