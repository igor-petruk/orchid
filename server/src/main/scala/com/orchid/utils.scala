package com.orchid.util

import com.orchid.messages.generated.Messages
import com.orchid.messages.generated.Messages.GeneralFileInfo
import com.orchid.UUIDConversions
import com.orchid.tree.{Node, FilesystemError}

trait ErrorConversions{
  implicit def error2msg(errorItem:FilesystemError)={
    val error = Messages.Error.newBuilder
    error.setErrorType(errorItem.errorType)
    for (description<-errorItem.description){
      error.setDescription(description)
    }
    error
  }
}

trait FileUtils extends UUIDConversions{
  def splitPath(path:String)={
    val pathAndDir = path.splitAt(path.lastIndexOf("/"))
    (pathAndDir._1,
      if (pathAndDir._2.startsWith("/")) pathAndDir._2.substring(1)
      else pathAndDir._2)
  }

  def buildFileInfo(fullName:String, node:Node)={
    val infoBuilder = GeneralFileInfo.newBuilder();
    infoBuilder.setFileId(node.id)
    infoBuilder.setFileName(fullName)
    infoBuilder.setFileSize(node.size)
    infoBuilder.setIsDirectory(node.isDir)
    infoBuilder.build()
  }
}
