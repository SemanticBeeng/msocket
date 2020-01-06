package msocket.example.client

import akka.actor.typed.ActorSystem
import com.github.ghik.silencer.silent
import csw.example.api.client.ExampleClient
import csw.example.api.protocol.{ExampleCodecs, ExampleRequest}
import msocket.impl.post.HttpPostTransportJs
import msocket.impl.rsocket.RSocketTransportJs
import msocket.impl.sse.SseTransportJs
import msocket.impl.ws.WebsocketTransportJs

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationLong, FiniteDuration}

object ClientMainJs extends ExampleCodecs {

  def main(args: Array[String]): Unit = {
    implicit val streamingDelay: FiniteDuration = 1.second
    implicit val actorSystem: ActorSystem[Any]  = new ActorSystem

    lazy val httpPostTransport  = new HttpPostTransportJs[ExampleRequest]("http://localhost:5000/post-endpoint").logRequestResponse()
    lazy val sseTransport       = new SseTransportJs[ExampleRequest]("http://localhost:5000/sse-endpoint").logRequestResponse()
    lazy val websocketTransport = new WebsocketTransportJs[ExampleRequest]("ws://localhost:5000/websocket-endpoint").logRequestResponse()
    lazy val rSocketTransport   = new RSocketTransportJs[ExampleRequest]("ws://localhost:7000").logRequestResponse()

    val exampleClient = new ExampleClient(rSocketTransport)
    new ClientAppJs(exampleClient).testRun()
  }
}
