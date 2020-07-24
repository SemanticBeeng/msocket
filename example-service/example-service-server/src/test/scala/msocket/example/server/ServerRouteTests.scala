package msocket.example.server

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest, WSProbe}
import akka.stream.scaladsl.Source
import akka.testkit.TestDuration
import csw.example.api.protocol.ExampleRequest.{GetNumbers, Hello, HelloStream}
import csw.example.api.protocol.{ExampleCodecs, ExampleRequest}
import msocket.api.ContentType
import msocket.api.ContentType.Json
import msocket.impl.post.{ClientHttpCodecs, FetchEvent}
import msocket.impl.ws.WebsocketExtensions.WebsocketEncoding
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import akka.actor.typed.scaladsl.adapter._

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class ServerRouteTests
    extends AnyFunSuite
    with ScalatestRouteTest
    with Matchers
    with ExampleCodecs
    with ClientHttpCodecs
    with BeforeAndAfterAll {

  override def clientContentType: ContentType = Json

  private val wiring = new ServerWiring {
    override implicit lazy val actorSystem: ActorSystem[_] = system.toTyped
  }

  protected override def afterAll(): Unit = {
    wiring.actorSystem.terminate()
    Await.result(wiring.actorSystem.whenTerminated, 10.seconds)
  }

  test("websocket") {
    val wsClient = WSProbe()

    WS(s"/websocket-endpoint", wsClient.flow) ~> wiring.exampleServer.routesWithCors ~> check {
      wsClient.sendMessage(Json.strictMessage(GetNumbers(3): ExampleRequest))
      isWebSocketUpgrade shouldBe true
      wsClient.expectMessage() shouldBe TextMessage.Strict("3")
      wsClient.expectMessage() shouldBe TextMessage.Strict("6")
      wsClient.expectMessage() shouldBe TextMessage.Strict("9")
      wsClient.expectMessage() shouldBe TextMessage.Strict("12")
      wsClient.expectMessage() shouldBe TextMessage.Strict("15")
    }
  }

  test("http-streaming") {
    implicit val timeout: RouteTestTimeout = RouteTestTimeout(10.seconds.dilated)
    Post("/post-streaming-endpoint", HelloStream("mushtaq"): ExampleRequest) ~> wiring.exampleServer.routesWithCors ~> check {
      responseAs[Source[FetchEvent, NotUsed]].take(3).runForeach(println)
    }
  }

  test("simple-post") {
    implicit val timeout: RouteTestTimeout = RouteTestTimeout(10.seconds.dilated)
    Post("/post-endpoint", Hello("mushtaq"): ExampleRequest) ~> wiring.exampleServer.routesWithCors ~> check {
      println(status)
      println(response)
    }
  }

}
