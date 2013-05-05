package com.orchid.table

private[table] class CompoundIterator[T](iterators: Iterator[Iterable[T]]) extends Iterator[T]{
  var current: Iterator[T] = if (iterators.hasNext) iterators.next().iterator else null

  def hasNext: Boolean = if (current == null){
    false
  }else{
    val currentHasNext = current.hasNext
    if (currentHasNext){
      true
    }else if (iterators.hasNext){
      current = iterators.next.iterator
      hasNext
    }else{
      false
    }
  }

  def next(): T = {
    hasNext; current.next()
  }
}

trait Index[T]{
  val function:T=>Iterable[AnyRef]
}

case class SimpleIndex[T] (private val func:T=>AnyRef) extends Index[T]{
  val function: (T) => Iterable[AnyRef] = {x:T=>List(func(x))}
}

case class CollectionIndex[T] (private val func:T=>Iterable[AnyRef]) extends Index[T]{
  val function: (T) => Iterable[AnyRef] = func
}

class Table[T] private(
     indexFunctions:Set[Index[T]],
     indexes:Map[Index[T], Map[AnyRef,Set[T]]]
   ) extends Iterable[T]{

  def hasIndexes = !indexes.isEmpty

  def iterator:Iterator[T] =
    if (!indexes.values.isEmpty){
      val iv = indexes.values.head.valuesIterator
      new CompoundIterator(iv)
    }
    else throw new IllegalStateException("No indexes defined")

  def apply(index:Index[T]) = indexes(index)

  def withIndex(newIndexItem: Index[T])={
    val totalFunc = indexFunctions + newIndexItem
    val newIndex: Map[AnyRef,Set[T]] =
      if (hasIndexes){
        val iter = this.iterator
        var currentIndex:Map[AnyRef, Set[T]] = Map()
        while (iter.hasNext){
            val item = iter.next
            val indexValueForItem = newIndexItem.function(item)
            currentIndex = indexValueForItem.foldLeft(currentIndex){(acc, value)=>
              val oldSet = acc.getOrElse(value, Set[T]())
              acc + (value-> (oldSet + item))
            }
        }
        currentIndex
      } else Map()
    val oldIndex=indexes.get(newIndexItem).getOrElse(Map())
    val totalIndex = oldIndex ++ newIndex.map{ case (k,v) => k -> (v ++ oldIndex.getOrElse(k,Set())) }
    new Table(totalFunc, indexes + (newIndexItem->totalIndex))
  }

  def +(item:T)={
    val indexValues = indexFunctions.map(indexFunction=>(indexFunction->indexFunction.function(item)))
    val newIndexes = for ((symbol, indexValues)<-indexValues) yield {
      var currentMap = indexes.getOrElse(symbol, Map())
      for(indexValue<-indexValues){
        currentMap += (indexValue->(currentMap.getOrElse(indexValue,Set())+item))
      }
      (symbol-> currentMap)
    }
    val newIndexesMap = newIndexes.toMap
    new Table(indexFunctions, newIndexesMap)
  }

  def -(item:T)={
    val indexValues = indexFunctions.map(indexFunction=>(indexFunction->indexFunction.function(item)))
    val newIndexes = for ((symbol, indexValues)<-indexValues) yield {
      var currentMap = indexes.getOrElse(symbol, Map())
      for(indexValue<-indexValues){
        val newSet = (currentMap.getOrElse(indexValue,Set())-item)
        if (newSet.isEmpty){
          currentMap -= indexValue
        }else{
          currentMap += (indexValue->newSet)
        }
      }
      (symbol-> currentMap)
    }
    val newIndexesMap = newIndexes.toMap
    new Table(indexFunctions, newIndexesMap)
  }
}

object Table{
  def apply[T]() = new Table[T](Set(), Map())
}

