package csw.example.api.protocol

/**
 * Transport agnostic message protocol is defined as a Scala ADT
 * For each API method, there needs to be a matching message in this ADT
 * For example, Hello(name) is the message for hello(name) method in the API
 */
sealed trait ExampleRequest

object ExampleRequest {
  case class Hello(name: String)          extends ExampleRequest
  case class HelloStream(name: String)    extends ExampleRequest
  case class Square(number: Int)          extends ExampleRequest
  case class GetNumbers(divisibleBy: Int) extends ExampleRequest
  case object RandomBag                   extends ExampleRequest
  case object RandomBagStream             extends ExampleRequest
}
