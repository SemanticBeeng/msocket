package csw.example.api.protocol

import csw.example.api.protocol.ExampleProtocol.{ExampleRequest, ExampleStreamRequest}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs
import msocket.api.ErrorProtocol

/**
 * Borer Codecs
 */
object ExampleCodecs extends ExampleCodecs
trait ExampleCodecs {

  /** Codec for message protocol */
  implicit lazy val exampleRequestResponseCodec: Codec[ExampleRequest]     = CompactMapBasedCodecs.deriveAllCodecs
  implicit lazy val exampleRequestStreamCodec: Codec[ExampleStreamRequest] = CompactMapBasedCodecs.deriveAllCodecs

  /** Codec for error protocol */
  implicit lazy val exampleErrorCodec: Codec[ExampleError] = CompactMapBasedCodecs.deriveAllCodecs

  /**
   * Bind error protocol to the message protocol
   * This is required so that domain specific errors are correctly
   * serialized without error types being known at compile time
   */
  implicit lazy val exampleRequestResponseErrorProtocol: ErrorProtocol[ExampleRequest]     =
    ErrorProtocol.bind[ExampleRequest, ExampleError]
  implicit lazy val exampleRequestStreamErrorProtocol: ErrorProtocol[ExampleStreamRequest] =
    ErrorProtocol.bind[ExampleStreamRequest, ExampleError]
}