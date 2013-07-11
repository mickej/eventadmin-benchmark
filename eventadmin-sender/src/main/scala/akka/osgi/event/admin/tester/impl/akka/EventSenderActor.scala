package akka.osgi.event.admin.tester.impl.akka

import akka.actor.{Cancellable, Actor}
import org.osgi.service.event.{Event, EventAdmin}
import java.util.Collections
import akka.osgi.event.admin.tester.impl.MonitorableImpl
import scala.concurrent.duration.Duration

class EventSenderActor(admin : EventAdmin, topic : String, toWait : Int, monitorable : MonitorableImpl) extends Actor {
  import context._
  case object Trigger

  override def preStart() = context.system.scheduler.scheduleOnce(Duration(toWait, "ms"), self, Trigger)

  def receive = {
    case Trigger => {
      admin.sendEvent(new Event(topic, Collections.singletonMap[String, Long]("time.sent", System.currentTimeMillis)))
      monitorable.send
      context.system.scheduler.scheduleOnce(Duration(toWait, "ms"), self, Trigger)
    }
  }
}

class EventPostActor(admin : EventAdmin, topic : String, toWait : Int, monitorable : MonitorableImpl) extends Actor {
  import context._
  case object Trigger

  override def preStart() = context.system.scheduler.scheduleOnce(Duration(toWait, "ms"), self, Trigger)

  def receive = {
    case Trigger => {
      admin.postEvent(new Event(topic, Collections.singletonMap[String, Long]("time.sent", System.currentTimeMillis)))
      monitorable.send
      context.system.scheduler.scheduleOnce(Duration(toWait, "ms"), self, Trigger)
    }
  }
}