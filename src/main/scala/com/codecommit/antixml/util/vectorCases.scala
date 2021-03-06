package com.codecommit.antixml.util

import scala.collection.IndexedSeqLike
import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.{IndexedSeq, VectorBuilder}
import scala.collection.mutable.{ArrayBuffer, Builder}

private[antixml] sealed trait VectorCase[+A] extends IndexedSeq[A] with IndexedSeqLike[A, VectorCase[A]] {
  
  override protected[this] def newBuilder: Builder[A, VectorCase[A]] = VectorCase.newBuilder[A]
  
  def +:[B >: A](b: B): VectorCase[B]
  def :+[B >: A](b: B): VectorCase[B]
  
  def apply(index: Int): A
  def updated[B >: A](index: Int, b: B): VectorCase[B]
  
  def ++[B >: A](that: VectorCase[B]): VectorCase[B]
  
  def toVector: Vector[A]
}

private[antixml] object VectorCase {
  implicit def canBuildFrom[A]: CanBuildFrom[Traversable[_], A, VectorCase[A]] = new CanBuildFrom[Traversable[_], A, VectorCase[A]] {
    def apply() = newBuilder[A]
    def apply(from: Traversable[_]) = newBuilder[A]
  }
  
  def empty[A] = VectorN[A](Vector.empty)
  
  def newBuilder[A]: Builder[A, VectorCase[A]] = new Builder[A, VectorCase[A]] { this: Builder[A, VectorCase[A]] =>
    val small = new ArrayBuffer[A](4)
    var builder: VectorBuilder[A] = _
    
    def +=(a: A) = {
      if (builder == null) {
        small += a
        
        if (small.length > 4) {
          builder = new VectorBuilder[A]
          builder ++= small
        }
      } else {
        builder += a
      }
      this
    }
    
    override def ++=(seq: TraversableOnce[A]) = {
      if (builder == null) {
        small ++= seq
        
        if (small.length > 4) {
          builder = new VectorBuilder[A]
          builder ++= small
        }
      } else {
        builder ++= seq
      }
      this
    }
    
    def result() = {
      if (builder == null) {
        small.length match {
          case 0 => Vector0
          case 1 => Vector1(small(0))
          case 2 => Vector2(small(0), small(1))
          case 3 => Vector3(small(0), small(1), small(2))
          case 4 => Vector4(small(0), small(1), small(2), small(3))
        }
      } else {
        VectorN(builder.result())
      }
    }
    
    def clear() = this
  }
  
  def apply[A](as: A*) = fromSeq(as)
  
  def fromSeq[A](seq: Seq[A]) = seq match {
    case c: VectorCase[A] => c
    case _ if seq.lengthCompare(0) <= 0 => Vector0
    case _ if seq.lengthCompare(1) <= 0 => Vector1(seq(0))
    case _ if seq.lengthCompare(2) <= 0 => Vector2(seq(0), seq(1))
    case _ if seq.lengthCompare(3) <= 0 => Vector3(seq(0), seq(1), seq(2))
    case _ if seq.lengthCompare(4) <= 0 => Vector4(seq(0), seq(1), seq(2), seq(3))
    case vec: Vector[A] => VectorN(vec)
    case _ => VectorN(Vector(seq: _*))
  }
}

private[antixml] case object Vector0 extends VectorCase[Nothing] {
  def length = 0
  
  def +:[B](b: B) = Vector1(b)
  def :+[B](b: B) = Vector1(b)
  
  def apply(index: Int) = error("Apply on empty vector")
  def updated[B](index: Int, b: B) = error("Updated on empty vector")
  
  def ++[B](that: VectorCase[B]) = that
  
  def toVector = Vector()
}

private[antixml] case class Vector1[+A](_1: A) extends VectorCase[A] {
  def length = 1
  
  def +:[B >: A](b: B) = Vector2(b, _1)
  def :+[B >: A](b: B) = Vector2(_1, b)
  
  def apply(index: Int) = {
    if (index == 0) 
      _1 
    else
      throw new IndexOutOfBoundsException
  }
  
  def updated[B >: A](index: Int, b: B) = {
    if (index == 0)
      Vector1(b)
    else
      throw new IndexOutOfBoundsException
  }
  
  def ++[B >: A](that: VectorCase[B]) = that match {
    case Vector0 => this
    case Vector1(_2) => Vector2(_1, _2)
    case Vector2(_2, _3) => Vector3(_1, _2, _3)
    case Vector3(_2, _3, _4) => Vector4(_1, _2, _3, _4)
    case _: Vector4[B] | _: VectorN[B] =>
      VectorN(_1 +: that.toVector)
  }
  
  // TODO more methods
  
  def toVector = Vector(_1)
}

private[antixml] case class Vector2[+A](_1: A, _2: A) extends VectorCase[A] {
  def length = 2
  
  def +:[B >: A](b: B) = Vector3(b, _1, _2)
  def :+[B >: A](b: B) = Vector3(_1, _2, b)
  
  def apply(index: Int) = index match {
    case 0 => _1
    case 1 => _2
    case _ => throw new IndexOutOfBoundsException
  }
  
  def updated[B >: A](index: Int, b: B) = index match {
    case 0 => Vector2(b, _2)
    case 1 => Vector2(_1, b)
    case _ => throw new IndexOutOfBoundsException
  }
  
  def ++[B >: A](that: VectorCase[B]) = that match {
    case Vector0 => this
    case Vector1(_3) => Vector3(_1, _2, _3)
    case Vector2(_3, _4) => Vector4(_1, _2, _3, _4)
    case _: Vector3[B] | _: Vector4[B] | _: VectorN[B] =>
      VectorN(Vector(_1, _2) ++ that.toVector)
  }
  
  // TODO more methods
  
  def toVector = Vector(_1, _2)
}

private[antixml] case class Vector3[+A](_1: A, _2: A, _3: A) extends VectorCase[A] {
  def length = 3
  
  def +:[B >: A](b: B) = Vector4(b, _1, _2, _3)
  def :+[B >: A](b: B) = Vector4(_1, _2, _3, b)
  
  def apply(index: Int) = index match {
    case 0 => _1
    case 1 => _2
    case 2 => _3
    case _ => throw new IndexOutOfBoundsException
  }
  
  def updated[B >: A](index: Int, b: B) = index match {
    case 0 => Vector3(b, _2, _3)
    case 1 => Vector3(_1, b, _3)
    case 2 => Vector3(_1, _2, b)
    case _ => throw new IndexOutOfBoundsException
  }
  
  def ++[B >: A](that: VectorCase[B]) = that match {
    case Vector0 => this
    case Vector1(_4) => Vector4(_1, _2, _3, _4)
    case _: Vector2[B] | _: Vector3[B] | _: Vector4[B] | _: VectorN[B] =>
      VectorN(Vector(_1, _2, _3) ++ that.toVector)
  }
  
  // TODO more methods
  
  def toVector = Vector(_1, _2, _3)
}

private[antixml] case class Vector4[+A](_1: A, _2: A, _3: A, _4: A) extends VectorCase[A] {
  def length = 4
  
  def +:[B >: A](b: B) = VectorN(Vector(b, _1, _2, _3, _4))
  def :+[B >: A](b: B) = VectorN(Vector(_1, _2, _3, _4, b))
  
  def apply(index: Int) = index match {
    case 0 => _1
    case 1 => _2
    case 2 => _3
    case 3 => _4
    case _ => throw new IndexOutOfBoundsException
  }
  
  def updated[B >: A](index: Int, b: B) = index match {
    case 0 => Vector4(b, _2, _3, _4)
    case 1 => Vector4(_1, b, _3, _4)
    case 2 => Vector4(_1, _2, b, _4)
    case 3 => Vector4(_1, _2, _3, b)
    case _ => throw new IndexOutOfBoundsException
  }
  
  def ++[B >: A](that: VectorCase[B]) = that match {
    case Vector0 => this
    case _: Vector1[B] | _: Vector2[B] | _: Vector3[B] | _: Vector4[B] | _: VectorN[B] =>
      VectorN(Vector(_1, _2, _3, _4) ++ that.toVector)
  }
  
  // TODO more methods
  
  def toVector = Vector(_1, _2, _3, _4)
}

private[antixml] case class VectorN[+A](vector: Vector[A]) extends VectorCase[A] {
  def length = vector.length
  
  def +:[B >:A](b: B) = VectorN(b +: vector)
  def :+[B >:A](b: B) = VectorN(vector :+ b)
  
  def apply(index: Int) = vector(index)
  def updated[B >: A](index: Int, b: B) = VectorN(vector.updated(index, b))
  
  def ++[B >: A](that: VectorCase[B]) = VectorN(vector ++ that.toVector)
  
  override def drop(n: Int) = (vector.length - n) match {
    case x if x <= 0 => Vector0
    case 1 => Vector1(vector(vector.length - 1))
    case 2 => Vector2(vector(vector.length - 2), vector(vector.length - 1))
    case 3 => Vector3(vector(vector.length - 3), vector(vector.length - 2), vector(vector.length - 1))
    case 4 => Vector4(vector(vector.length - 4), vector(vector.length - 3), vector(vector.length - 2), vector(vector.length - 1))
    case _ => VectorN(vector drop n)
  }
  
  override def dropRight(n: Int) = (vector.length - n) match {
    case x if x <= 0 => Vector0
    case 1 => Vector1(vector(0))
    case 2 => Vector2(vector(0), vector(1))
    case 3 => Vector3(vector(0), vector(1), vector(2))
    case 4 => Vector4(vector(0), vector(1), vector(2), vector(3))
    case _ => VectorN(vector dropRight n)
  }
  
  override def init = VectorN(vector.init)    // TODO
  
  override def slice(from: Int, until: Int) = VectorN(vector.slice(from, until))    // TODO
  
  override def splitAt(n: Int) = {
    val (left, right) = vector splitAt n
    (VectorN(left), VectorN(right))   // TODO
  }
  
  override def tail = VectorN(vector.tail)    // TODO
  
  override def take(n: Int) = VectorN(vector take n)    // TODO
  
  override def takeRight(n: Int) = VectorN(vector takeRight n)    // TODO
  
  def toVector = vector
}
