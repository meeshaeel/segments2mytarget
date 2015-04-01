package net.facetz.export.mr.mailru.api

import java.io.{File, FileInputStream, IOException}

import argonaut.Argonaut._
import org.apache.commons.io.IOUtils

import scala.util.Try
import scalaj.http._

trait MailRuApiProvider {

  protected def clientId: String

  protected def clientSecret: String

  private[this] val defaultConnectionTimeout = 30 * 1000
  private[this] val defaultReadTimeout = 30 * 1000

  class MailRuRequestHelper(val request: HttpRequest) {
    def addAuthToken(token: String): HttpRequest = auth(request, token)

    def setTimeouts(connectionTimeout: Int = defaultConnectionTimeout, readTimeout: Int = defaultReadTimeout): HttpRequest =
      request.option(HttpOptions.connTimeout(defaultConnectionTimeout))
        .option(HttpOptions.readTimeout(defaultReadTimeout))
  }

  implicit def requestToAuthAndWithLimitsRequest(req: HttpRequest): MailRuRequestHelper = new MailRuRequestHelper(req)

  protected def auth(req: HttpRequest, token: String) = req.header("Authorization", s"Bearer $token")

  protected def getAuthToken: Option[String] = {
    val response: HttpResponse[String] = Http("https://target.mail.ru/api/v2/oauth2/token.json")
      .headers("Content-Type" -> "application/x-www-form-urlencoded")
      .params("grant_type" -> "client_credentials", "client_id" -> clientId, "client_secret" -> clientSecret)
      .setTimeouts()
      .postForm
      .asString

    if (response.isSuccess) {
      response.body.decodeOption[MailRuAuthResponse] match {
        case Some(auth) => Option(auth.access_token)
        case None => None
      }
    } else {
      None
    }
  }

  protected def getRemarketingUsersList(authToken: String): Option[List[RemarketingUserListResponseItem]] = {
    val findUserListsResponse: HttpResponse[String] = Http("https://target.mail.ru/api/v1/remarketing_users_lists.json")
      .addAuthToken(authToken)
      .setTimeouts()
      .asString

    if (findUserListsResponse.isSuccess) {
      findUserListsResponse.body.decodeOption[List[RemarketingUserListResponseItem]]
    } else {
      None
    }
  }

  protected def getRemarketingAuditories(authToken: String): Option[List[RemarketingAuditoryItem]] = {
    val existedRemarketings: HttpResponse[String] = Http("https://target.mail.ru/api/v1/remarketings.json")
      .addAuthToken(authToken)
      .setTimeouts()
      .asString

    existedRemarketings.body.decodeOption[List[RemarketingAuditoryItem]]
  }

  protected def createRemarketingAuditory(authToken: String, request: CreateRemarketingAuditoryRequest): Option[String] = {
    val createResponse: HttpResponse[String] = Http("https://target.mail.ru/api/v1/remarketings.json")
      .postData(request.asJson.toString())
      .addAuthToken(authToken)
      .setTimeouts()
      .asString

    if (createResponse.isSuccess) {
      Option(createResponse.body)
    } else {
      None
    }
  }

  protected def uploadSegmentFile(authToken: String, file: File, name: String): Try[Either[OverTheLimitResponse,
    RemarketingUserListResponseItem]] = {
    Try {
      val is: FileInputStream = new FileInputStream(file)

      val result: Either[OverTheLimitResponse, RemarketingUserListResponseItem] = {
        try {
          val array: Array[Byte] = IOUtils.toByteArray(is)
          val result: HttpResponse[String] =
            Http("https://target.mail.ru/api/v1/remarketing_users_lists.json")
              .addAuthToken(authToken)
              .setTimeouts()
              .postMulti(
                MultiPart("file", name, "application/text", array),
                MultiPart("name", "", "", name),
                MultiPart("type", "", "", "dmp_id"))
              .asString
          val overLimitValidation = result.body.decodeValidation[OverTheLimitResponse]
          val goodItemValidation = result.body.decodeValidation[RemarketingUserListResponseItem]
          if (overLimitValidation.isSuccess) {
            Left(overLimitValidation.getOrElse(null))
          } else if (goodItemValidation.isSuccess) {
            Right(goodItemValidation.getOrElse(null))
          } else {
            throw new RuntimeException("bad response")
          }
        } catch {
          case t: IOException if "Premature EOF".equals(t.getMessage) => Left(null)
        } finally {
          is.close()
        }
      }
      result
    }
  }

}
