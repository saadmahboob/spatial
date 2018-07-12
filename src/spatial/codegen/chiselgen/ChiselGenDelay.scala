package spatial.codegen.chiselgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._
import spatial.util.spatialConfig


trait ChiselGenDelay extends ChiselGenCommon {

  // var outMuxMap: Map[Sym[Reg[_]], Int] = Map()
  private var nbufs: List[(Sym[Reg[_]], Int)]  = List()

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    case DelayLine(delay, data) if (!spatialConfig.enableOptimizedReduce || (lhs.reduceType != Some(FixPtFMA))) =>
      if (delay > maxretime) maxretime = delay
      // emit(src"""val $lhs = Utils.delay($data, $size)""")

      data.rhs match {
        case Def.Const(_) => 
        case Def.Param(_,_) => 
        case _ =>
          alphaconv_register(src"$lhs")
          emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
          lhs.tp match {
            case a:Vec[_] => emitt(src"(0 until ${a.width}).foreach{i => ${lhs}(i).r := ${DL(src"${data}(i).r", delay)}}")
            case _ =>        emitt(src"""${lhs}.r := ${DL(src"${data}.r", delay, false)}""")
          }
      }

  case DelayLine(delay, data) if (spatialConfig.enableOptimizedReduce && (lhs.reduceType == Some(FixPtFMA))) =>
	case _ => super.gen(lhs, rhs)
  }


}