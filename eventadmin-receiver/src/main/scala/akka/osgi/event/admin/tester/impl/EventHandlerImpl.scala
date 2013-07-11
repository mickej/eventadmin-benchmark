package akka.osgi.event.admin.tester.impl

import _root_.akka.actor.ActorRef
import _root_.akka.osgi.event.admin.tester.impl.akka.Received
import org.osgi.service.event.{Event, EventHandler}

class EventHandlerImpl(ref : ActorRef) extends EventHandler {
  def handleEvent(event: Event) {
    ref ! Received(event.getProperty("time.sent").asInstanceOf[Long], System.currentTimeMillis, event)
  }
}
