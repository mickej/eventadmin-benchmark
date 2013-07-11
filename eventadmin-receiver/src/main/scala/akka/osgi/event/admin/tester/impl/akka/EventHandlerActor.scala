package akka.osgi.event.admin.tester.impl.akka

import akka.actor.Actor
import org.osgi.service.monitor.{StatusVariable, Monitorable}
import org.osgi.service.event.Event

case class Received(sent :Long, rec :Long, event :Event)

class EventHandlerActor(monitorable : MonitorableImpl) extends Actor {
  def receive = {
    case Received(s, r, e) => monitorable.received(r - s)
  }
}

class MonitorableImpl extends Monitorable {
  val names = List("events_received", "average_time", "shortest_time", "longest_time")
  private var rec = 0L
  private var totalTime = 0L
  private var shortest = 0L
  private var longest = 0L

  def received(time : Long) {
    rec = rec + 1
    totalTime = totalTime + time
    shortest = if (time < shortest) time else shortest
    longest = if (time > longest) time else longest
  }

  def getStatusVariableNames: Array[String] = names.toArray

  def getStatusVariable(id: String): StatusVariable = names find(s => s == id) match {
    case Some("events_received") => new StatusVariable(id, StatusVariable.CM_GAUGE, rec)
    case Some("average_time") => new StatusVariable(id, StatusVariable.CM_GAUGE, if (rec == 0) 0 else totalTime / rec)
    case Some("shortest_time") => new StatusVariable(id, StatusVariable.CM_GAUGE, shortest)
    case Some("longest_time") => new StatusVariable(id, StatusVariable.CM_GAUGE, longest)
    case None => throw new IllegalArgumentException(id + " does not exist")
  }

  def notifiesOnChange(id: String): Boolean = false

  def resetStatusVariable(id: String): Boolean = names find(s => s == id) match {
    case Some("events_received") => {
      rec = 0
      true
    }
    case Some("average_time") => {
      totalTime = 0
      true
    }
    case Some("shortest_time") => {
      shortest = 0
      true
    }
    case Some("longest_time") => {
      longest = 0
      true
    }
    case None => throw new IllegalArgumentException(id + " does not exist")
  }

  def getDescription(id: String): String = names find(s => s == id) match {
    case Some("events_received") => "The total events received"
    case Some("average_time") => "The average time between an event being sent and arrived at a event handler."
    case Some("shortest_time") => "The shortest time."
    case Some("longest_time") => "The longest time."
    case None => throw new IllegalArgumentException(id + " does not exist")
  }
}