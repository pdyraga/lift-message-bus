package net.liftmodules.bus

import net.liftweb.actor._

sealed trait MessageBusMessage

/**
  * `Subscribe` is a message that should be sent to `[[MessageBus]]` when `LiftActor` wants to subscribe
  * to the given `[[Topic]]`. For example:
  * {{{
  *   MessageBus ! Subscribe(this, topic)
  * }}}
  *
  * For `CometActor`s this will usually take place in the `localSetup` method.
  *
  * @param actor reference to `LiftActor` that wants to subscribe to `topic`
  * @param topic reference to `Topic` to which the `actor` should be subscribed
  */
case class Subscribe(actor: LiftActor, topic: Topic) extends MessageBusMessage

/**
  * `Unsubscribe` is a message that should be sent to `[[MessageBus]]` when `LiftActor` wants to unsubscribe
  * from the given `[[Topic]]`. For example:
  * {{{
  *   MessageBus ! Unsubscribe(this, topic)
  * }}}
  *
  * For `CometActor`s this will usually take place in the `localShutdown` method.
  *
  * @param actor reference to `LiftActor` that wants to unsubscribe from `topic`
  * @param topic reference to `Topic` from which the `actor` should be unsubscribed
  */
case class Unsubscribe(actor: LiftActor, topic: Topic) extends MessageBusMessage

/**
  * The `payload` of `For` message will be delivered by `[[MessageBus]]` to all `LiftActor`s that subscribed
  * to the given `[[Topic]]`. For example:
  * {{{
  *   case object WombatPhotosTopic extends Topic {
  *     val name = "wombat-photos"
  *   }
  *
  *   MessageBus ! For(WombatPhotosTopic, WombatPhoto('wombat.jpg'))
  * }}}
  *
  * The `WombatPhoto('wombat.jpg')` message will be delivered to all `LiftActor`s that previously subscribed to
  * `WombatPhotosTopic` sending `[[Subscribe]]` to `[[MessageBus]]`.
  *
  * Actors should listen for messages delivered from `[[MessageBus]]` as an effect of `[[For]]` sent by sender in the
  * same way as for any other message types (e.g. implementing `messageHandler` method or `low`/`medium`/`highPriority`
  * method).
  *
  * @param topic reference to `Topic` to which the message should be sent
  * @param payload message that should be sent to `Topic`
  */
case class For(topic: Topic, payload: Any) extends MessageBusMessage

/**
  * The `payload` of `ForAll` message will be delivered by `[[MessageBus]]` to all `LiftActor`s that subscribed
  * to the given `[[Topic]]` type. For example:
  * {{{
  *   case class WombatPhotosTopic(wombatId: String) extends Topic {
  *     val name = "wombat-topic-" + wombatId
  *   }
  *
  *   case class BeaverPhotosTopic(beaverId: String) extends Topic {
  *     val name = "beaver-topic-" + beaverId
  *   }
  *
  *   MessageBus ! Subscribe(listener1, WombatPhotosTopic("1"))
  *   MessageBus ! Subscribe(listener2, WombatPhotosTopic("2"))
  *   MessageBus ! Subscribe(listener3, BeaverPhotosTopic("1"))
  *
  *   MessageBus ! ForAll[WombatPhotosTopic](WombatPhoto('wombat.jpg'))
  * }}}
  *
  * the `WombatPhoto('wombat.jpg')` message will be sent to `listener1` and `listener2` because they are both subscribed
  * to topic of type `WombatPhotosTopic`. `listener3` will not receive this message because it's not subscribed
  * to topic of type `WombatPhotosTopic'.`
  *
  * @param payload message that should be sent to `Topic`
  * @tparam T the type of `Topic`. Message will be sent to all `Topic`s of this type.
  */
case class ForAll[T <: Topic](payload: Any)(implicit manifest: Manifest[T]) extends MessageBusMessage {
  val topicClass = manifest.runtimeClass
}

/**
  * `MessageBus` allows to send messages between `LiftActor`s even if they live in separate user sessions.
  * All `LiftActor`s that subscribed to the given `[[Topic]]` will receive messages sent to this `Topic`.
  *
  * If you want your actor to subscribe to the given `Topic`, you need to send the `Subscribe` message:
  * {{{
  *   case class BeaverPhotosTopic(beaverId: String) extends Topic {
  *     val name = "beaver-photos-" + beaverId
  *   }
  *
  *   val topic = BeaverPhotosTopic("1")
  *   MessageBus ! Subscribe(this, topic)
  * }}}
  *
  * from this point, all messages sent to this topic will be received by the actor.
  *
  * There are two ways to send a message to `Topic`: `For` and `ForAll`.
  * The `For` class takes a `Topic` instance and sends a payload only to actors subscribed to this particular `Topic`.
  * The `ForAll` class takes a `Topic` type and sends a payload to all actors subscribed to all `Topic`s of the given type.
  *
  * Let's consider two listeners:
  * {{{
  *   MessageBus ! Subscribe(listener1, BeaverTopic("1"))
  *   MessageBus ! Subscribe(listener2, BeaverTopic("2"))
  * }}}
  *
  * If we send a message in the following way:
  * {{{
  *   MessageBus ! For(BeaverTopic("1"), message)
  * }}}
  * only `listener1` will receive it.
  *
  * However, if we send a message in the following way:
  * {{{
  *   MessageBus ! ForAll[BeaverTopic](message)
  * }}}
  * Both `listener1` and `listener2` will receive it.
  *
  * `LiftActor` can unsubscribe from the given `Topic` by sending `Unsubscribe` message:
  * {{{
  *   MessageBus ! Unsubscribe(this, BeaverTopic("1"))
  * }}}
  *
  * In case of `CometActor`s, the `Subscribe`/`Unsubscribe` actions should usually take place in `localSetup`
  * and `localShutdown` methods.
  *
  * There are no limitations in a number of `Topic`s to which a given `LiftActor` can be subscribed.
  *
  * Actors subscribed to `Topic`s should listen for messages in the same way as they listen
  * for any other message types (e.g. implementing `messageHandler` method or `low`/`medium`/`highPriority` method).
  */
object MessageBus extends LiftActor {
  private var topicPublishers =  scala.collection.immutable.Map[Topic, PublisherActor]()

  def messageHandler = {
    case Subscribe(actor, topic) =>
      publisherFor(topic) ! AddListener(actor)

    case Unsubscribe(actor, topic) =>
      if (hasPublisherFor_?(topic)) {
        publisherFor(topic) ! RemoveListener(actor)
      }

    case For(topic, payload) =>
      publisherFor(topic) ! payload

    case forAll @ ForAll(payload) =>
      publishersFor(forAll.topicClass).foreach { publisher =>
        publisher ! payload
      }
  }

  private def hasPublisherFor_?(topic: Topic): Boolean = {
    topicPublishers.get(topic).isDefined
  }

  private def publisherFor(topic: Topic): PublisherActor = {
    topicPublishers.get(topic) match {
      case Some(publisher) =>
        publisher

      case None =>
        val publisher = new PublisherActor()
        topicPublishers += (topic -> publisher)
        publisher
    }
  }

  private def publishersFor(topicClass: Class[_]): Seq[PublisherActor] = {
    topicPublishers.filterKeys(_.getClass == topicClass).values.toSeq
  }
}
