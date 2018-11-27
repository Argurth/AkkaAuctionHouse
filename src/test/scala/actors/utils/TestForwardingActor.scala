package actors.utils

import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout

/**
  * Created by argurth on 27/11/18.
  */
object TestForwardingActor {
  def props(receiver: ActorRef)(implicit timeout: Timeout) =
    Props(new TestForwardingActor(receiver))
}

class TestForwardingActor(val receiver: ActorRef) extends Actor {
  def receive: PartialFunction[Any, Unit] = {
    case message => receiver forward message
  }
}
