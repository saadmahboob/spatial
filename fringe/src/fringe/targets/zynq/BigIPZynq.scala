package fringe.targets.zynq

import chisel3._
import chisel3.util._
import fringe.globals
import fringe.utils.getRetimed
import fringe.templates.math._
import fringe.utils.implicits._
import fringe.BigIP

class BigIPZynq extends BigIP with ZynqBlackBoxes {
  def divide(dividend: UInt, divisor: UInt, latency: Int, flow: Bool, myName: String): UInt = getConst(divisor) match {
    case Some(bigNum) => getRetimed(dividend / bigNum.U, latency, flow)
    case None =>
      val m = Module(new Divider(dividend.getWidth, divisor.getWidth, false, latency)); m.suggestName(myName)
      m.io.dividend := dividend
      m.io.divisor := divisor
      m.io.flow := flow
      m.io.out
  }

  def divide(dividend: SInt, divisor: SInt, latency: Int, flow: Bool, myName: String): SInt = getConst(divisor) match {
    case Some(bigNum) => getRetimed(dividend / bigNum.S, latency, flow)
    case None =>
      val m = Module(new Divider(dividend.getWidth, divisor.getWidth, true, latency)); m.suggestName(myName)
      m.io.dividend := dividend.asUInt
      m.io.divisor := divisor.asUInt
      m.io.flow := flow
      m.io.out.asSInt
  }

  def mod(dividend: UInt, divisor: UInt, latency: Int, flow: Bool, myName: String): UInt = getConst(divisor) match {
    case Some(bigNum) => getRetimed(dividend % bigNum.U, latency, flow)
    case None =>
      val m = Module(new Modulo(dividend.getWidth, divisor.getWidth, false, latency)); m.suggestName(myName)
      m.io.dividend := dividend
      m.io.divisor := divisor
      m.io.flow := flow
      m.io.out
  }

  def mod(dividend: SInt, divisor: SInt, latency: Int, flow: Bool, myName: String): SInt = getConst(divisor) match {
    case Some(bigNum) => getRetimed(dividend % bigNum.S, latency, flow)
    case None =>
      val m = Module(new Modulo(dividend.getWidth, divisor.getWidth, true, latency)); m.suggestName(myName)
      m.io.dividend := dividend.asUInt
      m.io.divisor := divisor.asUInt
      m.io.flow := flow
      m.io.out.asSInt
  }

  def multiply(a: UInt, b: UInt, latency: Int, flow: Bool, myName: String): UInt = {
    val aconst = getConst(a)
    val bconst = getConst(b)
    if (aconst.isDefined | bconst.isDefined) { // Constant optimization
      if (aconst.isDefined && bconst.isDefined) { (aconst.get * bconst.get).U }
      else {
        val const = if (aconst.isDefined) aconst.get else bconst.get
        val other = if (aconst.isDefined) b else a
        getRetimed(const.U * other, latency, flow)
      }
    } else {
      val m = Module(new Multiplier(a.getWidth, b.getWidth, math.max(a.getWidth, b.getWidth), false, latency)); m.suggestName(myName)
      m.io.a := a
      m.io.b := b
      m.io.flow := flow
      m.io.out
    }
  }

  def multiply(a: SInt, b: SInt, latency: Int, flow: Bool, myName: String): SInt = {
    val aconst = getConst(a)
    val bconst = getConst(b)
    if (aconst.isDefined | bconst.isDefined) { // Constant optimization
      if (aconst.isDefined && bconst.isDefined) { (aconst.get * bconst.get).S }
      else {
        val const = if (aconst.isDefined) aconst.get else bconst.get
        val other = if (aconst.isDefined) b else a
        getRetimed(const.S * other, latency, flow)
      }
    } else {
      val m = Module(new Multiplier(a.getWidth, b.getWidth, math.max(a.getWidth, b.getWidth), true, latency)); m.suggestName(myName)
      m.io.a := a.asUInt
      m.io.b := b.asUInt
      m.io.flow := flow
      m.io.out.asSInt
    }
  }

  override def sqrt(a: UInt, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new SquareRooter(a.getWidth, false, latency)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.out
  }

  def fadd(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FAdd(mw, e, latency)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }

  def fsub(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FSub(mw, e, latency)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fmul(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FMul(mw, e, latency)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fdiv(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FDiv(mw, e, latency)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def flt(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FLt(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def feq(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FEq(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fgt(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FGt(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fge(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FGe(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fle(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FLe(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fne(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FNe(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  override def fabs(a: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FAbs(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.out
  }
  override def fexp(a: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FExp(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.out
  }
  override def fln(a: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FLog(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.out
  }
  override def fsqrt(a: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FSqrt(mw, e)); m.suggestName(myName)
    m.io.a := a
    m.io.out
  }
  override def frec(a: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FRec(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.out
  }
  override def frsqrt(a: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FRSqrt(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.out
  }
  override def ffma(a: UInt, b: UInt, c: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FFma(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.c := c
    m.io.out
  }
  override def fix2flt(a: UInt, sign: Boolean, dec: Int, frac: Int, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new Fix2Float(dec, frac, mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.out
  }
  override def fix2fix(src: UInt, s1: Boolean, d1: Int, f1: Int, s2: Boolean, d2: Int, f2: Int, latency: Int, flow: Bool, rounding: RoundingMode, saturating: OverflowMode, myName: String): UInt = {
    if (src.litArg.isEmpty) {
      val fix2fixBox = Module(new fix2fixBox(s1, d1, f1, s2, d2, f2, rounding, saturating, latency, myName))
      fix2fixBox.io.a := src
      fix2fixBox.io.expect_neg := false.B
      fix2fixBox.io.expect_pos := false.B
      fix2fixBox.io.flow := flow
      fix2fixBox.io.b
    }
    // Likely that there are mistakes here
    else {
      val f_gain = f2 - f1
      val d_gain = d2 - d1
      val salt = rounding match {
        case Unbiased if f_gain < 0 => BigInt((scala.math.random * (1 << -f_gain).toDouble).toLong)
        case _ => BigInt(0)
      }
      val newlit = saturating match {
        case Wrapping =>
          if (f_gain < 0 & d_gain >= 0)       (src.litArg.get.num + salt) >> -f_gain
          else if (f_gain >= 0 & d_gain >= 0) (src.litArg.get.num) << f_gain
          else if (f_gain >= 0 & d_gain < 0)  ((src.litArg.get.num + salt) >> -f_gain) & BigInt((1 << (d2 + f2 + 1)) - 1)
          else ((src.litArg.get.num) << f_gain) & BigInt((1 << (d2 + f2 + 1)) -1)
        case Saturating =>
          if (src.litArg.get.num > BigInt((1 << (d2 + f2 + 1))-1)) BigInt((1 << (d2 + f2 + 1))-1)
          else {
            if (f_gain < 0 & d_gain >= 0)       (src.litArg.get.num + salt) >> -f_gain
            else if (f_gain >= 0 & d_gain >= 0) (src.litArg.get.num) << f_gain
            else if (f_gain >= 0 & d_gain < 0)  ((src.litArg.get.num + salt) >> -f_gain) & BigInt((1 << (d2 + f2 + 1)) - 1)
            else ((src.litArg.get.num) << f_gain) & BigInt((1 << (d2 + f2 + 1)) -1)
          }
      }
      getRetimed(newlit.S((d2 + f2 + 1).W).asUInt.apply(d2 + f2 - 1, 0), latency, flow)
    }
  }

  override def flt2fix(a: UInt, mw: Int, e: Int, sign: Boolean, dec: Int, frac: Int, latency: Int, flow: Bool, rounding: RoundingMode, saturating: OverflowMode, myName: String): UInt = {
    val m = Module(new Float2Fix(mw, e, sign, dec, frac)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.out
  }
  override def flt2flt(a: UInt, mwa: Int, ea: Int, mw_out: Int, e_out: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new Float2Float(mwa, ea, mw_out, e_out)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.out
  }
  override def fltaccum(a: UInt, en: Bool, last: Bool, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FAccum(mw, e)); m.suggestName(myName)
    m.io.flow := flow
    m.io.a := a
    m.io.en := en
    m.io.last := last
    m.io.out
  }

}
