package net.liftmodules.bus

import net.liftweb.actor._

case class AddListener(listener: LiftActor)
case class RemoveListener(listener: LiftActor)

/**
  * `PublisherActor` is the component that is responsible for sending messages to subscribed listeners.
  * It is an internal actor used by `[[MessageBus]]` and created automatically when a new `[[Topic]]` instance
  * is registered in `[[MessageBus]]`.
  */
class PublisherActor extends LiftActor {
  protected var actors = List[LiftActor]()

  def messageHandler = {
    case AddListener(listener) =>
      actors ::= listener

    case RemoveListener(listener) =>
      actors = actors.filter(_ != listener)

    case message =>
      distributeMessage(message)
  }

  protected def distributeMessage(message: Any) = {
    actors.foreach(_ ! message)
  }
}
