package org.bau.emu.riscv;

/*

Important compiler features
if, else, while, maybe return
+, -, <<, >>, ==, !=, <=, |, &, []
*10: n = (n<<1) + (n<<3) + token[i] - '0';
getchar(), putchar(), "\xff"

RISC V instruction set:

31           24  19  14     11          6
funct7       rs2 rs1 funct3 rd          opcode R-type
imm[11:0]        rs1 funct3 rd          opcode I-type
imm[11:5]    rs2 rs1 funct3 imm[4:0]    opcode S-type
imm[12|10:5] rs2 rs1 funct3 imm[4:1|11] opcode B-type *
imm[31:12]                  rd          opcode U-type
imm[20|10:1|11|19:12]       rd          opcode J-type calls

                         FMT   Opcode  funct3  funct7
add    ADD                 R   110011  0x0     0x00        rd = rs1 + rs2
sub    SUB                 R   110011  0x0     0x20        rd = rs1 - rs2
xor    XOR                 R   110011  0x4     0x00        rd = rs1 ^ rs2
or     OR                  R   110011  0x6     0x00        rd = rs1 | rs2
and    AND                 R   110011  0x7     0x00        rd = rs1 & rs2
sll    Shift Left Logical  R   110011  0x1     0x00        rd = rs1 << rs2
srl    Shift Right Logical R   110011  0x5     0x00        rd = rs1 >> rs2
sra    Shift Right Arith*  R   110011  0x5     0x20        rd = rs1 >> rs2 msb-extends
slt    Set Less Than       R   110011  0x2     0x00        rd = (rs1 < rs2) ? 1 : 0
sltu   Set Less Than (U)   R   110011  0x3     0x00        rd = (rs1 < rs2) ? 1 : 0 msb-extends
addi   ADD Immediate       I   10011   0x0                 rd = rs1 + imm
xori   XOR Immediate       I   10011   0x4                 rd = rs1 ^ imm
ori    OR Immediate        I   10011   0x6                 rd = rs1 | imm
andi   AND Immediate       I   10011   0x7                 rd = rs1 & imm
slli   Shift               I   10011   0x1 imm[5:11]=0x00  rd = rs1 << imm[0:4]
srli   Shift               I   10011   0x5 imm[5:11]=0x00  rd = rs1 >> imm[0:4]
srai   Shift               I   10011   0x5 imm[5:11]=0x20  rd = rs1 >> imm[0:4] msb-extends
slti   Set Less Than       I   10011   0x2                 rd = (rs1 < imm) ? 1 : 0
sltiu  Set Less Than (U)   I   10011   0x3                 rd = (rs1 < imm) ? 1 : 0 zero-extends
lb     Load Byte           I   11      0x0                 rd = M[rs1+imm][0:7]
lh     Load Half           I   11      0x1                 rd = M[rs1+imm][0:15]
lw     Load Word           I   11      0x2                 rd = M[rs1+imm][0:31]
lbu    Load Byte (U)       I   11      0x4                 rd = M[rs1+imm][0:7] zero-extends
lhu    Load Half (U)       I   11      0x5                 rd = M[rs1+imm][0:15] zero-extends
sb     Store Byte          S   100011  0x0                 M[rs1+imm][0:7] = rs2[0:7]
sh     Store Half          S   100011  0x1                 M[rs1+imm][0:15] = rs2[0:15]
sw     Store Word          S   100011  0x2                 M[rs1+imm][0:31] = rs2[0:31]
beq    Branch ==           B   1100011 0x0                 if(rs1 == rs2) PC += imm
bne    Branch !=           B   1100011 0x1                 if(rs1 != rs2) PC += imm
blt    Branch <            B   1100011 0x4                 if(rs1 <  rs2) PC += imm
bge    Branch >=           B   1100011 0x5                 if(rs1 >= rs2) PC += imm
bltu   Branch < (U)        B   1100011 0x6                 if(rs1 <  rs2) PC += imm zero-extends
bgeu   Branch >= (U)       B   1100011 0x7                 if(rs1 >= rs2) PC += imm zero-extends
jal    Jump And Link       J   1101111                     rd = PC+4; PC += imm
jalr   Jump And Link Reg   I   1100111 0x0                 rd = PC+4; PC = rs1 + imm  (return; call function pointer)
lui    Load Upper Imm      U   110111                      rd = imm << 12
auipc  Add Upper Imm to PC U   10111                       rd = PC + (imm << 12)
ecall  Environment Call    I   1110011 0x0 imm=0x0         Transfer control to OS
ebreak Environment Break   I   1110011 0x0 imm=0x1         Transfer control to debugger

important:

add    ADD                 R   110011  0x0     0x00        rd = rs1 + rs2
sub    SUB                 R   110011  0x0     0x20        rd = rs1 - rs2
xor    XOR                 R   110011  0x4     0x00        rd = rs1 ^ rs2
or     OR                  R   110011  0x6     0x00        rd = rs1 | rs2
and    AND                 R   110011  0x7     0x00        rd = rs1 & rs2
slli   Shift               I   10011   0x1 imm[5:11]=0x00  rd = rs1 << imm[0:4]
srli   Shift               I   10011   0x5 imm[5:11]=0x00  rd = rs1 >> imm[0:4]
srai   Shift               I   10011   0x5 imm[5:11]=0x20  rd = rs1 >> imm[0:4] msb-extends
lw     Load Word           I   11      0x2                 rd = M[rs1+imm][0:31]
ecall  Environment Call    I   1110011 0x0 imm=0x0         Transfer control to OS
jal    Jump And Link       J   1101111                     rd = PC+4; PC += imm
beq    Branch ==           B   1100011 0x0                 if(rs1 == rs2) PC += imm
bne    Branch !=           B   1100011 0x1                 if(rs1 != rs2) PC += imm
blt    Branch <            B   1100011 0x4                 if(rs1 <  rs2) PC += imm
bge    Branch >=           B   1100011 0x5                 if(rs1 >= rs2) PC += imm

JALR x0, ra, 0

void emitLoad32(int constant) {
    int upper = (constant >> 12);
    int lower = (constant & 0xFFF);
    if ((lower & 0x800) != 0) {
        upper += 1; // Compensate for sign extension
    }
    emit("LUI", rd, upper);
    emit("ADDI", rd, rs1, lower);
}
addi   ADD Immediate       I   10011   0x0                 rd = rs1 + imm
lui    Load Upper Imm      U   110111                      rd = imm << 12



*/

public class Emulator {


//    private final int[] memory;
//    // x0: always 0
//    // x1: ra return address
//    // x2: sp stack pointer
//    // x5-x7, x28-x31: temporaries
//    private final int[] regs = new int[32];
//    // program counter
//    private int pc;
//    private int outPos;
//    private final byte[] output;
//
//    public Emulator(int size) {
//        this.memory = new int[size];
//        this.output = new byte[size];
//    }
//
//
//    private void step() {
//        int instr = memory[pc];
//        int opcode = instr & 0x7f;
//        int rd = (instr >> 7) & 0x1f;
//        // distinguish ADD vs LW
//        int funct3 = (instr >> 12) & 0x07;
//        int rs1 = (instr >> 15) & 0x1f;
//        int rs2 = (instr >> 20) & 0x1f;
//        int imm = instr >> 12;
//
//
//        switch (opcode) {
//        case OP_I_TYPE: // ADDI, etc.
//            if (funct3 == 0x0) { // ADDI instruction logic
//                if (rd != 0) {
//                    regs[rd] = regs[rs1] + imm;
//                }
//            }
//            break;
//        case OP_LOAD: { // LW
//            // In I-type, addr = rs1 + immediate
//            int addr = regs[rs1] + imm;
//            if (rd != 0) {
//                regs[rd] = memory[addr];
//            }
//            break;
//        }
//        case OP_STORE: { // SW
//            int addr = regs[rs1] + imm;
//            memory[addr] = regs[rs2];
//            break;
//        }
//        case OP_BRANCH: { // BEQ, BNE
//         // Note: Branch immediates (B-type) are much more complex.
//            // For a simple emulator, start by mastering I-type first!
//            if (funct3 == 0x0) { // BEQ
//                if (regs[rs1] == regs[rs2]) {
//                    // In a real emulator, you'd decode the B-type immediate here
//                    // For now, let's assume PC jumping is handled by your logic
//                }
//            }
//            break;
////            if (regs[rs1] == regs[rs2]) {
////                pc += imm;
////            }
////            break;
//        }
//        case OP_SYSTEM: {
//            // // ECALL
//            // 1. Check which syscall is being requested (look in register a7/x17)
//            int syscall_num = regs[17];
//            if (syscall_num == 64) { // If it's the 'write' syscall
//                int fd = regs[10]; // File descriptor from a0
//                int addr = regs[11]; // String address from a1
//                int len = regs[12]; // Length from a2
//                if (fd == 1) { // If the target is stdout
//                    // We "trap" to the host C code and use printf!
//                    // We read the bytes from our emulator's memory array.
//                    output[outPos] = (byte) memory[addr];
//                }
//            }
//        }
//        }
//        pc++;
//    }
//
//    public void run() {
//
//    }

}
