package api.auctionHouse

import actors.{AuctionActor, AuctionHouseActor}
import akka.http.scaladsl.model.{StatusCode, StatusCodes}

/**
  * Created by argurth on 26/11/18.
  */
object AuctionHouseResponses {

  import StatusCodes._

  sealed trait Response

  /** Successful responses **/
  sealed class SuccessResponse(val statusCode: StatusCode) extends Response

  case class AuctionCreated(auction: AuctionHouseActor.Auction)
    extends SuccessResponse(Created)

  case class AuctionsFound(auctions: Set[AuctionHouseActor.Auction])
    extends SuccessResponse(OK)

  case class AuctionFound(auction: AuctionHouseActor.Auction)
    extends SuccessResponse(OK)

  case class AuctionUpdated(auction: AuctionHouseActor.Auction)
    extends SuccessResponse(OK)

  case class AuctionJoined(bidder: AuctionActor.Bidder)
    extends SuccessResponse(Created)

  case class BidPlaced(bid: AuctionActor.Bid)
    extends SuccessResponse(Created)

  /** Errors **/
  sealed class ErrorResponse(val message: String, val statusCode: StatusCode)
    extends Response

  case class AuctionAlreadyExist(item: String) extends
    ErrorResponse(s"The auction '$item' already exist", UnprocessableEntity)

  case class NegativeStartingPrice(price: Int) extends
    ErrorResponse(
      s"The provided starting price '$price' is negative",
      UnprocessableEntity
    )

  case class AuctionNotFound(item: String) extends
    ErrorResponse(s"The auction '$item' doesn't exist", NotFound)

  case class NotPermittedByState(as: AuctionActor.AuctionState)
    extends ErrorResponse(
      s"The actual auction state '${as.key}' doesn't allow this action",
      Locked
    )

  case class BidderAlreadyJoined(bidder: AuctionActor.Bidder)
    extends ErrorResponse(
      s"'${bidder.name}' already joined this auction", UnprocessableEntity)

  case class BidderDidNotJoin(bidderName: String) extends
    ErrorResponse(s"'$bidderName' hasn't joined this auction", NotFound)

  case class BidTooLow(bid: Int)
    extends ErrorResponse(
      s"The bid '$bid' is too low for this auction", UnprocessableEntity)

  case class UnknownError(
    details: String = "An unknown error happened",
    code: StatusCode = InternalServerError
  ) extends ErrorResponse(details, code)

}
