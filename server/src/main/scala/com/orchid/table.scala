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

case class Index[T] (function:T=>AnyRef)

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
      if (hasIndexes)
        this.groupBy(newIndexItem.function).mapValues(_.toSet)
      else
        Map()
    val oldIndex=indexes.get(newIndexItem).getOrElse(Map())
    val totalIndex = oldIndex ++ newIndex.map{ case (k,v) => k -> (v ++ oldIndex.getOrElse(k,Set())) }
    new Table(totalFunc, indexes + (newIndexItem->totalIndex))
  }

  def +(item:T)={
    val indexValues = indexFunctions.map(indexFunction=>(indexFunction->indexFunction.function(item)))
    val newIndexes = for ((symbol, value)<-indexValues) yield {
      val oldMap = indexes.getOrElse(symbol, Map())
      val newSet = oldMap.getOrElse(value, Set[T]()) + item
      (symbol-> (oldMap + (value->newSet)))
    }
    val newIndexesMap = newIndexes.toMap
    new Table(indexFunctions, newIndexesMap)
  }

  def -(item:T)={
    val indexValues = indexFunctions.map(indexFunction=>(indexFunction->indexFunction.function(item)))
    val newIndexes = for ((symbol, value)<-indexValues) yield {
      val oldMap = indexes.getOrElse(symbol, Map())
      val newSet = oldMap.getOrElse(value, Set[T]()) - item
      if (newSet.isEmpty){
        (symbol-> (oldMap - value))
      }else{
        (symbol-> (oldMap + (value->newSet)))
      }
    }
    val newIndexesMap = newIndexes.toMap
    new Table(indexFunctions, newIndexesMap)
  }
}

object Table{
  def apply[T]() = new Table[T](Set(), Map())
}

