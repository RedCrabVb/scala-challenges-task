package ru.vivt

import akka.actor
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import ru.vivt.Data.{countPhilosopher, typeScenario}

class Main(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val forks = Array.fill(countPhilosopher)(true)
  var philosophers: Array[ActorRef[Message]] = null

  override def onMessage(msg: Message): Behavior[Message] =
    msg match {
      case Start() =>


        def contextSpawn(name: String, forkLeft: Int, forkRight: Int) = {
          context.spawn(Philosopher(name, context.self, forkLeft, forkRight), name)
        }

        philosophers = (0 until countPhilosopher).map(
          i => contextSpawn(s"Name$i", i, (i + 1) % countPhilosopher)
        ).toArray

        forks.zipWithIndex.foreach((_, i) =>
          philosophers.foreach(philosopher => philosopher ! ForkFree(i, context.self))
        )

        this
      case ForkGet(i, philosopher) =>
        if (forks(i)) {
          forks(i) = false
          philosopher ! ForkSet(i, context.self)
        }
        this
      case ForkPut(i, _) =>
        if (!forks(i)) {
          forks(i) = true
          philosophers.foreach(other =>
            if(forks(i)) other ! ForkFree(i, context.self)
          )
        }
        this
      case ForkFree(i, _) => {
        philosophers.foreach(other =>
          if(forks(i)) other ! ForkFree(i, context.self)
        )
        this
      }
    }
}


object Main {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new Main(context))
}

object ProblemAboutPhilosophers {
  def main(args: Array[String]): Unit = {
    if (args.length > 0 && args(0).toInt == 1) {
      typeScenario = 1
    }

    val actorSystem = ActorSystem(Main(), "system")
    actorSystem ! Start()
  }
}

