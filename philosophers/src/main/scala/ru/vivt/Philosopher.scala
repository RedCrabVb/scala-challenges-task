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

import ru.vivt.Data

sealed trait Message
final case class ForkFree(i: Int, main: ActorRef[Message]) extends Message
final case class ForkPut(i: Int, state: ActorRef[Message]) extends Message
final case class ForkGet(i: Int, philosopher: ActorRef[Message]) extends Message
final case class ForkGetTwo(i: Int, j: Int, philosopher: ActorRef[Message]) extends Message
final case class ForkSet(i: Int, main: ActorRef[Message]) extends Message

case object Start extends Message

object Philosopher {
  def apply(name: String, main: ActorRef[Message], forkLeft: Int, forkRight: Int): Behavior[Message] = {
    Behaviors.setup(context => new Philosopher(name, main, forkLeft, forkRight, context))
  }
}

class Philosopher(name: String, main: ActorRef[Message], forkLeft: Int, forkRight: Int,
                  context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  var forkLeftStatus = false
  var forkRightStatus = false

  def think(): Unit = {
    println("To think: " + name)
  }

  def eat(): Unit = {
    println("Start lunch: " + name)
  }

  def forkPut(state: ActorRef[Message]): Unit = {
    println("Fork put: " + name)
    state ! ForkPut(forkLeft, state)
    state ! ForkPut(forkRight, state)
    forkLeftStatus = false
    forkRightStatus = false
  }

  def forkGet(): Unit = {
    main ! ForkGet(forkLeft, context.self)
    main ! ForkGet(forkRight, context.self)
  }

  override def onMessage(msg: Message): Behavior[Message] =
    msg match {
      case ForkFree(i, state) if i == forkRight || i == forkLeft =>
        main ! (Data.typeScenarioGet() match {
          case 0 => ForkGet(i, context.self)
          case 1 => ForkGetTwo(forkLeft, forkRight, context.self)
        })
        this
      case ForkFree(_, _) => this
      case ForkSet(i, state) if i == forkRight || i == forkLeft =>
        println(s"Fork $i get $name")
        if(i == forkRight) forkRightStatus = true
        else if (i == forkLeft) forkLeftStatus = true

        if (forkRightStatus && forkLeftStatus) {
          eat()
          forkPut(state)
          think()
        }
        this
      case _ => println("Error"); this
    }
}

