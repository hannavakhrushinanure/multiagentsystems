package masandt

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

class CaveRoom(var smell: Boolean, var wind: Boolean, var gold: Boolean, var pit: Boolean, var wampus: Boolean) {
  override def toString: String = "smell = " + smell + " wind = " + wind + " gold = " + gold + " pit = " + pit + " wampus = " + wampus
}

class Cave(level1: List[CaveRoom], level2: List[CaveRoom], level3: List[CaveRoom], level4: List[CaveRoom]) {
  val state = Map(
    1 -> level1,
    2 -> level2,
    3 -> level3,
    4 -> level4
  )

  def printCaveState() {
    println("The cave state: ")
    state.foreach(row => println("level " + row._1 + ": " + row._2.mkString("[", ", ", "]")))
  }
}

case class SensesRequestMessage(i: Int, j: Int, version: Int) {
  override def toString: String = "i = " + i + " j = " + j + " version = " + version
}

case class SensesResponseMessage(smell: Boolean, wind: Boolean, shine: Boolean, punch: Boolean, version: Int) {
  override def toString: String = "smell = " + smell + " wind = " + wind + " shine = " + shine + " punch = " + punch + " version = " + version
}

case object StopMessage

class WorldActor extends Actor {
  var cave = None: Option[Cave]
  init()

  def receive = {
    case SensesRequestMessage(i, j, version) =>
      println("an actor " + sender + " requested its senses for: " + "i = " + i + " j = " + j + " version = " + version)
      val resp = processSensesRequest(i, j, version)
      println("TravelerActor: sending a response = " + resp)
      sender ! resp

    case StopMessage =>
      println("WorldActor: game over")
      context.stop(self)

    case _ =>
      println("WorldActor: unsupported message")
  }

  def init() {
    println("Initializing the World Actor")
    cave = Some(new Cave(
      List(
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = true, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = true, wampus = false),
        new CaveRoom(smell = false, wind = true, gold = false, pit = false, wampus = false)),
      List(
        new CaveRoom(smell = true, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = true, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false)),
      List(
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = true),
        new CaveRoom(smell = true, wind = true, gold = true, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = true, wampus = false),
        new CaveRoom(smell = false, wind = true, gold = false, pit = false, wampus = false)),
      List(
        new CaveRoom(smell = true, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = true, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = true, wampus = false))
    ))
    println("WorldActor: ")
    cave.get.printCaveState()
  }

  def processSensesRequest(i: Int, j: Int, versionInReq: Int): SensesResponseMessage = {
    var targetRoom: CaveRoom = this.cave.get.state.apply(i).apply(j - 1)
    return SensesResponseMessage(smell = targetRoom.smell, wind = targetRoom.wind, shine = targetRoom.gold, punch = targetRoom.wampus, version = versionInReq)
  }

}

class TravelerActor(worldActor: ActorRef) extends Actor {
  var cave = None: Option[Cave]
  var dialogCurrentVersion = 0
  var currentPositionI = 0
  var currentPositionJ = 0

  var isDead = false
  var hasWon = false

  init()

  def receive = {
    case SensesResponseMessage(smell, wind, shine, punch, version) =>
      println("an actor " + sender + " send me the senses response: " + "smell = " + smell + " wind = " + wind + " shine = " + shine + " punch = " + punch + " version = " + version)
      val processedCorrectly = processSensesResponse(smell, wind, shine, punch, version)
      if (processedCorrectly) {
        if (isDead || hasWon) {
          println("TravelerActor: the game is over: I've " + (if (isDead) "died" else "won"))
          worldActor ! StopMessage
        } else {
          sendSensesRequest()
        }
      }

    case _ => println("TravelerActor: unsupported message")
  }

  def init() {
    println("Initializing the Traveler Actor")
    currentPositionJ = 1
    currentPositionI = 1
    dialogCurrentVersion = 1
    cave = Some(new Cave(
      List(
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false)),
      List(
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false)),
      List(
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false)),
      List(
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false),
        new CaveRoom(smell = false, wind = false, gold = false, pit = false, wampus = false))
    ))
    println("TravelerActor: ")
    cave.get.printCaveState()

    sendSensesRequest()
  }

  def sendSensesRequest() {
    val request = SensesRequestMessage(currentPositionI, currentPositionJ, dialogCurrentVersion)
    println("TravelerActor: sending a request = " + request)
    worldActor ! request
  }

  def processSensesResponse(smell: Boolean, wind: Boolean, shine: Boolean, punch: Boolean, version: Int): Boolean = {
    if (dialogCurrentVersion != version) {
      return false
    }
    if (punch) {
      isDead = true
      return true
    }
    if (shine) {
      hasWon = true
      return true
    }

    val targetRoom = this.cave.get.state.apply(currentPositionI).apply(currentPositionJ)
    targetRoom.smell = smell
    targetRoom.wind = wind

    // asking for user decision about the next step
    println("For the cave current state: ")
    this.cave.get.printCaveState()
    var userCommand = ""
    var hasBeenReadSuccessfully = false
    while (!hasBeenReadSuccessfully) {
      println("Please enter the next move(the following values are available: LEFT, RIGHT, UP, DOWN):")
      userCommand = scala.io.StdIn.readLine()
      userCommand match {
        case "LEFT" =>
          val proposedPosition = currentPositionJ - 1
          if (proposedPosition < 1) {
            println("Unable to accept this move: you've reached a cave border, try again")
          } else {
            currentPositionJ = proposedPosition
            hasBeenReadSuccessfully = true
          }
        case "RIGHT" =>
          val proposedPosition = currentPositionJ + 1
          if (proposedPosition > 4) {
            println("Unable to accept this move: you've reached a cave border, try again")
          } else {
            currentPositionJ = proposedPosition
            hasBeenReadSuccessfully = true
          }
        case "UP" =>
          val proposedPosition = currentPositionI + 1
          if (proposedPosition > 4) {
            println("Unable to accept this move: you've reached a cave border, try again")
          } else {
            currentPositionI = proposedPosition
            hasBeenReadSuccessfully = true
          }
        case "DOWN" =>
          val proposedPosition = currentPositionI - 1
          if (proposedPosition < 1) {
            println("Unable to accept this move: you've reached a cave border, try again")
          } else {
            currentPositionI = proposedPosition
            hasBeenReadSuccessfully = true
          }
        case whoa => println("Unable to understand the line given, try again")
      }
    }

    dialogCurrentVersion = dialogCurrentVersion + 1
    return true
  }
}

object Main extends App {
  val system = ActorSystem("WampusWorldSystem")
  val worldActor = system.actorOf(Props[WorldActor], name = "worldActor")
  val travelerActor = system.actorOf(Props(new TravelerActor(worldActor)), name = "travelerActor")
}
