package lightning

import akka.actor.{ActorSystem, Props}
import akka.stream.{ActorMaterializerSettings, Supervision, ActorMaterializer}
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ActorSubscriber, MaxInFlightRequestStrategy}
import akka.stream.scaladsl.{Sink, Source}
import com.softwaremill.react.kafka.KafkaMessages.{KafkaMessage, StringKafkaMessage}
import com.softwaremill.react.kafka.{ConsumerProperties, ProducerProperties, ReactiveKafka}
import kafka.serializer.{Decoder, Encoder, StringDecoder, StringEncoder}
import org.reactivestreams.{Publisher, Subscriber}
import play.api.libs.json.Json

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 10/12/15
 * Time: 4:38 PM
 *
 */
object Lightning extends App {
  implicit val actorSystem = ActorSystem("Lightning")

  val decider : Supervision.Decider =  {
    case ex =>
      println(s"Exception: $ex")
      Supervision.Resume
  }

  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider))

  val kafka = new ReactiveKafka()

  val publisher : Publisher[StringKafkaMessage] = kafka.consume(ConsumerProperties(
    brokerList = "localhost:9092",
    zooKeeperHost = "localhost:2181",
    topic = "lowercaseString",
    groupId = "groupName",
    decoder = new StringDecoder()
  ))

  val anotherPublisher : Publisher[StringKafkaMessage] = kafka.consume(ConsumerProperties(
    brokerList = "localhost:9092",
    zooKeeperHost = "localhost:2181",
    topic = "lowercaseString",
    groupId = "groupName2",
    decoder = new StringDecoder()
  ))

  val sampleMessagePublisher : Publisher[KafkaMessage[Option[SampleMessage]]] = kafka.consume(ConsumerProperties(
    brokerList = "localhost:9092",
    zooKeeperHost = "localhost:2181",
    topic = "sampleMessages",
    groupId = "groupName3",
    decoder = new SampleMessageDecoder()
  ))

  val subscriber : Subscriber[String] = kafka.publish(ProducerProperties(
    brokerList = "localhost:9092",
    topic = "uppercaseStrings",
    encoder = new StringEncoder()
  ))

  println("Storm is coming...")
  Source(publisher).map(_.message().toUpperCase).to(Sink(subscriber)).run()
  Source(anotherPublisher).map(_.message().toUpperCase).runWith(Sink.actorSubscriber(ActorLogger.props))
  Source(sampleMessagePublisher).filter(_.message().isDefined).filter(_.message().get.name == "foo").map(_.message()).runWith(Sink.actorSubscriber(ActorLogger.props))
  println("...and its here!")
}

case class SampleMessage(name : String, code : Int)

object SampleMessage {
  implicit val sampleMessageFormat = Json.format[SampleMessage]
}

class SampleMessageEncoder() extends Encoder[SampleMessage] {
  def toBytes(t: SampleMessage) = Json.toJson(t).toString().getBytes
}

class SampleMessageDecoder() extends Decoder[Option[SampleMessage]] {
  def fromBytes(bytes: Array[Byte]) = Json.parse(bytes).validate[SampleMessage].asOpt
}

object ActorLogger {
  def props = Props(new ActorLogger)
}

class ActorLogger extends ActorSubscriber {

  // required by implementors of ActorSubscriber
  override val requestStrategy = new MaxInFlightRequestStrategy(max = 10) {
    def inFlightInternally = 1
  }

  def receive = {
    case OnNext(msg) =>
      println(s"ActorLogger => $msg")


  }
}
