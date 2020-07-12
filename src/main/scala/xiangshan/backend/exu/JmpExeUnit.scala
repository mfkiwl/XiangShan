package xiangshan.backend.exu

import chisel3._
import xiangshan.{ExuOutput, FuType, XSConfig}
import xiangshan.backend.fu.{CSR, Jump}

class JmpExeUnit extends Exu(Exu.jmpExeUnitCfg) {

  val jmp = Module(new Jump)

  jmp.io.out.ready := io.out.ready
  jmp.io.dmem <> DontCare
  jmp.io.scommit := DontCare
  jmp.io.redirect := io.redirect

  lazy val p = XSConfig(
    FPGAPlatform = false
  )

  val csr = Module(new CSR()(p))
  csr.io.cfIn := io.in.bits.uop.cf
  csr.io.fpu_csr := DontCare
  csr.io.instrValid := DontCare
  csr.io.imemMMU := DontCare
  csr.io.dmemMMU := DontCare
  csr.io.out.ready := io.out.ready
  csr.io.in.bits.src3 := DontCare
  val csrOut = csr.access(
    valid = io.in.valid && io.in.bits.uop.ctrl.fuType===FuType.csr,
    src1 = io.in.bits.src1,
    src2 = io.in.bits.src2,
    func = io.in.bits.uop.ctrl.fuOpType
  )

  val csrExuOut = Wire(new ExuOutput)
  csrExuOut.uop := io.in.bits.uop
  csrExuOut.data := csrOut
  csrExuOut.redirectValid := false.B
  csrExuOut.redirect := DontCare
  csrExuOut.debug := DontCare

  jmp.io.in.bits := io.in.bits
  jmp.io.in.valid := io.in.valid && io.in.bits.uop.ctrl.fuType===FuType.jmp

  io.in.ready := io.out.ready
  io.out.bits := Mux(jmp.io.in.valid, jmp.io.out.bits, csrExuOut)
  io.out.valid := io.in.valid
}