package actors

import actors.AuctionActor.IncrementPolicy
import akka.actor.{Actor, Props}
import akka.http.scaladsl.model.DateTime
import api.auctionHouse.AuctionHouseResponses._

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by argurth on 26/11/18.
  */
object AuctionActor {
  def props(
    item: String,
    startingPrice: Int,
    incrementPolicy: IncrementPolicy,
    startDate: DateTime,
    endDate: DateTime
  ) = Props(
    new AuctionActor(item, startingPrice, incrementPolicy, startDate, endDate)
  )

  case class Bidder(name: String)

  case class Bid(bidderName: String, value: Int)

  case class WinningBid(bidder: Bidder, bid: Bid)

  sealed abstract class AuctionState(val key: String)

  case object Planned extends AuctionState("Planned")

  case object Opened extends AuctionState("Opened")

  case object Closed extends AuctionState("Closed")

  val availableAuctionStates: Vector[AuctionState] =
    Vector(Planned, Opened, Closed)

  sealed abstract class IncrementPolicy(val key: String)
  case object FreeIncrement extends IncrementPolicy("FreeIncrement")
  case class MinimalIncrement(min: Int)
    extends IncrementPolicy("MinimalIncrement")

  /** Events **/
  case object Get

  case object UpdateState

  case class Update(
    startingPrice: Option[Int] = None,
    incrementPolicy: Option[IncrementPolicy] = None,
    startDate: Option[DateTime] = None,
    endDate: Option[DateTime] = None
  )

  case class Join(username: String)

  case class PlaceBid(username: String, bid: Int)

}

class AuctionActor(
  item: String,
  var startingPrice: Int,
  var incrementPolicy: IncrementPolicy,
  var startDate: DateTime,
  var endDate: DateTime
) extends Actor {

  import AuctionActor._
  import context._

  var auctionState: AuctionState =
    if (DateTime.now > endDate) Closed
    else if (DateTime.now > startDate) Opened
    else Planned

  system.scheduler.schedule(0 second, 500 milliseconds, self, UpdateState)

  var bidders: Set[Bidder] = Set.empty[Bidder]
  var bids: List[Bid] = List.empty[Bid]

  var winner: Option[WinningBid] = None

  def auction =
    AuctionHouseActor.Auction(
      item,
      startingPrice,
      incrementPolicy,
      auctionState,
      startDate,
      endDate,
      bidders,
      bids.toVector,
      winner
    )

  def updateAuctionState(): Unit =
    auctionState match {
      case Planned if DateTime.now > startDate => auctionState = Opened
      case Opened if DateTime.now > endDate =>
        auctionState = Closed

        def notifyWinner(bidder: Bidder): Unit =
          Unit // Call to some notification in a real-life situation

        @tailrec
        def selectWinner(bidList: List[Bid]): Unit =
          bids match {
            case head :: tail =>
              bidders.find(_.name == head.bidderName) match {
                case Some(b) =>
                  winner = Some(WinningBid(b, head))
                  notifyWinner(b)
                case None => selectWinner(tail)
              }
            case _ => Unit
          }

        selectWinner(bids)

      case _ => Unit
    }

  def receive: PartialFunction[Any, Unit] = {
    case Get => sender() ! AuctionFound(auction)

    case UpdateState => updateAuctionState()

    case Update(
      newStartingPrice,
      newIncrementPolicy,
      newStartDate,
      newEndDate
    ) =>
      auctionState match {
        case Planned => newStartingPrice match {
          case Some(newSP) if newSP < 0 =>
            sender() ! NegativeStartingPrice(newSP)
          case _ =>
            newStartingPrice.foreach(startingPrice = _)
            newIncrementPolicy.foreach(incrementPolicy = _)
            newStartDate.foreach(startDate = _)
            newEndDate.foreach(endDate = _)
            updateAuctionState()
            sender() ! AuctionUpdated(auction)
        }
        case _ => sender() ! NotPermittedByState(auctionState)
      }

    case Join(username) =>
      val newBidder = Bidder(username)

      def addBidder(): AuctionJoined = {
        bidders = bidders + newBidder
        AuctionJoined(newBidder)
      }

      auctionState match {
        case Opened if !bidders.contains(newBidder) => sender() ! addBidder()
        case Opened => sender() ! BidderAlreadyJoined(newBidder)
        case _ => sender() ! NotPermittedByState(auctionState)
      }

    case PlaceBid(username, bid) =>
      val bidder = Bidder(username)

      def addBid(): BidPlaced = {
        bids = Bid(bidder.name, bid) :: bids
        BidPlaced(bids.head)
      }

      auctionState match {
        case Opened =>
          if (!bidders.contains(bidder))
            sender() ! BidderDidNotJoin(bidder.name)
          else (incrementPolicy, bids) match {
            case (FreeIncrement, highestBid :: _) if bid > highestBid.value =>
              sender() ! addBid()
            case (MinimalIncrement(min), highestBid :: _)
              if bid >= highestBid.value + min => sender() ! addBid()
            case (_, List()) if bid >= startingPrice => sender() ! addBid()
            case _ => sender() ! BidTooLow(bid)
          }
        case _ => sender() ! NotPermittedByState(auctionState)
      }
  }
}
