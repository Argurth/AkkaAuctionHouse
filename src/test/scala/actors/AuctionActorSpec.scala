package actors

import actors.AuctionActor._
import actors.AuctionHouseActor.Auction
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.DateTime
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import api.auctionHouse.AuctionHouseResponses._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by argurth on 26/11/18.
  */
class AuctionActorSpec extends TestKit(ActorSystem("testAuctionActor"))
  with WordSpecLike
  with MustMatchers
  with ImplicitSender
  with DefaultTimeout
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  def createAuctionActor(a: Auction): ActorRef =
    system.actorOf(
      AuctionActor.props(a.item, a.startingPrice, a.startDate, a.endDate)
    )

  "A planned auction" must {
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

    "return itself when receiving a get event" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
    }

    "update itself when it receive an update event" in {
      val auctionActor: ActorRef =
        createAuctionActor(defaultAuction)

      val newPrice = 200
      val newStart = DateTime.now.plus((3 days).toMillis)
      val newEnd = DateTime.now.plus((5 days).toMillis)

      def checkUpdated(event: Update, result: Auction) = {
        auctionActor ! event
        expectMsg(AuctionUpdated(result))
        auctionActor ! Get
        expectMsg(AuctionFound(result))
      }

      // Update nothing
      checkUpdated(
        event = Update(),
        result = defaultAuction
      )
      // Update only starting price
      checkUpdated(
        event = Update(startingPrice = Some(newPrice)),
        result = defaultAuction.copy(startingPrice = newPrice)
      )
      // Update only start date
      checkUpdated(
        event = Update(startDate = Some(newStart)),
        result = defaultAuction.copy(
          startingPrice = newPrice, startDate = newStart
        )
      )
      // Update only end date
      checkUpdated(
        event = Update(endDate = Some(newEnd)),
        result = defaultAuction.copy(
          startingPrice = newPrice, startDate = newStart, endDate = newEnd
        )
      )
      // Update all
      checkUpdated(
        event = Update(
          startingPrice = Some(defaultAuction.startingPrice),
          startDate = Some(defaultAuction.startDate),
          endDate = Some(defaultAuction.endDate)
        ),
        result = defaultAuction
      )
    }

    "change to opened when the startDate is reached" in {
      val newStart = DateTime.now.plus(50)
      val auctionActor: ActorRef =
        createAuctionActor(defaultAuction.copy(startDate = newStart))

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction.copy(startDate = newStart)))

      awaitAssert({
        auctionActor ! Get
        expectMsg(AuctionFound(defaultAuction.copy(
          startDate = newStart,
          auctionState = Opened
        )))
      }, 2 seconds, 500 millis)
    }

    "send back a NotPermittedByState when an user try to join" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
      auctionActor ! Join("testBidder")
      expectMsg(NotPermittedByState(Planned))
      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
    }

    "send back a NotPermittedByState when it receive a place bid event" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
      auctionActor ! PlaceBid("testBidder", 300)
      expectMsg(NotPermittedByState(Planned))
      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
    }
  }

  "An opened auction" must {
    val defaultAuction: Auction =
      Auction(
        "test",
        100,
        Opened,
        DateTime.now.minus((1 day).toMillis),
        DateTime.now.plus((1 day).toMillis),
        Set(),
        Vector(),
        None
      )

    "return itself when receiving a get event" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
    }

    "subscribe a user when it receive a join event" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)
      val testBidder = Bidder("testBidder")

      auctionActor ! Join(testBidder.name)
      expectMsg(AuctionJoined(testBidder))
      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction.copy(bidders = Set(testBidder))))
    }

    "place a bid for a user when it receive a place bid event when the " +
      "bid is greater than the previous one" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)
      val testBidder = Bidder("testBidder")
      val testBidder2 = Bidder("testBidder2")
      val testBid = Bid(testBidder.name, defaultAuction.startingPrice + 100)
      val testBid2 = Bid(testBidder.name, testBid.value + 100)
      val testBid3 = Bid(testBidder2.name, testBid2.value + 100)

      auctionActor ! Join(testBidder.name)
      expectMsg(AuctionJoined(testBidder))
      auctionActor ! Join(testBidder2.name)
      expectMsg(AuctionJoined(testBidder2))

      def checkBidPlaced(bid: Bid, expectBidsState: Vector[Bid]) = {
        auctionActor ! PlaceBid(bid.bidderName, bid.value)
        expectMsg(BidPlaced(bid))
        auctionActor ! Get
        expectMsg(AuctionFound(defaultAuction.copy(
          bidders = Set(testBidder, testBidder2),
          bids = expectBidsState
        )))
      }

      // Greater than starting price
      checkBidPlaced(testBid, Vector(testBid))
      // Greater than previous price, same user
      checkBidPlaced(testBid2, Vector(testBid2, testBid))
      // Greater than previous price, another user
      checkBidPlaced(testBid3, Vector(testBid3, testBid2, testBid))
    }

    "place a bid for a user when it receive a place bid event when the " +
      "first bid is equal to the starting price" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)
      val testBidder = Bidder("testBidder")
      val testBid = Bid(testBidder.name, defaultAuction.startingPrice)

      auctionActor ! Join(testBidder.name)
      expectMsg(AuctionJoined(testBidder))

      auctionActor ! PlaceBid(testBid.bidderName, testBid.value)
      expectMsg(BidPlaced(testBid))
      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction.copy(
        bidders = Set(testBidder),
        bids = Vector(testBid)
      )))
    }

    "change to closed and pick no winner when the endDate is reached" +
      " if no one placed a bid" in {
      val newEnd = DateTime.now.plus(10)
      val auctionActor: ActorRef =
        createAuctionActor(defaultAuction.copy(endDate = newEnd))

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction.copy(endDate = newEnd)))

      awaitAssert({
        auctionActor ! Get
        expectMsg(AuctionFound(defaultAuction.copy(
          endDate = newEnd,
          auctionState = Closed
        )))
      }, 2 seconds, 500 millis)
    }

    "change to closed and pick a winner when the endDate is reached" in {
      val newEnd = DateTime.now.plus(50)
      val auctionActor: ActorRef =
        createAuctionActor(defaultAuction.copy(endDate = newEnd))
      val testBidder = Bidder("testBidder")
      val testBid = Bid(testBidder.name, defaultAuction.startingPrice)

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction.copy(endDate = newEnd)))
      auctionActor ! Join(testBidder.name)
      expectMsg(AuctionJoined(testBidder))
      auctionActor ! PlaceBid(testBid.bidderName, testBid.value)
      expectMsg(BidPlaced(testBid))

      awaitAssert({
        auctionActor ! Get
        expectMsg(AuctionFound(defaultAuction.copy(
          endDate = newEnd,
          auctionState = Closed,
          bidders = Set(testBidder),
          bids = Vector(testBid),
          winningBid = Some(WinningBid(testBidder, testBid))
        )))
      }, 2 seconds, 500 millis)
    }

    "send back a NotPermittedByState when it receive an update event" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)
      val newPrice = 200

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
      auctionActor ! Update(startingPrice = Some(newPrice))
      expectMsg(NotPermittedByState(Opened))
      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
    }

    "send back a BidderAlreadyJoined when it receive a join event " +
      "with an already existing user" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)
      val testBidder = Bidder("testBidder")

      auctionActor ! Join(testBidder.name)
      expectMsg(AuctionJoined(testBidder))

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction.copy(bidders = Set(testBidder))))
      auctionActor ! Join(testBidder.name)
      expectMsg(BidderAlreadyJoined(testBidder))
      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction.copy(bidders = Set(testBidder))))
    }

    "send back a BidderDidNotJoin if the bidder didn't join when it receive " +
      "a place bid event" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)
      val testBid = Bid("testBidder", defaultAuction.startingPrice)

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
      auctionActor ! PlaceBid(testBid.bidderName, testBid.value)
      expectMsg(BidderDidNotJoin(testBid.bidderName))
      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
    }

    "send back a BidTooLow if no bid had been made when it receive a " +
      "place bid event with a bid smaller than the starting price" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)
      val testBidder = Bidder("testBidder")
      val testBid = Bid(testBidder.name, 0)

      auctionActor ! Join(testBidder.name)
      expectMsg(AuctionJoined(testBidder))

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction.copy(bidders = Set(testBidder))))
      auctionActor ! PlaceBid(testBid.bidderName, testBid.value)
      expectMsg(BidTooLow(testBid.value))
      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction.copy(bidders = Set(testBidder))))
    }

    "send back a BidTooLow if a bid had been made when it receive " +
      "a place bid event with a bid not greater than the highest bid" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)
      val testBidder = Bidder("testBidder")
      val testBidder2 = Bidder("testBidder2")
      val testBid = Bid(testBidder.name, defaultAuction.startingPrice)

      auctionActor ! Join(testBidder.name)
      expectMsg(AuctionJoined(testBidder))
      auctionActor ! Join(testBidder2.name)
      expectMsg(AuctionJoined(testBidder2))
      auctionActor ! PlaceBid(testBid.bidderName, testBid.value)
      expectMsg(BidPlaced(testBid))

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction.copy(
        bidders = Set(testBidder, testBidder2),
        bids = Vector(testBid)
      )))

      def expectBidTooLow(bid: Bid) = {
        auctionActor ! PlaceBid(bid.bidderName, bid.value)
        expectMsg(BidTooLow(bid.value))
        auctionActor ! Get
        expectMsg(AuctionFound(defaultAuction.copy(
          bidders = Set(testBidder, testBidder2),
          bids = Vector(testBid)
        )))
      }

      // Same user, equal
      expectBidTooLow(testBid)
      // Another user, equal
      expectBidTooLow(Bid(testBidder2.name, defaultAuction.startingPrice))
      // Same user, smaller
      expectBidTooLow(Bid(testBidder.name, 0))
      // Another user, smaller
      expectBidTooLow(Bid(testBidder2.name, 0))
    }
  }

  "A closed auction" must {
    val defaultAuction: Auction =
      Auction(
        "test",
        100,
        Closed,
        DateTime.now.minus((2 days).toMillis),
        DateTime.now.minus((1 day).toMillis),
        Set(),
        Vector(),
        None
      )
    "return itself when receiving a get event" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
    }

    "send back a NotPermittedByState when it receive an update event" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)
      val newPrice = 200

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
      auctionActor ! Update(startingPrice = Some(newPrice))
      expectMsg(NotPermittedByState(Closed))
      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
    }

    "send back a NotPermittedByState when an user try to join" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
      auctionActor ! Join("testBidder")
      expectMsg(NotPermittedByState(Closed))
      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
    }

    "send back a NotPermittedByState when it receive a place bid event" in {
      val auctionActor: ActorRef = createAuctionActor(defaultAuction)

      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
      auctionActor ! PlaceBid("testBidder", 300)
      expectMsg(NotPermittedByState(Closed))
      auctionActor ! Get
      expectMsg(AuctionFound(defaultAuction))
    }
  }
}
