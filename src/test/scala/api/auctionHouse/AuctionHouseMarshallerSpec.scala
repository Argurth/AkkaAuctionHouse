package api.auctionHouse

import actors.AuctionActor.{FreeIncrement, _}
import actors.AuctionHouseActor.Auction
import akka.http.scaladsl.model.DateTime
import api.auctionHouse.AuctionHouseResponses._
import org.scalatest.{MustMatchers, WordSpecLike}
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by argurth on 27/11/18.
  */
class AuctionHouseMarshallerSpec extends AuctionHouseMarshaller
  with WordSpecLike
  with MustMatchers {
  val today: DateTime = DateTime.now
  val tomorrow: DateTime = DateTime.now.plus((1 day).toMillis)

  def incrementToJson(incrementPolicy: IncrementPolicy): JsValue =
    incrementPolicy.toJson

  "The AuctionHouseMarshaller" must {
    def checkFormat[T](objectToTest: T, jsValue: JsValue)
      (implicit jsonFormat: JsonFormat[T]) = {
      objectToTest.toJson mustBe jsValue
      jsValue.convertTo[T] mustBe objectToTest
    }

    def checkWriting[T](objectToTest: T, jsValue: JsValue)
      (implicit jsonWriter: JsonWriter[T]) = {
      objectToTest.toJson mustBe jsValue
    }

    "successfully convert a DateTime to/from json" in {
      checkFormat(today, JsNumber(today.clicks))
    }

    "successfully convert an AuctionState to/from json" in {
      checkFormat[AuctionState](Planned, JsString(Planned.key))
      checkFormat[AuctionState](Opened, JsString(Opened.key))
      checkFormat[AuctionState](Closed, JsString(Closed.key))
    }

    "successfully convert an IncrementPolicy to/from json" in {
      checkFormat[IncrementPolicy](
        FreeIncrement,
        JsObject("key" -> JsString("FreeIncrement"))
      )
      checkFormat[IncrementPolicy](
        MinimalIncrement(100),
        JsObject(
          "key" -> JsString("MinimalIncrement"),
          "min" -> JsNumber(100)
        )
      )
    }

    "successfully convert a CreateAuctionParams to/from json" in {
      checkFormat(
        CreateAuctionParams("test", 100, FreeIncrement, today, tomorrow),
        JsObject(
          "item" -> JsString("test"),
          "startingPrice" -> JsNumber(100),
          "incrementPolicy" -> incrementToJson(FreeIncrement),
          "startDate" -> today.toJson,
          "endDate" -> tomorrow.toJson
        )
      )
    }

    "successfully convert an UpdateAuctionParams to/from json" in {
      checkFormat(
        UpdateAuctionParams(
          Some(100), Some(FreeIncrement), Some(today), Some(tomorrow)
        ),
        JsObject(
          "startingPrice" -> JsNumber(100),
          "incrementPolicy" -> incrementToJson(FreeIncrement),
          "startDate" -> today.toJson,
          "endDate" -> tomorrow.toJson
        )
      )

      checkFormat(
        UpdateAuctionParams(None, Some(FreeIncrement), Some(today), Some(tomorrow)),
        JsObject(
          "incrementPolicy" -> incrementToJson(FreeIncrement),
          "startDate" -> today.toJson,
          "endDate" -> tomorrow.toJson
        )
      )

      checkFormat(
        UpdateAuctionParams(None, None, Some(today), Some(tomorrow)),
        JsObject("startDate" -> today.toJson, "endDate" -> tomorrow.toJson)
      )

      checkFormat(
        UpdateAuctionParams(None, None, None, Some(tomorrow)),
        JsObject("endDate" -> tomorrow.toJson)
      )

      checkFormat(UpdateAuctionParams(None, None, None, None), JsObject())
    }

    "successfully convert a JoinAuctionParams to/from json" in {
      checkFormat(
        JoinAuctionParams("test"),
        JsObject("bidderName" -> JsString("test"))
      )
    }

    "successfully convert a PlaceBidParams to/from json" in {
      checkFormat(
        PlaceBidParams(100),
        JsObject("value" -> JsNumber(100))
      )
    }

    "successfully convert a Bidder to/from json" in {
      checkFormat(
        Bidder("testBidder"),
        JsObject("name" -> JsString("testBidder"))
      )
    }

    "successfully convert a Bid to/from json" in {
      checkFormat(
        Bid("testBidder", 100),
        JsObject(
          "bidderName" -> JsString("testBidder"),
          "value" -> JsNumber(100)
        )
      )
    }

    "successfully convert a WinningBid to/from json" in {
      checkFormat(
        WinningBid(Bidder("testBidder"), Bid("test", 100)),
        JsObject(
          "bidder" -> Bidder("testBidder").toJson,
          "bid" -> Bid("test", 100).toJson
        )
      )
    }

    "successfully convert an Auction to/from json" in {
      val state: AuctionState = Planned
      checkFormat(
        Auction(
          "test",
          100,
          FreeIncrement,
          state,
          today,
          tomorrow,
          Set(),
          Vector(),
          None
        ),
        JsObject(
          "item" -> JsString("test"),
          "startingPrice" -> JsNumber(100),
          "incrementPolicy" -> incrementToJson(FreeIncrement),
          "auctionState" -> state.toJson,
          "startDate" -> today.toJson,
          "endDate" -> tomorrow.toJson,
          "bidders" -> JsArray(),
          "bids" -> JsArray()
        )
      )

      val bidders = Set(Bidder("testBidder"), Bidder("test2"))
      val bids = Vector(Bid("test", 200), Bid("test2", 100))
      val winningBid = WinningBid(Bidder("testBidder"), Bid("test", 200))
      checkFormat(
        Auction(
          "test",
          100,
          MinimalIncrement(10),
          state,
          today,
          tomorrow,
          bidders,
          bids,
          Some(winningBid)
        ),
        JsObject(
          "item" -> JsString("test"),
          "startingPrice" -> JsNumber(100),
          "incrementPolicy" -> incrementToJson(MinimalIncrement(10)),
          "auctionState" -> state.toJson,
          "startDate" -> today.toJson,
          "endDate" -> tomorrow.toJson,
          "bidders" -> JsArray(bidders.map(_.toJson).toVector),
          "bids" -> JsArray(bids.map(_.toJson)),
          "winningBid" -> winningBid.toJson
        )
      )
    }

    "successfully convert an ErrorResponse to/from json" in {
      Set(
        AuctionAlreadyExist("test"),
        NegativeStartingPrice(100),
        AuctionNotFound("test"),
        NotPermittedByState(Closed),
        BidderAlreadyJoined(Bidder("testBidder")),
        BidderDidNotJoin("test"),
        BidTooLow(100)
      ) foreach { case (e: ErrorResponse) =>
        checkWriting[ErrorResponse](
          e,
          JsObject("message" -> JsString(e.message))
        )
      }
    }

    "successfully convert an UnknownError to/from json" in {
      checkWriting[UnknownError](
        UnknownError(),
        JsObject("message" -> JsString(UnknownError().message))
      )
    }

    "throw a DeserializationException if the state key is invalid" in {
      a[DeserializationException] must be thrownBy {
        JsString("invalidKey").convertTo[AuctionState]
      }
    }
  }
}
