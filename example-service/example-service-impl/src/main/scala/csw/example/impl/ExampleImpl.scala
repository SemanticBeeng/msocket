package csw.example.impl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import csw.example.api.ExampleApi

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class ExampleImpl(implicit actorSystem: ActorSystem) extends ExampleApi {
  import actorSystem.dispatcher

  override def hello(name: String): Future[String] = {
    Future.successful(s"Hello $name")
  }

  override def square(number: Int): Future[Int] = {
    akka.pattern.after(3.minutes, actorSystem.scheduler) {
      Future.successful(number * number)
    }
  }

  //////////////

  override def helloStream(name: String): Source[String, NotUsed] = {
    Source
      .tick(10.millis, 10.millis, ())
      .scan(0)((acc, _) => acc + 1)
      .map(x => s"hello \n $name again $x")
      .mapMaterializedValue(_ => NotUsed)
  }

  override def getNumbers(divisibleBy: Int): Source[Int, Future[Option[String]]] = {
    if (divisibleBy == 0) {
      Source.empty[Int].mapMaterializedValue(_ => Future.successful(Some("divide by zero error")))
    } else {
      Source
        .fromIterator(() => Iterator.from(1).filter(_ % divisibleBy == 0))
        .throttle(1, 1.second)
        .mapMaterializedValue(_ => Future.successful(None))
    }
  }
}
