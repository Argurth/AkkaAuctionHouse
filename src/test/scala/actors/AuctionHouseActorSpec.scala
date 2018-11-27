package actors

import actors.AuctionHouseActor._
import actors.utils.TestForwardingActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.DateTime
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import api.auctionHouse.AuctionHouseResponses._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by argurth on 27/11/18.
  */
class AuctionHouseActorSpec
  extends TestKit(ActorSystem("testAuctionHouseActor"))
    with WordSpecLike
    with MustMatchers
    with ImplicitSender
    with DefaultTimeout
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  def createWithProbe(probe: TestProbe): ActorRef =
    system.actorOf(Props(
      new AuctionHouseActor {
        override def createAuction(
          name: String,
          startingPrice: Int,
          startDate: DateTime,
          endDate: DateTime
        ): ActorRef =
          context.actorOf(TestForwardingActor.props(probe.ref), name)
      }
    ))

  "An auction house" must {
    val defaultAuction: Auction =
      Auction(
        "test",
        100,
        AuctionActor.Planned,
        DateTime.now.plus((1 day).toMillis),
        DateTime.now.plus((2 days).toMillis),
        Set(),
        Vector(),
        None
      )

    def createAuction(
      auctionHouseActor: ActorRef,
      probe: TestProbe
    )(
      auction: Auction
    ) = {
      auctionHouseActor ! CreateAuction(
        auction.item,
        auction.startingPrice,
        auction.startDate,
        auction.endDate
      )
      probe.expectMsg(AuctionActor.Get)
      probe.reply(AuctionFound(defaultAuction))
      expectMsg(AuctionCreated(defaultAuction))
    }


    "create an auction when it receive a CreateAuction message" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)

      createAuction(auctionHouseActor, probe)(defaultAuction)
    }

    "forward an AuctionActor.Get message when it receive a " +
      "GetAuction message" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)

      createAuction(auctionHouseActor, probe)(defaultAuction)

      auctionHouseActor ! GetAuction(defaultAuction.item)
      probe.expectMsg(AuctionActor.Get)
      probe.reply(AuctionFound(defaultAuction))
      expectMsg(AuctionFound(defaultAuction))
    }

    "send back a list of auction when it receive a GetAuctions message" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)
      val itemName2 = "item2"

      createAuction(auctionHouseActor, probe)(defaultAuction)
      createAuction(auctionHouseActor, probe)(
        defaultAuction.copy(item = itemName2)
      )

      auctionHouseActor ! GetAuctions
      probe.expectMsg(AuctionActor.Get)
      probe.reply(AuctionFound(defaultAuction))
      probe.expectMsg(AuctionActor.Get)
      probe.reply(AuctionFound(defaultAuction.copy(item = itemName2)))
      expectMsg(AuctionsFound(
        Set(defaultAuction, defaultAuction.copy(item = itemName2))
      ))
    }

    "forward an AuctionActor.Update message when it receive an " +
      "UpdateAuction message" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)
      val newPrice = 200

      createAuction(auctionHouseActor, probe)(defaultAuction)

      auctionHouseActor ! UpdateAuction(
        defaultAuction.item, Some(newPrice), None, None
      )
      probe.expectMsg(AuctionActor.Update(Some(newPrice), None, None))
      probe.reply(AuctionUpdated(
        defaultAuction.copy(startingPrice = newPrice)
      ))
      expectMsg(AuctionUpdated(defaultAuction.copy(startingPrice = newPrice)))
    }

    "forward an AuctionActor.Join message when it receive a " +
      "JoinAuction message" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)
      val testBidder = AuctionActor.Bidder("testBidder")

      createAuction(auctionHouseActor, probe)(defaultAuction)

      auctionHouseActor ! JoinAuction(defaultAuction.item, testBidder.name)
      probe.expectMsg(AuctionActor.Join(testBidder.name))
      probe.reply(AuctionJoined(testBidder))
      expectMsg(AuctionJoined(testBidder))
    }

    "forward a AuctionActor.PlaceBid message when it receive a " +
      "PlaceBid message" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)
      val testBidder = AuctionActor.Bidder("testBidder")
      val testBid = AuctionActor.Bid(testBidder.name, 100)

      createAuction(auctionHouseActor, probe)(defaultAuction)

      auctionHouseActor ! PlaceBid(
        defaultAuction.item,
        testBid.bidderName,
        testBid.value
      )
      probe.expectMsg(AuctionActor.PlaceBid(testBid.bidderName, testBid.value))
      probe.reply(BidPlaced(testBid))
      expectMsg(BidPlaced(testBid))
    }

    "send back an AuctionAlreadyExist message when it receive a " +
      "CreateAuction message with an item field already used" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)

      createAuction(auctionHouseActor, probe)(defaultAuction)

      auctionHouseActor ! CreateAuction(
        defaultAuction.item,
        defaultAuction.startingPrice,
        defaultAuction.startDate,
        defaultAuction.endDate
      )
      probe.expectNoMessage()
      expectMsg(AuctionAlreadyExist(defaultAuction.item))
    }

    "send back an NegativeStartingPrice message when it receive a " +
      "CreateAuction message with a negative starting price" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)
      val negativeStartingPrice = -1

      auctionHouseActor ! CreateAuction(
        defaultAuction.item,
        negativeStartingPrice,
        defaultAuction.startDate,
        defaultAuction.endDate
      )
      probe.expectNoMessage()
      expectMsg(NegativeStartingPrice(negativeStartingPrice))
    }

    "send back an empty list of auction when it receive a GetAuctions " +
      "message but have no auction" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)

      auctionHouseActor ! GetAuctions
      expectMsg(AuctionsFound(Set()))
    }

    "send back an AuctionNotFound message when it receive a GetAuction " +
      "message, but the provided item doesn't exist" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)

      auctionHouseActor ! GetAuction(defaultAuction.item)
      expectMsg(AuctionNotFound(defaultAuction.item))
    }

    "send back an AuctionNotFound message when it receive an UpdateAuction " +
      "message, but the provided item doesn't exist" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)
      val newPrice = 200

      auctionHouseActor ! UpdateAuction(
        defaultAuction.item, Some(newPrice), None, None
      )
      expectMsg(AuctionNotFound(defaultAuction.item))
    }

    "send back an AuctionNotFound message when it receive a JoinAuction " +
      "message, but the provided item doesn't exist" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)
      val testBidder = AuctionActor.Bidder("testBidder")

      auctionHouseActor ! JoinAuction(defaultAuction.item, testBidder.name)
      expectMsg(AuctionNotFound(defaultAuction.item))
    }

    "send back an AuctionNotFound message when it receive a PlaceBid " +
      "message, but the provided item doesn't exist" in {
      val probe = TestProbe()
      val auctionHouseActor: ActorRef = createWithProbe(probe)
      val testBidder = AuctionActor.Bidder("testBidder")
      val testBid = AuctionActor.Bid(testBidder.name, 100)

      auctionHouseActor ! PlaceBid(
        defaultAuction.item,
        testBid.bidderName,
        testBid.value
      )
      expectMsg(AuctionNotFound(defaultAuction.item))
    }
  }
}