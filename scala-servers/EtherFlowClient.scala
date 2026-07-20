// EtherFlowClient.scala
// EtherFlow HTTP Client for Scala — sttp + circe
//
// A strongly-typed, Future-based HTTP client for Scala 3 that mirrors the
// EtherFlow Java/Kotlin builder API. Uses sttp for transport and circe for JSON.
//
// Usage:
//   val client = EtherFlowClient.builder
//     .baseUrl("https://api.example.com")
//     .retry(3)
//     .build
//
//   val user: Future[User] = client.get[User]("/users/1")
//   val created: Future[User] = client.post[User, NewUser]("/users", newUser)

package io.etherflow.scala

import sttp.client4.*
import sttp.client4.circe.*
import sttp.model.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*

import scala.concurrent.*
import scala.concurrent.duration.*
import scala.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────────────────────────────────────

final case class EtherFlowError(
  message: String,
  statusCode: Option[Int] = None,
  responseBody: Option[String] = None,
  cause: Option[Throwable] = None
) extends Exception(message, cause.orNull)

// ─────────────────────────────────────────────────────────────────────────────
// Config
// ─────────────────────────────────────────────────────────────────────────────

final case class EtherFlowConfig(
  baseUrl: String                        = "",
  timeout: Duration                      = 10.seconds,
  maxRetries: Int                        = 3,
  retryDelay: Duration                   = 200.milliseconds,
  defaultHeaders: Map[String, String]    = Map.empty
)

// ─────────────────────────────────────────────────────────────────────────────
// Safe Result (mirrors EtherFlow Java's Mono<Result<T>>)
// ─────────────────────────────────────────────────────────────────────────────

enum EtherFlowResult[+T]:
  case Success(data: T)
  case Failure(error: EtherFlowError)

// ─────────────────────────────────────────────────────────────────────────────
// Client
// ─────────────────────────────────────────────────────────────────────────────

/** EtherFlow HTTP client for Scala 3. Mirrors the Java/Kotlin EtherFlowClient builder API. */
final class EtherFlowClient private (config: EtherFlowConfig)(using ExecutionContext):

  private val backend: SttpBackend[Future, Any] =
    HttpClientFutureBackend()

  private val baseUri: Uri =
    uri"${config.baseUrl.stripSuffix("/")}"

  // ── Builder companion ─────────────────────────────────────────────────────

  object EtherFlowClient:
    def builder(using ec: ExecutionContext): Builder = Builder()

    def create(baseUrl: String)(using ec: ExecutionContext): EtherFlowClient =
      new EtherFlowClient(EtherFlowConfig(baseUrl = baseUrl))

  // ── Public API ────────────────────────────────────────────────────────────

  /** GET — deserialise JSON into T using circe auto-derivation. */
  def get[T: Decoder](path: String, headers: Map[String, String] = Map.empty): Future[T] =
    execute[T](Method.GET, path, body = None, headers)

  /** GET returning a list of T. */
  def getList[T: Decoder](path: String, headers: Map[String, String] = Map.empty): Future[List[T]] =
    execute[List[T]](Method.GET, path, body = None, headers)

  /** POST — serialise body to JSON via circe, deserialise response to T. */
  def post[T: Decoder, B: Encoder](path: String, body: B, headers: Map[String, String] = Map.empty): Future[T] =
    execute[T](Method.POST, path, body = Some(body.asJson.noSpaces), headers)

  /** PUT — serialise body to JSON, deserialise response to T. */
  def put[T: Decoder, B: Encoder](path: String, body: B, headers: Map[String, String] = Map.empty): Future[T] =
    execute[T](Method.PUT, path, body = Some(body.asJson.noSpaces), headers)

  /** PATCH. */
  def patch[T: Decoder, B: Encoder](path: String, body: B, headers: Map[String, String] = Map.empty): Future[T] =
    execute[T](Method.PATCH, path, body = Some(body.asJson.noSpaces), headers)

  /** DELETE. */
  def delete[T: Decoder](path: String, headers: Map[String, String] = Map.empty): Future[T] =
    execute[T](Method.DELETE, path, body = None, headers)

  /** Safe GET — returns EtherFlowResult, never fails the Future. */
  def getResult[T: Decoder](path: String): Future[EtherFlowResult[T]] =
    get[T](path).transform {
      case Success(v) => Success(EtherFlowResult.Success(v))
      case Failure(e: EtherFlowError) => Success(EtherFlowResult.Failure(e))
      case Failure(e) => Success(EtherFlowResult.Failure(EtherFlowError(e.getMessage, cause = Some(e))))
    }

  /** Health check — calls /health on the configured base URL. */
  def checkHealth(): Future[Map[String, Json]] =
    get[Map[String, Json]]("/health").recover { _ =>
      Map("status" -> Json.fromString("DOWN"), "error" -> Json.fromString("Health check failed"))
    }

  def close(): Unit = backend.close()

  // ── Internal ──────────────────────────────────────────────────────────────

  private def resolveUri(path: String): Uri =
    if path.startsWith("http") then uri"$path"
    else uri"$baseUri$path"

  private def execute[T: Decoder](
    method: Method,
    path: String,
    body: Option[String],
    extra: Map[String, String],
    attempt: Int = 0
  ): Future[T] =
    val uri     = resolveUri(path)
    val headers = (config.defaultHeaders ++ extra ++ Map(
      "Accept"     -> "application/json",
      "User-Agent" -> "EtherFlow-Scala-Client/1.0"
    )).map { case (k, v) => Header(k, v) }.toList

    val baseReq = basicRequest
      .method(method, uri)
      .headers(headers*)
      .response(asStringAlways)

    val req = body.fold(baseReq) { b =>
      baseReq.body(b).contentType(MediaType.ApplicationJson)
    }

    req.send(backend).flatMap { resp =>
      val code = resp.code.code
      if code >= 200 && code < 300 then
        parser.decode[T](resp.body) match
          case Right(v)  => Future.successful(v)
          case Left(err) => Future.failed(EtherFlowError(s"[EtherFlow] Decode error: ${err.getMessage}"))
      else if code >= 400 && code < 500 then
        Future.failed(EtherFlowError(s"[EtherFlow] HTTP $code", statusCode = Some(code), responseBody = Some(resp.body)))
      else
        val err = EtherFlowError(s"[EtherFlow] HTTP $code", statusCode = Some(code))
        if attempt < config.maxRetries then
          println(s"[EtherFlow.Scala] Retry ${attempt + 1}/${config.maxRetries}: ${err.message}")
          val delay = config.retryDelay.toMillis * math.pow(2, attempt).toLong
          after(delay.milliseconds)(execute[T](method, path, body, extra, attempt + 1))
        else
          Future.failed(EtherFlowError(s"[EtherFlow] Max retries exceeded. Last: ${err.message}"))
    }.recoverWith { case e: EtherFlowError => Future.failed(e)
      case e =>
        if attempt < config.maxRetries then
          val delay = config.retryDelay.toMillis * math.pow(2, attempt).toLong
          after(delay.milliseconds)(execute[T](method, path, body, extra, attempt + 1))
        else
          Future.failed(EtherFlowError(s"[EtherFlow] Network error: ${e.getMessage}", cause = Some(e)))
    }

  private def after[T](delay: FiniteDuration)(f: => Future[T]): Future[T] =
    val p = Promise[T]()
    val timer = new java.util.Timer(true)
    timer.schedule(new java.util.TimerTask { def run(): Unit = p.completeWith(f) }, delay.toMillis)
    p.future

// ─────────────────────────────────────────────────────────────────────────────
// Builder
// ─────────────────────────────────────────────────────────────────────────────

final class Builder(using ec: ExecutionContext):
  private var config = EtherFlowConfig()

  def baseUrl(url: String): this.type         = { config = config.copy(baseUrl = url); this }
  def timeout(d: Duration): this.type         = { config = config.copy(timeout = d); this }
  def retry(n: Int): this.type                = { config = config.copy(maxRetries = n); this }
  def retryDelay(d: Duration): this.type      = { config = config.copy(retryDelay = d); this }
  def header(k: String, v: String): this.type = {
    config = config.copy(defaultHeaders = config.defaultHeaders + (k -> v)); this
  }
  def build: EtherFlowClient = new EtherFlowClient(config)

// ─────────────────────────────────────────────────────────────────────────────
// Example Usage
// ─────────────────────────────────────────────────────────────────────────────

/*
// build.sbt:
// libraryDependencies ++= Seq(
//   "com.softwaremill.sttp.client4" %% "core"         % "4.x",
//   "com.softwaremill.sttp.client4" %% "circe"        % "4.x",
//   "io.circe"                      %% "circe-generic" % "0.14.x"
// )

import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.generic.auto.*

case class User(id: Option[Int], name: String, email: String) derives Encoder, Decoder

@main def run(): Unit =
  given ec: ExecutionContext = ExecutionContext.global
  val client = EtherFlowClient.builder
    .baseUrl("https://jsonplaceholder.typicode.com")
    .retry(3)
    .build

  // 2. GET
  val userFuture = client.get[User]("/users/1")
  userFuture.foreach(u => println(s"User: ${u.name} — ${u.email}"))

  // 3. GET list
  val users = client.getList[User]("/users")
  users.foreach(us => println(s"Total users: ${us.size}"))

  // 4. POST
  val newUser = User(None, "Alice", "alice@example.com")
  val created = client.post[User, User]("/users", newUser)
  created.foreach(u => println(s"Created: ${u.name}"))

  // 5. Safe result
  client.getResult[User]("/users/999").foreach {
    case EtherFlowResult.Success(u)  => println(s"Found: ${u.name}")
    case EtherFlowResult.Failure(e)  => println(s"Error: ${e.message}")
  }

  Thread.sleep(5000) // wait for async
  client.close()
*/
