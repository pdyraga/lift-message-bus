package net.liftmodules.messagebus

/**
  * Topic instances can be perceived as a street advertising columns. Components interested in some
  * sort of messages will subscribe to appropriate `Topic` instance in `[[MessageBus]]` that controls message
  * flow between message publishers and listeners.
  *
  * For example, we might have some components interested in receiving beaver photo updates and components
  * interested in receiving wombat photo updates. For this situation, we would create two `Topics`:
  * {{{
  *   case object BeaverPhotosTopic extends Topic {
  *     val name = "beaver-photos"
  *   }
  *
  *   case object WombatPhotosTopic extends Topic {
  *     val name = "wombat-photos"
  *   }
  * }}}
  *
  * All messages sent to `BeaverPhotosTopic` would be received only by components subscribed to `BeaverPhotosTopic`.
  * All messages sent to `WombatPhotosTopic` would be received only by components subscribed to `WombatPhotosTopic`.
  */
trait Topic {
  def name: String
}
