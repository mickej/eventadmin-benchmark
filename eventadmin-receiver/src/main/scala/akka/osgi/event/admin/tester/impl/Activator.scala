package akka.osgi.event.admin.tester.impl

import _root_.akka.actor.{Props, ActorRef, ActorSystem}
import _root_.akka.osgi.ActorSystemActivator
import _root_.akka.osgi.event.admin.tester.impl.akka._
import org.osgi.framework._
import org.osgi.service.event.{EventAdmin, EventConstants, EventHandler}
import java.util.Hashtable
import org.osgi.util.tracker.{ServiceTrackerCustomizer, ServiceTracker}
import scala.Some
import org.osgi.service.monitor.Monitorable

class Activator extends ActorSystemActivator {
  val topics = {
    val levels = System.getProperty("topic.levels", "3").toInt
    def go(num : Int) : String = {
      if (num == levels) Utils.readable(num)
      else Utils.readable(num) + "/" + go(num + 1)
    }

    Range(1, levels).inclusive.map(f => go(f)).toList
  }

  val handlersPerTopic = System.getProperty("topics.per.handler", "1").toInt

  var eventAdminTracker : ServiceTracker[EventAdmin, EventAdmin] = null

  def configure(context: BundleContext, system: ActorSystem) {
    eventAdminTracker = new ServiceTracker[EventAdmin, EventAdmin](context, classOf[EventAdmin], new ServiceTrackerCustomizer[EventAdmin, EventAdmin] {
      var monitor : Option[ActorRef] = None
      var monitorableReg : Option[ServiceRegistration[Monitorable]] = None
      var registeredHandlers : List[ServiceRegistration[EventHandler]] = List()

      def addingService(reference: ServiceReference[EventAdmin]): EventAdmin = {
        val eventAdminService = context.getService(reference)

        val monitorable = new MonitorableImpl
        val props = new Hashtable[String, String]()
        props.put(Constants.SERVICE_PID, "event_handler_monitorable")
        monitorableReg = Option(context.registerService(classOf[Monitorable], monitorable, props))
        monitor = Option(system.actorOf(Props(new EventHandlerActor(monitorable)), "eventHandlerMonitorable"))

        registeredHandlers = createHandlers(context, monitor get)
        eventAdminService
      }

      def modifiedService(reference: ServiceReference[EventAdmin], service: EventAdmin) {}

      def removedService(reference: ServiceReference[EventAdmin], service: EventAdmin) {
        monitor match {
          case Some(m) => system.stop(m)
          case None => // No need to do anything
        }

        monitorableReg match {
          case Some(m) => m.unregister
          case None => // No need to do anything
        }

        registeredHandlers.foreach(h => h.unregister)
        registeredHandlers = List()
      }
    })
    eventAdminTracker.open
  }

  def createHandlers(context : BundleContext, monitorActor : ActorRef) : List[ServiceRegistration[EventHandler]] = {
    def create(t : String) : List[ServiceRegistration[EventHandler]] = {
      def register(handler : EventHandler) : ServiceRegistration[EventHandler] = {
        val props = new Hashtable[String, String]()
        props.put(EventConstants.EVENT_TOPIC, t)
        context.registerService(classOf[EventHandler], handler, props)
      }

      Range(1, handlersPerTopic).inclusive.map(i => register(new EventHandlerImpl(monitorActor))).toList
    }

    topics.map(create).flatten
  }
}
