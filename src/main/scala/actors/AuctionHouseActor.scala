package actors

import actors.AuctionActor.WinningBid
import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model.DateTime
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import api.auctionHouse.AuctionHouseResponses._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by argurth on 26/11/18.
  */
object AuctionHouseActor {
  def props(implicit timeout: Timeout) = Props(new AuctionHouseActor())

  def name = "auctionHouse"

  case class Auction(
    item: String,
    startingPrice: Int,
    auctionState: AuctionActor.AuctionState,
    startDate: DateTime,
    endDate: DateTime,
    bidders: Set[AuctionActor.Bidder],
    bids: Vector[AuctionActor.Bid],
    winningBid: Option[WinningBid]
  )

  /** Events **/
  case class CreateAuction(
    item: String,
    startingPrice: Int,
    startDate: DateTime,
    endDate: DateTime
  )

  case object GetAuctions

  case class GetAuction(item: String)

  case class UpdateAuction(
    item: String,
    startingPrice: Option[Int],
    startDate: Option[DateTime],
    endDate: Option[DateTime]
  )

  case class JoinAuction(item: String, bidderName: String)

  case class PlaceBid(item: String, bidderName: String, value: Int)

}

class AuctionHouseActor(implicit timeout: Timeout) extends Actor {

  import AuctionHouseActor._
  import context._

  def createAuction(
    item: String,
    startingPrice: Int,
    startDate: DateTime,
    endDate: DateTime
  ): ActorRef =
    context.actorOf(
      AuctionActor.props(item, startingPrice, startDate, endDate),
      item
    )

  def receive: PartialFunction[Any, Unit] = {
    case CreateAuction(item, startingPrice, startDate, endDate) =>
      if (startingPrice < 0) sender() ! NegativeStartingPrice(startingPrice)
      else context.child(item) match {
        case Some(_) => sender() ! AuctionAlreadyExist(item)
        case None =>
          val futureAuction =
            createAuction(item, startingPrice, startDate, endDate)
              .ask(AuctionActor.Get)
              .mapTo[AuctionFound]
              .map { af => AuctionCreated(af.auction) }

          pipe(futureAuction) to sender()
      }

    case GetAuctions =>
      val getAuctions: Future[AuctionsFound] =
        Future sequence {
          for {
            child <- context.children
          } yield {
            self.ask(GetAuction(child.path.name))
              .mapTo[AuctionFound]
              .map { af => Success(af.auction) }
              .recover { case e => Failure(e) }
          }
        } map { f => AuctionsFound(f.filter(_.isSuccess).map(_.get).toSet) }

      pipe(getAuctions) to sender()

    case GetAuction(item) =>
      context.child(item)
        .fold(sender() ! AuctionNotFound(item))(_ forward AuctionActor.Get)

    case UpdateAuction(item, startingPrice, startDate, endDate) =>
      context.child(item).fold(
        sender() ! AuctionNotFound(item)
      )(
        _ forward AuctionActor.Update(startingPrice, startDate, endDate)
      )

    case JoinAuction(item, username) =>
      context.child(item).fold(
        sender() ! AuctionNotFound(item)
      )(
        _ forward AuctionActor.Join(username)
      )

    case PlaceBid(item, username, bid) =>
      context.child(item).fold(
        sender() ! AuctionNotFound(item)
      )(
        _ forward AuctionActor.PlaceBid(username, bid)
      )
  }
}
