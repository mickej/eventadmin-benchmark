package akka.osgi.event.admin.tester.impl

import org.osgi.service.monitor.{StatusVariable, Monitorable}

class MonitorableImpl extends Monitorable {
  val names = List("events_sent")
  private var sent = 0L

  def send = sent = sent + 1

  def getStatusVariableNames: Array[String] = names.toArray

  def getStatusVariable(id: String): StatusVariable = names find(s => s == id) match {
    case Some("events_sent") => new StatusVariable(id, StatusVariable.CM_GAUGE, sent)
    case None => throw new IllegalArgumentException(id + " does not exist")
  }

  def notifiesOnChange(id: String): Boolean = false

  def resetStatusVariable(id: String): Boolean = names find(s => s == id) match {
    case Some("events_sent") => {
      sent = 0
      true
    }
    case None => throw new IllegalArgumentException(id + " does not exist")
  }

  def getDescription(id: String): String = names find(s => s == id) match {
    case Some("events_sent") => "The total events sent"
    case None => throw new IllegalArgumentException(id + " does not exist")
  }
}
