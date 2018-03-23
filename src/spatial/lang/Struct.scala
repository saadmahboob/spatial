package spatial.lang

import forge.tags._
import core._
import spatial.node._

abstract class Struct[T:Struct](implicit ev: T <:< Struct[T]) extends Top[T] with Ref[Nothing,T] {
  override val __isPrimitive = false
  @rig def field[A:Type](name: String): A = Struct.field[T,A](me, name)
}

object Struct {
  @rig def apply[S:Struct](elems: (String,Sym[_])*): S = stage(SimpleStruct[S](elems))
  @rig def field[S:Struct,A:Type](struct: S, name: String): A = stage(FieldApply[S,A](struct,name))
  @rig def field_update[S:Struct,A:Type](struct: S, name: String, data: A): Void = stage(FieldUpdate[S,A](struct,name,data))
}