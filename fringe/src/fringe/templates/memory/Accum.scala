package fringe.templates.memory

import chisel3._
import scala.collection.mutable._
import chisel3.util.Mux1H
import fringe.templates.math._
import fringe.Ledger._
import fringe.templates.counters.SingleCounter
import fringe.utils.{log2Up, getRetimed}
import fringe.utils.implicits._
import fringe.Ledger

import scala.math.log

sealed abstract class Accum
object Accum {
  case object Add extends Accum
  case object Mul extends Accum
  case object Min extends Accum
  case object Max extends Accum
}

class FixFMAAccum(
    val numWriters:   Int,
    val cycleLatency: Double,
    val fmaLatency:   Double,
    val s: Boolean,
    val d: Int,
    val f: Int,
    init: Double)
  extends Module {

  def this(tup: (Int, Double, Double, Boolean, Int, Int, Double)) = this(tup._1, tup._2, tup._3, tup._4, tup._5, tup._6, tup._7)

  val cw = log2Up(cycleLatency) + 2
  val initBits = (init*scala.math.pow(2,f)).toLong.S((d+f).W).asUInt
  val io = IO(new FixFMAAccumBundle(numWriters, d, f))

  val activeIn1 = Mux1H(io.input.map(_.enable), io.input.map(_.input1))
  val activeIn2 = Mux1H(io.input.map(_.enable), io.input.map(_.input2))
  val activeEn  = io.input.map(_.enable).reduce{_|_}
  val activeLast = Mux1H(io.input.map(_.enable), io.input.map(_.last))
  val activeReset = io.reset
  val activeFirst = io.input.map(_.first).reduce{_|_} | io.reset

  val fixin1 = Wire(new FixedPoint(s,d,f))
  fixin1.r := activeIn1
  val fixin2 = Wire(new FixedPoint(s,d,f))
  fixin2.r := activeIn2

  // Use log2Down to be consistent with latency model that truncates
  val drain_latency = (log(cycleLatency)/log(2)).toInt

  val laneCtr = Module(new SingleCounter(1, Some(0), Some(cycleLatency.toInt), Some(1), false, cw))
  laneCtr.io <> DontCare
  laneCtr.io.input.enable := activeEn
  laneCtr.io.input.reset := activeReset | activeLast.D(drain_latency + fmaLatency)
  laneCtr.io.setup.saturate := false.B

  val firstRound = Module(new SRFF())
  firstRound.io.input.set := activeFirst & !laneCtr.io.output.done
  firstRound.io.input.asyn_reset := false.B
  firstRound.io.input.reset := laneCtr.io.output.done | activeReset
  val isFirstRound = firstRound.io.output

  val drainState = Module(new SRFF())
  drainState.io.input.set := activeLast
  drainState.io.input.asyn_reset := false.B
  drainState.io.input.reset := activeLast.D(drain_latency + fmaLatency)
  val isDrainState = drainState.io.output

  val dispatchLane = laneCtr.io.output.count(0).asUInt
  val accums = Array.tabulate(cycleLatency.toInt){i => (Module(new FF(d+f)), i.U(cw.W))}
  accums.foreach{case (acc, lane) => 
    acc.io <> DontCare
    val fixadd = Wire(new FixedPoint(s,d,f))
    fixadd.r := Mux(isFirstRound, 0.U, acc.io.rPort(0).output(0))
    val result = Wire(new FixedPoint(s,d,f))
    result.r := Math.fma(fixin1, fixin2, fixadd, Some(fmaLatency), true.B, "").r
    acc.io.rPort <> DontCare
    acc.io.wPort(0).data(0) := result.r
    acc.io.wPort(0).en(0) := getRetimed(activeEn & dispatchLane === lane, fmaLatency.toInt)
    acc.io.wPort(0).reset := activeReset | activeLast.D(drain_latency + fmaLatency)
    acc.io.wPort(0).init := initBits
  }

  io.output := getRetimed(accums.map(_._1.io.rPort(0).output(0)).reduce{_+_}, drain_latency, isDrainState, (init*scala.math.pow(2,f)).toLong).r // TODO: Please build tree and retime appropriately
}


class FixOpAccum(val t: Accum, val numWriters: Int, val cycleLatency: Double, val opLatency: Double, val s: Boolean, val d: Int, val f: Int, init: Double) extends Module {
  def this(tup: (Accum, Int, Double, Double, Boolean, Int, Int, Double)) = this(tup._1, tup._2, tup._3, tup._4, tup._5, tup._6, tup._7, tup._8)

  val cw = log2Up(cycleLatency) + 2
  val initBits = (init*scala.math.pow(2,f)).toLong.S((d+f).W).asUInt
  val io = IO(new FixOpAccumBundle(numWriters, d, f))

  val activeIn1 = Mux1H(io.input.map(_.enable), io.input.map(_.input1))
  val activeEn  = io.input.map(_.enable).reduce{_|_}
  val activeLast = Mux1H(io.input.map(_.enable), io.input.map(_.last))
  val activeReset = io.reset
  val activeFirst = io.input.map(_.first).reduce{_|_} | io.reset

  val fixin1 = Wire(new FixedPoint(s,d,f))
  fixin1.r := activeIn1

  // Use log2Down to be consistent with latency model that truncates
  val drain_latency = ((log(cycleLatency)/log(2)).toInt * opLatency).toInt

  val laneCtr = Module(new SingleCounter(1, Some(0), Some(cycleLatency.toInt), Some(1), false, cw))
  laneCtr.io <> DontCare
  laneCtr.io.input.enable := activeEn
  laneCtr.io.input.reset := activeReset | activeLast.D(drain_latency + opLatency)
  laneCtr.io.setup.saturate := false.B

  val firstRound = Module(new SRFF())
  firstRound.io.input.set := activeFirst & !laneCtr.io.output.done
  firstRound.io.input.asyn_reset := false.B
  firstRound.io.input.reset := laneCtr.io.output.done | activeReset
  val isFirstRound = firstRound.io.output

  val drainState = Module(new SRFF())
  drainState.io.input.set := activeLast
  drainState.io.input.asyn_reset := false.B
  drainState.io.input.reset := activeLast.D(drain_latency + opLatency)
  val isDrainState = drainState.io.output

  val dispatchLane = laneCtr.io.output.count(0).asUInt
  val accums = Array.tabulate(cycleLatency.toInt){i => (Module(new FF(d+f)), i.U(cw.W))}
  accums.foreach{case (acc, lane) => 
    val fixadd = Wire(new FixedPoint(s,d,f))
    fixadd.r := acc.io.rPort(0).output(0)
    val result = Wire(new FixedPoint(s,d,f))
    t match {
      case Accum.Add => result.r := Mux(isFirstRound.D(opLatency.toInt), getRetimed(fixin1.r, opLatency.toInt), getRetimed(fixin1 + fixadd, opLatency.toInt).r)
      case Accum.Mul => result.r := Mux(isFirstRound.D(opLatency.toInt), getRetimed(fixin1.r, opLatency.toInt), Math.mul(fixin1,fixadd, Some(opLatency), true.B, Truncate, Wrapping, "").r)
      case Accum.Min => result.r := Mux(isFirstRound.D(opLatency.toInt), getRetimed(fixin1.r, opLatency.toInt), getRetimed(Mux(fixin1 < fixadd, fixin1, fixadd).r, opLatency.toInt))
      case Accum.Max => result.r := Mux(isFirstRound.D(opLatency.toInt), getRetimed(fixin1.r, opLatency.toInt), getRetimed(Mux(fixin1 > fixadd, fixin1, fixadd).r, opLatency.toInt))
    }
    acc.io.wPort <> DontCare
    acc.io.rPort <> DontCare
    acc.io.reset := false.B
    acc.io.wPort(0).data(0) := result.r
    acc.io.wPort(0).en(0) := getRetimed(activeEn & dispatchLane === lane, opLatency.toInt)
    acc.io.wPort(0).reset := activeReset | activeLast.D(drain_latency + opLatency)
    acc.io.wPort(0).init := initBits
  }

  t match {
    case Accum.Add => io.output := getRetimed(accums.map(_._1.io.rPort(0).output(0)).reduce[UInt]{case (a:UInt,b:UInt) =>
      val t1 = Wire(new FixedPoint(s,d,f))
      val t2 = Wire(new FixedPoint(s,d,f))
      t1.r := a
      t2.r := b
      (t1+t2).r
    }, drain_latency, isDrainState, (init*scala.math.pow(2,f)).toLong)
    case Accum.Mul => io.output := getRetimed(accums.map(_._1.io.rPort(0).output(0)).foldRight[UInt](1.FP(s,d,f).r){case (a:UInt,b:UInt) =>
      val t1 = Wire(new FixedPoint(s,d,f))
      val t2 = Wire(new FixedPoint(s,d,f))
      t1.r := a
      t2.r := b
      Math.mul(t1, t2, Some(0), true.B, Truncate, Wrapping, "").r
    }, drain_latency, isDrainState, init = (init*scala.math.pow(2,f)).toLong)
    case Accum.Min => io.output := getRetimed(accums.map(_._1.io.rPort(0).output(0)).reduce[UInt]{case (a:UInt,b:UInt) =>
      val t1 = Wire(new FixedPoint(s,d,f))
      val t2 = Wire(new FixedPoint(s,d,f))
      t1.r := a
      t2.r := b
      Mux(t1 < t2, t1.r, t2.r)
    }, drain_latency, isDrainState, (init*scala.math.pow(2,f)).toLong)
    case Accum.Max => io.output := getRetimed(accums.map(_._1.io.rPort(0).output(0)).reduce[UInt]{case (a:UInt,b:UInt) =>
      val t1 = Wire(new FixedPoint(s,d,f))
      val t2 = Wire(new FixedPoint(s,d,f))
      t1.r := a
      t2.r := b
      Mux(t1 > t2, t1.r,t2.r)
      // Utils.getRetimed(Mux(t1 > t2, t1.r,t2.r), opLatency.toInt)
    }, drain_latency, isDrainState, (init*scala.math.pow(2,f)).toLong)
  }
}
