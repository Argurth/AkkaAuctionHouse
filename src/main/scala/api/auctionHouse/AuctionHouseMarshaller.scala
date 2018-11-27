package api.auctionHouse

import actors.AuctionActor
import akka.http.scaladsl.model.DateTime
import spray.json._

/**
  * Created by argurth on 26/11/18.
  */
trait AuctionHouseMarshaller extends DefaultJsonProtocol {

  import actors.AuctionHouseActor._
  import actors.AuctionActor._
  import api.auctionHouse.AuctionHouseResponses._

  case class CreateAuctionParams(
    item: String,
    startingPrice: Int,
    startDate: DateTime,
    endDate: DateTime
  )

  case class UpdateAuctionParams(
    startingPrice: Option[Int],
    startDate: Option[DateTime],
    endDate: Option[DateTime]
  )

  case class JoinAuctionParams(bidderName: String)

  case class PlaceBidParams(bid: Int)

  implicit object DateTimeFormat extends RootJsonFormat[DateTime] {
    def write(d: DateTime) = JsNumber(d.clicks)

    def read(json: JsValue): DateTime = json match {
      case JsNumber(value) => DateTime(value.toLong)
      case _ => deserializationError("Timestamp expected")
    }
  }

  implicit object AuctionStateFormat extends RootJsonFormat[AuctionState] {
    def write(as: AuctionState) = JsString(as.key)

    def read(json: JsValue): AuctionState = json match {
      case JsString(value) =>
        AuctionActor.availableAuctionStates.find(_.key == value) match {
          case Some(as) => as
          case None => deserializationError("Auction state not found")
        }
      case _ => deserializationError("Auction state expected")
    }
  }

  implicit val createAuctionParamsFormat:
    RootJsonFormat[CreateAuctionParams] = jsonFormat4(CreateAuctionParams)
  implicit val updateAuctionParamsFormat:
    RootJsonFormat[UpdateAuctionParams] = jsonFormat3(UpdateAuctionParams)
  implicit val joinAuctionParamsFormat: RootJsonFormat[JoinAuctionParams] =
    jsonFormat1(JoinAuctionParams)
  implicit val placeBidParamsFormat: RootJsonFormat[PlaceBidParams] =
    jsonFormat1(PlaceBidParams)

  implicit val bidderFormat: RootJsonFormat[Bidder] =
    jsonFormat1(Bidder)
  implicit val bidFormat: RootJsonFormat[Bid] =
    jsonFormat2(Bid)
  implicit val winningBidFormat: RootJsonFormat[WinningBid] =
    jsonFormat2(WinningBid)
  implicit val auctionFormat: RootJsonFormat[Auction] =
    jsonFormat8(Auction)

  implicit object ErrorResponseWriter
    extends RootJsonWriter[ErrorResponse] {
    def write(er: ErrorResponse) = JsObject("message" -> JsString(er.message))
  }

  implicit object UnknownErrorWriter extends RootJsonWriter[UnknownError] {
    def write(ue: UnknownError) = JsObject("message" -> JsString(ue.message))
  }

}
