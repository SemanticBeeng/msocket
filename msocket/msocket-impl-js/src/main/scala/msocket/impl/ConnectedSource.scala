package msocket.impl

import akka.stream.scaladsl.Source
import io.bullet.borer.Decoder
import msocket.api.Subscription

class ConnectedSource[Res: Decoder] extends Source[Res, Subscription] {
  private var onMessage: Res => Unit           = x => ()
  private var subscription: Subscription       = () => ()
  override val materializedValue: Subscription = subscription

  def start[Req](req: Req, connector: JsTransport[Req]): ConnectedSource[Res] = {
    subscription = connector.requestStream(req, onMessage)
    this
  }

  def foreach(f: Res => Unit): Unit = {
    onMessage = f
  }
}
