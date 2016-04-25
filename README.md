# MessageBus for Lift framework

`MessageBus` facilitates communication between `LiftActors`. 

Originally, the idea was to allow any two or more Comets communicate with each other even if they all live in separate user sessions. Lift does not allow to obtain reference to Comet actors from other user sessions. This is reasonable limitation. However, sometimes, we need to send an instant notification to all Comets rendering the given page so that they can execute `partialUpdate` as soon as some event occurrs. Polling database each second for changes from all Comets displaying the given page is less than optimal in most cases. Here the `MessageBus` comes into play.

Comets (actually any descendants of `LifActor`) subscribe to `Topic`s which are abstractions allowing to specify in which type of messages the given listener is interested. In our implementation, `Topic` is a trait with one method `def name: String`. Here are two example `Topic` implementations:

```
case class WombatPhotosTopic(wombatId: String) extends Topic {
  val name = "wombat-topic-" + wombatId
}

case class BeaverPhotosTopic(beaverId: String) extends Topic {
  val name = "beaver-topic-" + beaverId
}
```

Actors can subscribe to `MessageBus` with `Subscribe` message (for Comets they will be usually sent in `localStartup` method):

```
MessageBus ! Subscribe(listener1, WombatPhotosTopic("1"))
MessageBus ! Subscribe(listener2, WombatPhotosTopic("2"))
MessageBus ! Subscribe(listener3, BeaverPhotosTopic("3"))
```

There are two ways to send a message to other actors: `For` and `ForAll`. The `payload` of `For` message will be delivered by `MessageBus` to all `LiftActor`s that subscribed to the given `Topic` (we look for their equality). The `payload` of `ForAll` message will be delivered by `MessageBus` to all `LiftActor`s that subscribed to the given `Topic` type.

If we take the topics and listeners that we've set up above and do:
```
MessageBus ! For(WombatPhotosTopic("1"), WombatPhoto('w1.jpg')) // 1
MessageBus ! ForAll[WombatPhotosTopic](WombatPhoto('w2.jpg')) // 2
```
The first message will be delivered only to `listener1` while second message will be delivered both to `listener1` and `listener2`. 

Actors can unsubscribe from the given `Topic` sending `Unsubscribe` message:
```
MessageBus ! Unsubscribe(this, BeaverPhotosTopic("3"))
```

Actors should listen for messages from `MessageBus` in the same way as they listen for any other type of message. That is, for example, `messageHandler` method for `LiftActor`s and `high/low/mediumPriority` methods for `CometActor`s: 

```
case object BeaverPhotosTopic extends Topic {
  val name = "beaver-photos"
}

class BeaverPhotosWall extends NamedCometActorTrait {
  override def localSetup = {
    super.localSetup
    MessageBus ! Subscribe(this, BeaverPhotosTopic)
  }

  override def localShutdown = {
    super.localShutdown
    MessageBus ! Unsubscribe(this, BeaverPhotosTopic)
  }
  
  override def lowPriority = {
    case NewBeaverPhotoPosted(photo) =>
      partialUpdate(renderNewPhoto(photo))
  }
}
```

