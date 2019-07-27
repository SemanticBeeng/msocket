package csw.simple.api

sealed trait Protocol

object Protocol {
  sealed trait RequestResponse    extends Protocol
  case class Hello(name: String)  extends RequestResponse
  case class Square(number: Int)  extends RequestResponse
  case class Ping(msg: String)    extends RequestResponse
  case class Publish(number: Int) extends RequestResponse

  sealed trait RequestStream              extends Protocol
  case class GetNames(size: Int)          extends RequestStream
  case class GetNumbers(divisibleBy: Int) extends RequestStream

  sealed trait RequestChannel       extends Protocol
  case class HelloAll(msg: String)  extends RequestChannel
  case class SquareAll(number: Int) extends RequestChannel
}
