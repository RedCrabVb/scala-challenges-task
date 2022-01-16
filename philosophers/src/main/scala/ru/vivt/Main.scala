package ru.vivt

import akka.actor
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import ru.vivt.Data.{countPhilosopher, typeScenario}

class Main(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "start" =>
        val forks = Array.fill(countPhilosopher)(true)


        def contextSpawn(name: String, forkLeft: Int, forkRight: Int) = {
          context.spawn(Philosopher(name, forks, forkLeft, forkRight), name)
        }

        val philosophers: Array[ActorRef[Message]] = (0 until countPhilosopher).map(
          i => contextSpawn(s"Name$i", i, (i + 1) % countPhilosopher)
        ).toArray
        val state: ActorRef[Message] = context.spawn(State(forks, philosophers), "state")
        forks.zipWithIndex.map((_, i) => state ! ForkFree(i, null))



        this
    }
}


object Main {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new Main(context))
}

object ProblemAboutPhilosophers {
  def main(args: Array[String]): Unit = {
    if (args.length > 0 && args(0).toInt == 1) {
      typeScenario = 1
    }

    val actorSystem = ActorSystem(Main(), "system")
    actorSystem ! "start"
  }
}

