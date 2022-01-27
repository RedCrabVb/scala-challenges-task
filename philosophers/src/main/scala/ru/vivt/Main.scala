package ru.vivt

import akka.actor
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import ru.vivt.Data.{countPhilosopher, typeScenario}

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt

class Main(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  def contextSpawn(name: String, forkLeft: Int, forkRight: Int): ActorRef[Message] = {
    context.spawn(Philosopher(name, context.self, forkLeft, forkRight), name)
  }

  val forks: Array[Boolean] = Array.fill(countPhilosopher)(true)
  var philosophers: Array[ActorRef[Message]] = (0 until countPhilosopher).map(
    i => contextSpawn(s"Name$i", i, (i + 1) % countPhilosopher)
  ).toArray

  override def onMessage(msg: Message): Behavior[Message] =
    msg match {
      case Start =>
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
      case ForkGetTwo(i, j, philosopher) =>
        if (forks(i) && forks(j)) {
          forks(i) = false
          forks(j) = false
          philosopher ! ForkSet(i, context.self)
          philosopher ! ForkSet(j, context.self)
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
      case ForkFree(i, _) =>
        philosophers.foreach(other =>
          if(forks(i)) other ! ForkFree(i, context.self)
        )
        this
    }
}


object Main {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new Main(context))
}

object ProblemAboutPhilosophers {
  def main(args: Array[String]): Unit = {

    Data(args(0) match {
      case "block" => 0
      case "notblock" => 1
      case _ => ???
    })

    val main = ActorSystem(Main(), "main")
    main ! Start

    Thread.sleep(5000)
    main.terminate()
    println("Stop")
  }
}

