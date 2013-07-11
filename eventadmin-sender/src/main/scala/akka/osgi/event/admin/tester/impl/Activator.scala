package akka.osgi.event.admin.tester.impl

import _root_.akka.actor.{Props, ActorSystem, ActorRef}
import _root_.akka.osgi.ActorSystemActivator
import _root_.akka.osgi.event.admin.tester.impl.akka.{DoYourThing, Mother}
import org.osgi.service.monitor.Monitorable
import org.osgi.framework._
import org.osgi.util.tracker.{ServiceTracker, ServiceTrackerCustomizer}
import org.osgi.service.event.{EventConstants, EventHandler, EventAdmin}
import java.util.Hashtable
import scala.Some

class Activator extends ActorSystemActivator {
  val topics = {
    val levels = System.getProperty("topic.levels", "3").toInt
    def go(num : Int) : String = {
      if (num == levels) Utils.readable(num)
      else Utils.readable(num) + "/" + go(num + 1)
    }

    Range(1, levels).inclusive.map(f => go(f)).toList
  }

  val sendMilliseconds = System.getProperty("milliseconds.between.each.send", "1000").toInt
  val postMilliseconds = System.getProperty("milliseconds.between.each.post", "1000").toInt
  val actorsPerTopic = System.getProperty("actors.per.topic", "1").toInt

  var eventAdminTracker : ServiceTracker[EventAdmin, EventAdmin] = null

  def configure(context: BundleContext, system: ActorSystem) {
    eventAdminTracker = new ServiceTracker[EventAdmin, EventAdmin](context, classOf[EventAdmin], new ServiceTrackerCustomizer[EventAdmin, EventAdmin] {
      var mother : Option[ActorRef] = None
      var monitorableReg : Option[ServiceRegistration[Monitorable]] = None

      def addingService(reference: ServiceReference[EventAdmin]): EventAdmin = {
        val eventAdminService = context.getService(reference)

        val monitorable = new MonitorableImpl
        val props = new Hashtable[String, String]()
        props.put(Constants.SERVICE_PID, "event_admin_send_monitorable")
        monitorableReg = Option(context.registerService(classOf[Monitorable], monitorable, props))

        mother = Option(system.actorOf(Props(new Mother(eventAdminService, monitorable)), "eventActorsMother"))
        mother.get ! DoYourThing(topics, sendMilliseconds, postMilliseconds, actorsPerTopic)

        eventAdminService
      }

      def modifiedService(reference: ServiceReference[EventAdmin], service: EventAdmin) {}

      def removedService(reference: ServiceReference[EventAdmin], service: EventAdmin) {
        mother match {
          case Some(m) => system.stop(m)
          case None => // No need to do anything
        }

        monitorableReg match {
          case Some(m) => m.unregister
          case None => // No need to do anything
        }
      }
    })
    eventAdminTracker.open
  }
}
