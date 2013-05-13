package com.orchid.node.restclient

import org.apache.http.impl.nio.client.DefaultHttpAsyncClient
import concurrent.{Promise, Future}
import org.apache.http.client.methods.{HttpPut, HttpUriRequest, HttpGet}
import org.apache.http.concurrent.FutureCallback
import org.apache.http.HttpResponse
import org.apache.commons.io.{IOUtils, FileUtils}
import java.io.{InputStream, StringWriter}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import reflect.ClassTag
import org.apache.http.entity.InputStreamEntity

case class RestRequestCancelledException() extends RuntimeException

trait RestClientComponentApi{
  def restClient:RestClientApi
}

trait RestClientApi{
  def getJSON[T:ClassTag](uri:String):Future[T]

  def putInputStream(uri:String,inputStream:InputStream):Future[Unit]
}

trait RestClientComponent extends RestClientComponentApi{
  lazy val restClient: RestClientApi = new RestClientApi {
    val client = new DefaultHttpAsyncClient()
    client.start()

    val objectMapper = new ObjectMapper
    objectMapper.registerModule(DefaultScalaModule)

    private def runQuery[T:ClassTag](request:HttpUriRequest)(success: HttpResponse=>T)={
      val promise = Promise[T]()

      client.execute(request, new FutureCallback[HttpResponse]{
        def completed(result: HttpResponse) {
          val value = success(result)
          promise.success(value)
        }

        def failed(ex: Exception) {
          promise.failure(ex)
        }

        def cancelled() {
          promise.failure(RestRequestCancelledException())
        }
      })

      promise.future
    }

    def putInputStream(uri: String, inputStream:InputStream): Future[Unit] = {
      val put = new HttpPut(uri)
      put.setEntity(new InputStreamEntity(inputStream, inputStream.available()))
      runQuery[Unit](put){result=>
        Unit
      }
    }

    def getJSON[T:ClassTag](uri:String):Future[T]={
      runQuery[T](new HttpGet(uri)){result=>
        val writer = new StringWriter();
        IOUtils.copy(result.getEntity.getContent, writer, "UTF-8");
        val string = writer.toString
        objectMapper.reader(implicitly[ClassTag[T]].runtimeClass).readValue[T](string)
      }
    }
  }
}
