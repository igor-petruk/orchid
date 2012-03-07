package com.orchid.utils

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

    def next() = {
      var value = null.asInstanceOf[T];
      do {
        index += 1
        value = values(index)
      } while (value != null)
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

