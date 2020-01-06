package portable.akka.extensions

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.github.ghik.silencer.silent

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js.timers

object PortableAkka {

  def setTimeout(duration: FiniteDuration)(body: => Unit)(implicit @silent actorSystem: ActorSystem[_]): Unit = {
    timers.setTimeout(duration)(body)
  }

  def onMessage[Out, Mat](stream: Source[Out, Mat])(f: Out => Unit): Source[Out, Mat] = {
    stream.onMessage(f)
    stream
  }

  def onError[Out, Mat](stream: Source[Out, Mat])(errorHandler: Throwable => Unit): Source[Out, Mat] = {
    stream.onError(errorHandler)
    stream
  }

  def withEffects[Out, Mat](stream: Source[Out, Mat])(messageHandler: Out => Unit, errorHandler: Throwable => Unit): Source[Out, Mat] =
    onError(onMessage(stream)(messageHandler))(errorHandler)
}