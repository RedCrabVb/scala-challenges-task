package ru.vivt

import ru.vivt.Data.forks

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SpawnProtocol}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


object Data {
  val forks: Array[Boolean] = Array.fill(5)(true)
}


object Main {
  var typeScenario = 0

  def main(args: Array[String]): Unit = {
    if (args.length > 0 && args(0).toInt == 1) {
      typeScenario = 1
    }

    val philosopherMain: ActorSystem[NotUsed] = ActorSystem(guard(), "guard")

    philosopherMain ! NotUsed
    Thread.sleep(10_000)
    philosopherMain ! NotUsed
    Await.result(philosopherMain.whenTerminated, 1.second)
  }


  case class Message(current: Int, from: Array[ActorRef[Message]])

  def philosopher(name: String,
                  forkLeft: Int,
                  forkRight: Int,
                  scenarioForkGet: (Int, Int, () => Unit) => Unit): Behavior[Message] =

    def eat(): Unit = {
      println("Start eating: " + name)
      Thread.sleep(100)
      println("End eating: " + name)
      println("Fork put: " + name)
      forkPut()
      println("To think: " + name)
    }

    def forkPut(): Unit = {
      forks(forkLeft) = true
      forks(forkRight) = true
    }

    Behaviors.receive {
      case (context, msg@Message(current, from)) =>
        scenarioForkGet(forkRight, forkLeft, () => new Thread(() => eat()).start())

        val currentNext = if (current == from.length - 1) 0 else current + 1
        from(currentNext) ! Message(currentNext, from)
        Behaviors.same
    }

  def guard(): Behavior[NotUsed] =
    Behaviors.setup { context =>


      def scenarioForkGetNotBlock(forkLeft: Int, forkRight: Int, method: () => Unit) = {
        if (forks(forkRight) && forks(forkLeft)) {
          forks(forkLeft) = false
          forks(forkRight) = false
          method()
        }
      }

      def scenarioForkGetBlock(forkLeft: Int, forkRight: Int, method: () => Unit) = {

        if (forks(forkRight)) {
          forks(forkRight) = false
          if (forks(forkLeft)) {
            forks(forkLeft) = false
            method()
          }
        }
      }

      val scenarioForkGet = if (typeScenario == 0) scenarioForkGetNotBlock(_, _, _) else scenarioForkGetBlock(_, _, _)

      def contextSpawn(name: String, forkLeft: Int, forkRight: Int): ActorRef[Message] = {
        context.spawn(philosopher(name, forkLeft, forkRight, scenarioForkGet), name.replace(" ", ""))
      }


      val philosophers: Array[ActorRef[Message]] = Array(
        contextSpawn("Name 1", 0, 1),
        contextSpawn("Name 2", 1, 2),
        contextSpawn("Name 3", 2, 3),
        contextSpawn("Name 4", 3, 4),
        contextSpawn("Name 5", 4, 0)
      )


      Behaviors.receiveMessage { _ =>
        philosophers(0) ! Message(0, philosophers)

        println("started philosopher")

        Behaviors.receive { (_, _) =>
          Behaviors.stopped
        }
      }
    }


}
