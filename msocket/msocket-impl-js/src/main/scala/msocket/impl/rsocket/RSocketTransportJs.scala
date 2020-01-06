package msocket.impl.rsocket

import io.bullet.borer.{Decoder, Encoder}
import msocket.api.Encoding.JsonText
import msocket.api.{ErrorProtocol, Subscription}
import msocket.impl.JsTransport
import typings.rsocketDashCore.Anon_DataMimeType
import typings.rsocketDashCore.rSocketClientMod.ClientConfig
import typings.rsocketDashCore.rsocketDashCoreMod.RSocketClient
import typings.rsocketDashFlowable.singleMod.{CancelCallback, IFutureSubscriber}
import typings.rsocketDashTypes.reactiveSocketTypesMod.{Payload, ReactiveSocket}
import typings.rsocketDashTypes.reactiveStreamTypesMod.{ISubscriber, ISubscription}
import typings.rsocketDashWebsocketDashClient.rSocketWebSocketClientMod.ClientOptions
import typings.rsocketDashWebsocketDashClient.rsocketDashWebsocketDashClientMod.{default => RSocketWebSocketClient}
import typings.std

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetIntervalHandle
import scala.util.{Failure, Success, Try}

class RSocketTransportJs[Req: Encoder: ErrorProtocol](uri: String)(implicit ec: ExecutionContext, streamingDelay: FiniteDuration)
    extends JsTransport[Req] {

  private val client: RSocketClient[String, String] = new RSocketClient(
    ClientConfig(
      setup = Anon_DataMimeType(
        dataMimeType = "application/json",
        keepAlive = 60000,
        lifetime = 1800000,
        metadataMimeType = "application/json"
      ),
      transport = new RSocketWebSocketClient(ClientOptions(url = uri))
    )
  )

  private def subscriber[T](p: Promise[T]): IFutureSubscriber[Try[T]] = new IFutureSubscriber[Try[T]] {
    def onComplete(value: Try[T]): Unit           = p.tryComplete(value)
    def onError(error: std.Error): Unit           = p.tryFailure(new RuntimeException(error.toString))
    def onSubscribe(cancel: CancelCallback): Unit = println("inside onSubscribe")
  }

  private val socketPromise: Promise[ReactiveSocket[String, String]] = Promise()
  client.connect().map(Try(_)).subscribe(PartialOf(subscriber(socketPromise)))

  override def requestResponse[Res: Decoder: Encoder](req: Req): Future[Res] = {
    val responsePromise: Promise[Res] = Promise()
    socketPromise.future.foreach { socket =>
      socket
        .requestResponse(Payload(JsonText.encode(req)))
        .map(payload => Try(JsonText.decodeWithError(payload.data.get)))
        .subscribe(PartialOf(subscriber(responsePromise)))
    }

    responsePromise.future
  }

  override def requestStream[Res: Decoder: Encoder](request: Req, onMessage: Res => Unit, onError: Throwable => Unit): Subscription = {
    val subscriptionPromise: Promise[ISubscription] = Promise()
    val _onError                                    = onError

    val pullStreamHandle: Future[SetIntervalHandle] = subscriptionPromise.future.map { subscription =>
      timers.setInterval(streamingDelay) {
        subscription.request(1)
      }
    }

    def cancelSubscription(): Unit = {
      subscriptionPromise.future.foreach(_.cancel())
      pullStreamHandle.foreach(timers.clearInterval)
    }

    val subscriber: ISubscriber[Try[Res]] = new ISubscriber[Try[Res]] {
      override def onComplete(): Unit              = println("stream completed")
      override def onError(error: std.Error): Unit = _onError(new RuntimeException(error.toString))
      override def onNext(value: Try[Res]): Unit = value match {
        case Failure(exception) =>
          _onError(exception)
          cancelSubscription()
        case Success(value) => onMessage(value)
      }
      override def onSubscribe(subscription: ISubscription): Unit = subscriptionPromise.trySuccess(subscription)
    }

    socketPromise.future.foreach { socket =>
      socket
        .requestStream(Payload(JsonText.encode(request)))
        .map(payload => Try(JsonText.decodeWithError(payload.data.get)))
        .subscribe(PartialOf(subscriber))
    }

    () => cancelSubscription()
  }

  def shutdown(): Unit = client.close()
}
