package ru.vivt

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SpawnProtocol}
import akka.actor.{Actor, Props}
import akka.{NotUsed, actor}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.sys.exit
import scala.util.Random


sealed trait Message
final case class ForkFree(i: Int, state: ActorRef[Message]) extends Message
final case class ForkBusy(i: Int, state: ActorRef[Message]) extends Message
//final case class OtherPhilosopher(others: Array[ActorRef[Message]]) extends Message
final case class Start() extends Message

object Philosopher {
  def apply(name: String, forks: Array[Boolean], forkLeft: Int, forkRight: Int): Behavior[Message] = {
    Behaviors.setup(context => new Philosopher(name, forks, forkLeft, forkRight, context))
  }
}

object State {
  def apply(forks: Array[Boolean],
            others: Array[ActorRef[Message]]): Behavior[Message] = {
    Behaviors.setup(context => new State(forks, others, context))
  }
}

class State(forks: Array[Boolean],
            others: Array[ActorRef[Message]],
            context: ActorContext[Message]) extends AbstractBehavior[Message](context) {


  override def onMessage(msg: Message): Behavior[Message] = {
    msg match {
      case ForkBusy(i, _) =>
        forks(i) = false
        this
      case ForkFree(i, _) =>
        forks(i) = true
        others.foreach(other =>
          if(forks(i)) other ! ForkFree(i, context.self)
        )
        this
    }
  }
}

class Philosopher(name: String, forks: Array[Boolean], forkLeft: Int, forkRight: Int,
                  context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  var othersPhilosopher = Array[ActorRef[Message]]()
  var forkLeftStatus = false
  var forkRightStatus = false

  def think(): Unit = {
    println("To think: " + name)
  }

  def eat(): Unit = {
    println("Start eating: " + name)
    Thread.sleep(3 - Random.nextInt % 3)
    println("End eating: " + name)
  }

  def forkPut(state: ActorRef[Message]): Unit = {
    println("Fork put: " + name)
    state ! ForkFree(forkLeft, state)
    state ! ForkFree(forkRight, state)
  }

  def forkGet(): Unit = {
    forks(forkRight) = false
    forks(forkLeft) = false
  }

  override def onMessage(msg: Message): Behavior[Message] =
    msg match {
      case ForkFree(i, state) if i == forkRight || i == forkLeft =>
        if (forks(i)) {
//          forks(i) = false
          println(s"Fork $i get $name")
          if(i == forkRight) forkRightStatus = true
          else if (i == forkLeft) forkLeftStatus = true
        }

        if (forkRightStatus && forkLeftStatus) {
          forkGet()
          eat()
          forkPut(state)
          think()
        }
        this
      case ForkFree(_, _) => this
      case _ => println("Error"); this
    }
}

