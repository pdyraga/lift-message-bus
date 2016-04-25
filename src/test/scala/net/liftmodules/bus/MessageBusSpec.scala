package net.liftmodules.bus

import java.util.UUID

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

import net.liftweb.actor.{LAScheduler, MockLiftActor}

class MessageBusSpec extends Specification with BeforeAfterAll {


  def beforeAll = {
    LAScheduler.onSameThread = true
  }

  def afterAll = {
    LAScheduler.onSameThread = false
  }

  sequential

  "MessageBus" should {

    import Fixture._

    "notify actor subscribed to topic" in {
      val listener1 = new MockLiftActor()
      val listener2 = new MockLiftActor()

      val topic1 = TestTopic.generate
      val topic2 = TestTopic.generate

      MessageBus ! Subscribe(listener1, topic1)
      MessageBus ! Subscribe(listener2, topic2)

      MessageBus ! For(topic1, "pizza")
      MessageBus ! For(topic2, "beer")

      listener1.messages should_== List("pizza")
      listener2.messages should_== List("beer")
    }

    "not notify actors not subscribed to topic" in {
      val listener = new MockLiftActor
      val topic1 = TestTopic.generate
      val topic2 = TestTopic.generate

      MessageBus ! Subscribe(listener, topic1)

      MessageBus ! For(topic2, "beer")

      listener.messages should beEmpty
    }

    "not notify actors that unsubscribed from topic" in {
      val listener1 = new MockLiftActor
      val listener2 = new MockLiftActor
      val topic = TestTopic.generate

      MessageBus ! Subscribe(listener1, topic)
      MessageBus ! Subscribe(listener2, topic)
      MessageBus ! Unsubscribe(listener1, topic)

      MessageBus ! For(topic, "beer")

      listener1.messages should beEmpty
      listener2.messages should_== List("beer")
    }

    "notify actors subscribed to topic of a given type" in {
      val listener1 = new MockLiftActor
      val listener2 = new MockLiftActor
      val listener3 = new MockLiftActor

      val topic1 = TestTopic.generate
      val topic2 = TestTopic.generate
      val topic3 = AnotherTestTopicType.generate

      MessageBus ! Subscribe(listener1, topic1)
      MessageBus ! Subscribe(listener2, topic2)
      MessageBus ! Subscribe(listener3, topic3)

      MessageBus ! ForAll[TestTopic]("pizza")

      listener1.messages should_== List("pizza")
      listener2.messages should_== List("pizza")
      listener3.messages should beEmpty
    }
  }

  object Fixture {
    case class TestTopic(name: String) extends Topic
    object TestTopic {
      def generate = TestTopic(UUID.randomUUID().toString)
    }

    case class AnotherTestTopicType(name: String) extends Topic
    object AnotherTestTopicType {
      def generate = AnotherTestTopicType(UUID.randomUUID().toString)
    }
  }
}
