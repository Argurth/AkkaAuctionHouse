package api

import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.duration._

/**
  * Created by argurth on 23/11/18.
  */
trait RequestTimeout {
  def requestTimeoutFromConfig(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)

    FiniteDuration(d.length, d.unit)
  }
}
