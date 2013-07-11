package akka.osgi.event.admin.tester.impl.akka

import akka.actor.{ActorRef, Props, Actor}
import org.osgi.service.event.EventAdmin
import akka.osgi.event.admin.tester.impl.MonitorableImpl

case class Poster(topic : String, toWait : Int, actorsPerTopic : Int)
case class Sender(topic : String, toWait : Int, actorsPerTopic : Int)
case class DoYourThing(topics : List[String], sendMilliseconds : Int, postMilliseconds : Int, actorsPerTopic : Int)

class Mother(admin : EventAdmin, monitorable : MonitorableImpl) extends Actor {
  def receive = {
    case Poster(t, wait, perTopic) => Range(1, perTopic).inclusive.foreach(i => context.actorOf(Props(new EventPostActor(admin, t, wait, monitorable)), "poster" + t.replace("/", "") + i.toString))
    case Sender(t, wait, perTopic) => Range(1, perTopic).inclusive.foreach(i => context.actorOf(Props(new EventSenderActor(admin, t, wait, monitorable)), "sender" + t.replace("/", "") + i.toString))
    case DoYourThing(t, s, p, a) => createActors(t, s, p, a)
  }

  def createActors(topics : List[String], sendMilliseconds : Int, postMilliseconds : Int, actorsPerTopic : Int) = {
    def create(t : String) = {
      if (sendMilliseconds > 0) sendActor(self, t, sendMilliseconds, actorsPerTopic)
      if (postMilliseconds > 0) postActor(self, t, postMilliseconds, actorsPerTopic)
    }

    topics.foreach(create)
  }

  def postActor(ref : ActorRef, topic : String, toWait : Int, actorsPerTopic : Int) = ref ! Poster(topic, toWait, actorsPerTopic)
  def sendActor(ref : ActorRef, topic : String, toWait : Int, actorsPerTopic : Int) = ref ! Sender(topic, toWait, actorsPerTopic)
}
