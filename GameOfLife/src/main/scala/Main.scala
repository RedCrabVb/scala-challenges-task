import akka.actor.*

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.*

//https://stackoverflow.com/questions/20580513/no-classtag-available-for-myclass-this-t-for-an-abstract-type
object Main extends App {
  val system = ActorSystem("game-of-life")

  implicit val ec: ExecutionContextExecutor = system.dispatcher // implicit ExecutionContext for scheduler

  val Width = 20
  val Height = 20

  val world = for {i <- 0 until Width; j <- 0 until Height} yield {
    (i, j)
  }

  // create view so we can send results to
  val view = system.actorOf(Props(classOf[View], Width, Height), "view")

  // create map of cells, key is coordinate, a tuple (Int, Int)
  val cells = world.map { (i, j) =>
    val cellRef = system.actorOf(Props(classOf[Cell], i, j), s"cell_$i-$j") // correct usage of Props, see docs for details
    ((i, j), cellRef)
  }.toMap

  // we need some helpers to work with grid
  val neighbours = (x: Int, y: Int) => Neighbours(
    for (i <- x - 1 to x + 1;
         j <- y - 1 to y + 1
         if (i, j) != (x, y)
         ) yield {
      cells(((i + Width) % Width, (j + Height) % Height))
    }
  )

  world.foreach { (i, j) =>
    cells((i, j)) ! neighbours(i, j) // send cell its' neighbours
  }

  // now we need to synchronously update all cells,
  // this is the main reason why suggested model (one actor - one cell) is probably not the most adequate
  world.foreach { (i, j) =>
    cells((i, j)) ! SetState(isAlive = util.Random.nextBoolean)
  }

  // for simplicity I assume that update will take less then update time
  val refreshTime = 100.millis

  system.scheduler.schedule(1.second, refreshTime) {
    world.foreach {
      cells(_, _) ! Run
    }
  }


}


class View(w: Int, h: Int) extends Actor {

  var actorStates: Map[(Int, Int), Boolean] = Map()

  val symbols = Map((true -> "x "), (false -> ". "))

  def receive: Receive = {
    case UpdateView(alive, x, y) =>
      actorStates = actorStates + (((x, y), alive))
      if (actorStates.size == w * h) {
        for {j <- 0 until h} {
          for (i <- 0 until w) {
            print(symbols(actorStates((i, j))))
          }
          println()
        }
        println()
        actorStates = Map.empty
      }
  }
}

class Cell(x: Int, y: Int) extends Actor {

  var neighbours: Seq[ActorRef] = Seq()
  var neighbourStates: Map[ActorRef, Boolean] = Map() //  Map.empty[Map[ActorRef, Boolean]] is better
  var alive: Boolean = false

  def receive: Receive = {
    case Run =>
      neighbourStates = Map.empty
      neighbours.foreach(_ ! QueryState)
    case SetState(isAlive) =>
      this.alive = isAlive
    case Neighbours(xs) =>
      neighbours = xs
    case QueryState =>
      sender() ! NeighbourState(alive)
    case NeighbourState(alive) =>
      neighbourStates = neighbourStates + ((sender(), alive))
      // this is tricky when all senders has send you their states it doesn't mean that you can mutate your own,
      // they could still may request your internal state, will use hackish previousState
      if (neighbourStates.size == 8) { // total of 8 neighbours sent their states, we are complete with update
        val aliveMembers = neighbourStates.values.count(identity)
        this.alive = aliveMembers match {
          case n if n < 2 => false
          case 3 => true
          case n if n > 3 => false;
          case _ => this.alive
        }

        context.actorSelection("/user/view") ! UpdateView(this.alive, x, y)
      }
  }
}


case class SetState(isAlive: Boolean)

case class Neighbours(xs: Seq[ActorRef])

case object QueryState

case object Run

case class NeighbourState(alive: Boolean)

case class UpdateView(alive: Boolean, x: Int, y: Int)