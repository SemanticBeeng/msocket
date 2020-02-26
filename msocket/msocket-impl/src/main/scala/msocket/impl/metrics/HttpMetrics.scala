package msocket.impl.metrics

import akka.http.scaladsl.server.Directives.{as, entity}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.HostDirectives.extractHost
import io.bullet.borer.Decoder
import io.prometheus.client.Counter
import msocket.api.{ErrorProtocol, Labelled}
import msocket.impl.post.ServerHttpCodecs._

object HttpMetrics extends HttpMetrics

trait HttpMetrics extends Metrics {

  private[metrics] val httpCounterMetricName = "http_requests_total"
  def httpCounter(labelNames: List[String]): Counter =
    counter(
      metricName = httpCounterMetricName,
      help = "Total http requests",
      labelNames = labelNames
    )

  def httpMetrics[Req: Decoder: ErrorProtocol](metricsEnabled: Boolean, counter: => Counter)(
    handle: Req => Route
  )(implicit labelGen: Req => Labelled[Req]): Route =
    extractHost { address =>
      if (metricsEnabled)
        entity(as[Req]) { req =>
          val labels = labelValues(labelGen(req), address)
          counter.labels(labels: _*).inc()
          handle(req)
        } else entity(as[Req])(handle)
    }
}
