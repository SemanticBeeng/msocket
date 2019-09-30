package msocket.api.utils

import com.github.ghik.silencer.silent
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryEncoder
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryDecoder
import io.bullet.borer.derivation.MapBasedCodecs._
import io.bullet.borer.{Decoder, Encoder}

sealed trait Result[S, E] {
  def toEither: Either[E, S] = this match {
    case Result.Success(value) => Right(value)
    case Result.Error(value)   => Left(value)
  }
}

object Result {
  case class Success[S, E](value: S) extends Result[S, E]
  case class Error[S, E](value: E)   extends Result[S, E]

  def fromEither[E, S](either: Either[E, S]): Result[S, E] = either match {
    case Left(value)  => Error(value)
    case Right(value) => Success(value)
  }

  implicit def resultDec[E: Decoder, S: Decoder]: Decoder[Result[S, E]] = {
    @silent implicit lazy val errorEnc: Decoder[Error[S, E]]     = deriveUnaryDecoder[Error[S, E]]
    @silent implicit lazy val successEnc: Decoder[Success[S, E]] = deriveUnaryDecoder[Success[S, E]]
    deriveDecoder[Result[S, E]]
  }

  implicit def resultEnc[E: Encoder, S: Encoder]: Encoder[Result[S, E]] = {
    @silent implicit lazy val errorEnc: Encoder[Error[S, E]]     = deriveUnaryEncoder[Error[S, E]]
    @silent implicit lazy val successEnc: Encoder[Success[S, E]] = deriveUnaryEncoder[Success[S, E]]
    deriveEncoder[Result[S, E]]
  }
}
