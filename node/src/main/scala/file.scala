package com.orchid.node.file

import java.util.UUID
import java.io.{File, InputStream}

trait FileStorageConfig{
  def storageDirectory:String
}

trait FileStorageComponentApi{
  def fileStorageConfig: FileStorageConfig
}

trait FileStorageApi{
  def uploadFile(uuid:UUID, inputStream:InputStream)
}

trait FileStorageComponent extends FileStorageComponentApi{
  lazy val fileStorage: FileStorageApi = new FileStorageApi{
    new File(fileStorageConfig.storageDirectory).mkdir()

    def uploadFile(uuid: UUID, inputStream: InputStream) {

    }
  }
}

