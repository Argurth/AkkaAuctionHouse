import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import api.{RequestTimeout, RestRoutes}

import scala.concurrent.Future

/**
  * Created by argurth on 26/11/18.
  */
object API extends App with RestRoutes with RequestTimeout {
  implicit val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val requestTimeout = requestTimeoutFromConfig(config)
  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  implicit val materializer = ActorMaterializer()
  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(routes, host, port)

  bindingFuture map { serverBinding =>
    println(s"RestApi bound to ${serverBinding.localAddress} ")
  } recover {
    case ex: Exception =>
      println(ex, "Failed to bind to {}:{}!", host, port)
      system.terminate()
  }
}
