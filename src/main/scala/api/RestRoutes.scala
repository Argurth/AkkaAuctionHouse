package api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import actors.AuctionHouseActor
import api.auctionHouse.AuctionHouseRoutes

/**
  * Created by argurth on 26/11/18.
  */
trait RestRoutes extends AuctionHouseRoutes {
  implicit val system: ActorSystem

  override def createAuctionHouse(): ActorRef =
    system.actorOf(AuctionHouseActor.props, AuctionHouseActor.name)

  def routes: Route = auctionHouseAPIRoutes
}
