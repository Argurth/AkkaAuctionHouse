package api.auctionHouse

import actors.AuctionHouseActor._
import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext

/**
  * Created by argurth on 26/11/18.
  */
trait AuctionHouseRoutes extends AuctionHouseMarshaller {

  import api.auctionHouse.AuctionHouseResponses._

  implicit def executionContext: ExecutionContext

  implicit def requestTimeout: Timeout

  def createAuctionHouse(): ActorRef

  lazy val auctionHouse: ActorRef = createAuctionHouse()

  def auctionHouseAPIRoutes: Route =
    auctionsRoute ~ auctionRoute ~ bidderRoute ~ bidRoute

  def handleExceptions(r: Response): StandardRoute =
    r match {
      case e: ErrorResponse => complete(e.statusCode, e)
      case _ => complete(UnknownError().statusCode, UnknownError().message)
    }

  def auctionsRoute: Route =
    pathPrefix("auctions") {
      pathEndOrSingleSlash {
        // POST /auctions
        post {
          entity(as[CreateAuctionParams]) { params =>
            onSuccess(
              auctionHouse.ask(CreateAuction(
                params.item,
                params.startingPrice,
                params.incrementPolicy,
                params.startDate,
                params.endDate
              )).mapTo[Response]
            ) {
              case s: AuctionCreated => complete(s.statusCode, s.auction)
              case r => handleExceptions(r)
            }
          }
        } ~
          // GET /auctions
          get {
            onSuccess(auctionHouse.ask(GetAuctions).mapTo[Response]) {
              case s: AuctionsFound => complete(s.statusCode, s.auctions)
              case r => handleExceptions(r)
            }
          }
      }
    }

  def auctionRoute: Route =
    pathPrefix("auctions" / Segment) { item =>
      pathEndOrSingleSlash {
        // GET /auctions/:item
        get {
          onSuccess(auctionHouse.ask(GetAuction(item)).mapTo[Response]) {
            case s: AuctionFound => complete(s.statusCode, s.auction)
            case r => handleExceptions(r)
          }
        } ~
          // PATCH /auctions/:item
          patch {
            entity(as[UpdateAuctionParams]) { params =>
              onSuccess(
                auctionHouse.ask(UpdateAuction(
                  item,
                  params.startingPrice,
                  params.incrementPolicy,
                  params.startDate,
                  params.endDate
                )).mapTo[Response]
              ) {
                case s: AuctionUpdated => complete(s.statusCode, s.auction)
                case r => handleExceptions(r)
              }
            }
          }
      }
    }

  def bidderRoute: Route =
    pathPrefix("auctions" / Segment / "bidders") { item =>
      pathEndOrSingleSlash {
        // POST /auctions/:item/bidders
        post {
          entity(as[JoinAuctionParams]) { params =>
            onSuccess(
              auctionHouse
                .ask(JoinAuction(item, params.bidderName))
                .mapTo[Response]
            ) {
              case s: AuctionJoined => complete(s.statusCode, s.bidder)
              case r => handleExceptions(r)
            }
          }
        }
      }
    }

  def bidRoute: Route =
    pathPrefix("auctions" / Segment / "bidders" / Segment / "bids") {
      (item, bidder) => pathEndOrSingleSlash {
        // POST /auctions/:item/bidders/:bidder
        post {
          entity(as[PlaceBidParams]) { params =>
            onSuccess(
              auctionHouse
                .ask(PlaceBid(item, bidder, params.value))
                .mapTo[Response]
            ) {
              case s: BidPlaced => complete(s.statusCode, s.bid)
              case r => handleExceptions(r)
            }
          }
        }
      }
    }
}
