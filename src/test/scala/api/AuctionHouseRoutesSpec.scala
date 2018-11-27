package api

import actors.AuctionActor.{Bid, Bidder, Closed, Planned}
import actors.AuctionHouseActor._
import actors.utils.TestForwardingActor
import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestKit, TestProbe}
import akka.util.Timeout
import api.auctionHouse.AuctionHouseResponses._
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by argurth on 27/11/18.
  */
class AuctionHouseRoutesSpec
  extends RestRoutes
    with RequestTimeout
    with WordSpecLike
    with ScalaFutures
    with MustMatchers
    with ScalatestRouteTest
    with BeforeAndAfterAll {
  implicit val config: Config = ConfigFactory.load()
  implicit val requestTimeout: Timeout = requestTimeoutFromConfig(config)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  override def createAuctionHouse(): ActorRef =
    system.actorOf(TestForwardingActor.props(probe.ref), "testAuctionHouse")

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  val probe = TestProbe()
  val today: DateTime = DateTime.now
  val tomorrow: DateTime = DateTime.now.plus((1 day).toMillis)

  val defaultAuction: Auction =
    Auction(
      "test",
      100,
      Planned,
      DateTime.now.plus((1 day).toMillis),
      DateTime.now.plus((2 days).toMillis),
      Set(),
      Vector(),
      None
    )

  def checkExceptionsHandling(request: HttpRequest, msg: Any): Unit = {
    def performCheck(e: ErrorResponse, s: StatusCode) = {
      val result = request ~> routes ~> runRoute
      probe.expectMsg(msg)
      probe.reply(e)
      check {
        status mustBe s
        contentType mustBe ContentTypes.`application/json`
        responseAs[String] mustBe e.toJson.toString
      }(result)
    }

    Set(
      AuctionAlreadyExist("test") -> UnprocessableEntity,
      NegativeStartingPrice(100) -> UnprocessableEntity,
      AuctionNotFound("test") -> NotFound,
      NotPermittedByState(Closed) -> Locked,
      BidderAlreadyJoined(Bidder("testBidder")) -> UnprocessableEntity,
      BidderDidNotJoin("test") -> NotFound,
      BidTooLow(100) -> UnprocessableEntity,
      UnknownError() -> InternalServerError
    ) foreach { case (e: ErrorResponse, s: StatusCode) => performCheck(e, s) }
  }

  "The auction house API" must {
    "forward a create auction request to the AuctionHouse actor" in {
      val params = (
        defaultAuction.item,
        defaultAuction.startingPrice,
        defaultAuction.startDate,
        defaultAuction.endDate
      )
      val request =
        Post("/auctions")
          .withEntity(
            ContentTypes.`application/json`,
            CreateAuctionParams.tupled(params).toJson.toString
          )

      val result = request ~> routes ~> runRoute
      probe.expectMsg(CreateAuction.tupled(params))
      probe.reply(AuctionCreated(defaultAuction))
      check {
        status mustBe StatusCodes.Created
        contentType mustBe ContentTypes.`application/json`
        responseAs[String] mustBe defaultAuction.toJson.toString
      }(result)

      checkExceptionsHandling(request, CreateAuction.tupled(params))
    }

    "forward a get auctions request to the AuctionHouse actor" in {
      val request = Get("/auctions")

      val result = request ~> routes ~> runRoute
      probe.expectMsg(GetAuctions)
      probe.reply(AuctionsFound(Set(defaultAuction)))
      check {
        status mustBe StatusCodes.OK
        contentType mustBe ContentTypes.`application/json`
        responseAs[String] mustBe Set(defaultAuction).toJson.toString
      }(result)

      checkExceptionsHandling(request, GetAuctions)
    }

    "forward a get auction request to the AuctionHouse actor" in {
      val request = Get(s"/auctions/${defaultAuction.item}")

      val result = request ~> routes ~> runRoute
      probe.expectMsg(GetAuction(defaultAuction.item))
      probe.reply(AuctionFound(defaultAuction))
      check {
        status mustBe StatusCodes.OK
        contentType mustBe ContentTypes.`application/json`
        responseAs[String] mustBe defaultAuction.toJson.toString
      }(result)

      checkExceptionsHandling(request, GetAuction(defaultAuction.item))
    }

    "forward an update auction request to the AuctionHouse actor" in {
      val request =
        Patch(s"/auctions/${defaultAuction.item}")
          .withEntity(
            ContentTypes.`application/json`,
            UpdateAuctionParams(None, None, None).toJson.toString
          )

      val result = request ~> routes ~> runRoute
      probe.expectMsg(UpdateAuction(
        defaultAuction.item,
        None,
        None,
        None
      ))
      probe.reply(AuctionUpdated(defaultAuction))
      check {
        status mustBe StatusCodes.OK
        contentType mustBe ContentTypes.`application/json`
        responseAs[String] mustBe defaultAuction.toJson.toString
      }(result)

      checkExceptionsHandling(request, UpdateAuction(
        defaultAuction.item,
        None,
        None,
        None
      ))
    }

    "forward a join auction request to the AuctionHouse actor" in {
      val bidder = Bidder("testBidder")
      val request =
        Post(s"/auctions/${defaultAuction.item}/bidders")
          .withEntity(
            ContentTypes.`application/json`,
            JoinAuctionParams(bidder.name).toJson.toString
          )

      val result = request ~> routes ~> runRoute
      probe.expectMsg(JoinAuction(defaultAuction.item, bidder.name))
      probe.reply(AuctionJoined(bidder))
      check {
        status mustBe StatusCodes.Created
        contentType mustBe ContentTypes.`application/json`
        responseAs[String] mustBe bidder.toJson.toString
      }(result)

      checkExceptionsHandling(
        request, JoinAuction(defaultAuction.item, bidder.name)
      )
    }

    "forward a place bid on auction request to the AuctionHouse actor" in {
      val bidder = Bidder("testBidder")
      val bid = Bid("test", 200)
      val request =
        Post(s"/auctions/${defaultAuction.item}/bidders/${bidder.name}")
          .withEntity(
            ContentTypes.`application/json`,
            PlaceBidParams(bid.value).toJson.toString
          )

      val result = request ~> routes ~> runRoute
      probe.expectMsg(PlaceBid(defaultAuction.item, bidder.name, bid.value))
      probe.reply(BidPlaced(bid))
      check {
        status mustBe StatusCodes.Created
        contentType mustBe ContentTypes.`application/json`
        responseAs[String] mustBe bid.toJson.toString
      }(result)

      checkExceptionsHandling(
        request, PlaceBid(defaultAuction.item, bidder.name, bid.value)
      )
    }
  }
}
