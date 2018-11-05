package com.banno.twittersampleendpoint

import cats.effect._
import org.http4s.Method.GET
import org.http4s._
import org.http4s.dsl.impl._

class StaticContentRoute[F[_] : Effect] {
  private val swaggerUiDir = "/api-doc"

  def fetchResource(path: String, req: Request[F]): F[Response[F]] = {
    StaticFile.fromResource(path, Some(req)).getOrElseF(Response.notFoundFor[F](req))
  }

  def routes(): HttpService[F] =
    HttpService {
      case req@GET -> Root / "css" / _ =>
        fetchResource(swaggerUiDir + req.pathInfo, req)
      case req@GET -> Root / "images" / _ =>
        fetchResource(swaggerUiDir + req.pathInfo, req)
      case req@GET -> Root / "lib" / _ =>
        fetchResource(swaggerUiDir + req.pathInfo, req)
      case req@GET -> Root =>
        fetchResource(swaggerUiDir + "/index.html", req)
      case req@GET -> Root / "swagger-ui.js" =>
        fetchResource(swaggerUiDir + "/swagger-ui.min.js", req)
      case req@GET -> Root / "swagger.json" =>
        fetchResource("/swagger.json", req)
    }

}
