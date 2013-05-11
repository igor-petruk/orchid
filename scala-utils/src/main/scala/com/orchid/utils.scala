package com.orchid

import annotation.tailrec
import com.orchid.messages.generated.Messages.GeneralFileInfo
import com.google.protobuf.ByteString
import java.util.UUID
import com.fasterxml.uuid.impl.UUIDUtil
import com.orchid.messages.generated.Messages
import xml.PrettyPrinter
import java.io.FileOutputStream
import java.nio.channels.Channels


trait XMLUtils{

  implicit class nodePimp(node:scala.xml.Node){
    def savePretty(filename:String){
      val pp = new PrettyPrinter(80, 2)
      val fos = new FileOutputStream(filename)
      val writer = Channels.newWriter(fos.getChannel(), "UTF-8")

      try {
        writer.write("<?xml version='1.0' encoding='UTF-8'?>\n")
        writer.write(pp.format(node))
      } finally {
        writer.close()
      }
    }
  }
}

trait UUIDConversions{
  implicit def bs2uuid(bs:ByteString):UUID = UUIDUtil.uuid(bs.toByteArray)
  implicit def uuid2bs(uuid:UUID):ByteString = ByteString.copyFrom(UUIDUtil.asByteArray(uuid))
}

object EnumMap {
  def apply[K <: Enum[_], T](implicit k: Manifest[K], t: Manifest[T]) = {
    new EnumMap[K, T]()(k, t).asInstanceOf[Map[K, T]]
  }
}

class EnumMap[K <: Enum[_], +T] private (values: Array[T])
                                       (implicit m: Manifest[K])
  extends Map[K, T] {

  def this()(implicit k: Manifest[K], t: Manifest[T]) =
    this(t.newArray(k.erasure.getEnumConstants.length))(k)

  val notNulls = values.count(_ != null)

  def get(key: K) = {
    val value = values(key.ordinal())
    if (value != null) Some(value) else None
  }

  def iterator = new Iterator[(K, T)] {
    var index: Int = -1;

    def hasNext = index < notNulls - 1

    @tailrec
    private def nextNonNullIndex(index:Int):Int=
      if (values(index)!=null) index
      else nextNonNullIndex(index+1)

    def next() = {
      val value = values(nextNonNullIndex(index+1));
      val key = m.erasure.getEnumConstants()(index).asInstanceOf[K]
      (key, value)
    }
  }

  def -(key: K) = get(key) match {
    case None => this
    case Some(_) => {
      this.+((key, null))
    }
  }

  def +[B1 >: T](kv: (K, B1)) = {
    val newArray = values.clone()
    newArray(kv._1.ordinal()) = kv._2.asInstanceOf[T]
    new EnumMap(newArray)
  }
}

