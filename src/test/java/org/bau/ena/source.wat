BeginModule(version: 1)
  BeginTypeSection(20)
    OnTypeCount(4)
    OnFuncType(index: 0, params: [i32, i32, i32, i32], results: [i32])
    OnFuncType(index: 1, params: [], results: [i64])
    OnFuncType(index: 2, params: [i64], results: [])
    OnFuncType(index: 3, params: [], results: [])
  EndTypeSection
  BeginImportSection(68)
    OnImportCount(2)
    OnImport(index: 0, kind: func, module: "wasi_snapshot_preview1", field: "fd_write")
    OnImportFunc(import_index: 0, func_index: 0, sig_index: 0)
    OnImport(index: 1, kind: func, module: "wasi_snapshot_preview1", field: "fd_read")
    OnImportFunc(import_index: 1, func_index: 1, sig_index: 0)
  EndImportSection
  BeginFunctionSection(4)
    OnFunctionCount(3)
    OnFunction(index: 2, sig_index: 1)
    OnFunction(index: 3, sig_index: 2)
    OnFunction(index: 4, sig_index: 3)
  EndFunctionSection
  BeginMemorySection(3)
    OnMemoryCount(1)
    OnMemory(index: 0, initial: 1)
  EndMemorySection
  BeginExportSection(19)
    OnExportCount(2)
    OnExport(index: 0, kind: memory, item_index: 0, name: "memory")
    OnExport(index: 1, kind: func, item_index: 4, name: "_start")
  EndExportSection
  BeginCodeSection(7483)
    OnFunctionBodyCount(3)
    BeginFunctionBody(2, size:30)
    OnLocalDeclCount(0)
    EndLocalDecls
    OnI32ConstExpr(0 (0x0))
    OnI32ConstExpr(0 (0x0))
    OnI32ConstExpr(1 (0x1))
    OnI32ConstExpr(24 (0x18))
    OnCallExpr(func_index: 1)
    OnDropExpr
    OnI32ConstExpr(24 (0x18))
    OnLoadExpr(opcode: "i32.load" (40), memidx: 0, align log2: 2, offset: 0)
    OnConvertExpr("i32.eqz" (69))
    OnIfExpr(sig: i64)
    OnI64ConstExpr(18446744073709551615 (0xffffffffffffffff))
    OnElseExpr
    OnI32ConstExpr(16 (0x10))
    OnLoadExpr(opcode: "i64.load8_u" (49), memidx: 0, align log2: 0, offset: 0)
    OnEndExpr
    OnEndExpr
    EndFunctionBody(2)
    BeginFunctionBody(3, size:20)
    OnLocalDeclCount(0)
    EndLocalDecls
    OnI32ConstExpr(16 (0x10))
    OnLocalGetExpr(index: 0)
    OnStoreExpr(opcode: "i64.store8" (60), memidx: 0, align log2: 0, offset: 0)
    OnI32ConstExpr(1 (0x1))
    OnI32ConstExpr(0 (0x0))
    OnI32ConstExpr(1 (0x1))
    OnI32ConstExpr(28 (0x1c))
    OnCallExpr(func_index: 0)
    OnDropExpr
    OnEndExpr
    EndFunctionBody(3)
    BeginFunctionBody(4, size:7428)
    OnLocalDeclCount(1)
    OnLocalDecl(index: 0, count: 64, type: i64)
    EndLocalDecls
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnI64ConstExpr(16 (0x10))
    OnLocalSetExpr(index: 18)
    OnI64ConstExpr(256 (0x100))
    OnLocalSetExpr(index: 19)
    OnLocalGetExpr(index: 19)
    OnLocalSetExpr(index: 20)
    OnI64ConstExpr(0 (0x0))
    OnLocalSetExpr(index: 21)
    OnBlockExpr(sig: void)
    OnLoopExpr(sig: void)
    OnLocalGetExpr(index: 21)
    OnI64ConstExpr(10000 (0x2710))
    OnCompareExpr("i64.lt_s" (83))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(0 (0x0))
    OnCompareExpr("i64.gt_s" (85))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnConvertExpr("i32.wrap_i64" (167))
    OnConvertExpr("i32.eqz" (69))
    OnBrIfExpr(depth: 1)
    OnLocalGetExpr(index: 21)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 21)
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(32 (0x20))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(10 (0xa))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.or" (132))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnElseExpr
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(36 (0x24))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnI64ConstExpr(0 (0x0))
    OnLocalSetExpr(index: 22)
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnBlockExpr(sig: void)
    OnLoopExpr(sig: void)
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(48 (0x30))
    OnCompareExpr("i64.ge_s" (89))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(57 (0x39))
    OnCompareExpr("i64.le_s" (87))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(97 (0x61))
    OnCompareExpr("i64.ge_s" (89))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(102 (0x66))
    OnCompareExpr("i64.le_s" (87))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnBinaryExpr("i64.or" (132))
    OnConvertExpr("i32.wrap_i64" (167))
    OnConvertExpr("i32.eqz" (69))
    OnBrIfExpr(depth: 1)
    OnLocalGetExpr(index: 16)
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(58 (0x3a))
    OnBinaryExpr("i64.sub" (125))
    OnI64ConstExpr(8 (0x8))
    OnBinaryExpr("i64.shr_u" (136))
    OnI64ConstExpr(48 (0x30))
    OnBinaryExpr("i64.and" (131))
    OnBinaryExpr("i64.sub" (125))
    OnI64ConstExpr(96 (0x60))
    OnLocalGetExpr(index: 16)
    OnBinaryExpr("i64.sub" (125))
    OnI64ConstExpr(8 (0x8))
    OnBinaryExpr("i64.shr_u" (136))
    OnI64ConstExpr(87 (0x57))
    OnBinaryExpr("i64.and" (131))
    OnBinaryExpr("i64.sub" (125))
    OnLocalSetExpr(index: 16)
    OnLocalGetExpr(index: 22)
    OnI64ConstExpr(4 (0x4))
    OnBinaryExpr("i64.shl" (134))
    OnLocalGetExpr(index: 16)
    OnBinaryExpr("i64.or" (132))
    OnLocalSetExpr(index: 22)
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnBrExpr(depth: 0)
    OnEndExpr
    OnEndExpr
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(48 (0x30))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 20)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 22)
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 20)
    OnElseExpr
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(39 (0x27))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnI64ConstExpr(0 (0x0))
    OnLocalSetExpr(index: 22)
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnBlockExpr(sig: void)
    OnLoopExpr(sig: void)
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(39 (0x27))
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnConvertExpr("i32.eqz" (69))
    OnBrIfExpr(depth: 1)
    OnLocalGetExpr(index: 22)
    OnI64ConstExpr(8 (0x8))
    OnBinaryExpr("i64.shl" (134))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(255 (0xff))
    OnBinaryExpr("i64.and" (131))
    OnBinaryExpr("i64.or" (132))
    OnLocalSetExpr(index: 22)
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnBrExpr(depth: 0)
    OnEndExpr
    OnEndExpr
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(48 (0x30))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 20)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 22)
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 20)
    OnElseExpr
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(48 (0x30))
    OnCompareExpr("i64.ge_s" (89))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(57 (0x39))
    OnCompareExpr("i64.le_s" (87))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(48 (0x30))
    OnBinaryExpr("i64.sub" (125))
    OnLocalSetExpr(index: 22)
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnBlockExpr(sig: void)
    OnLoopExpr(sig: void)
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(48 (0x30))
    OnCompareExpr("i64.ge_s" (89))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(57 (0x39))
    OnCompareExpr("i64.le_s" (87))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnConvertExpr("i32.wrap_i64" (167))
    OnConvertExpr("i32.eqz" (69))
    OnBrIfExpr(depth: 1)
    OnLocalGetExpr(index: 22)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnLocalGetExpr(index: 22)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.shl" (134))
    OnBinaryExpr("i64.add" (124))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(48 (0x30))
    OnBinaryExpr("i64.sub" (125))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 22)
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnBrExpr(depth: 0)
    OnEndExpr
    OnEndExpr
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(48 (0x30))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 20)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 22)
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 20)
    OnElseExpr
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(61 (0x3d))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(60 (0x3c))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.or" (132))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(62 (0x3e))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.or" (132))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(33 (0x21))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.or" (132))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 16)
    OnLocalSetExpr(index: 23)
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(61 (0x3d))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(60 (0x3c))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.or" (132))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(62 (0x3e))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.or" (132))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(8 (0x8))
    OnBinaryExpr("i64.shl" (134))
    OnLocalGetExpr(index: 16)
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 23)
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnEndExpr
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 23)
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 20)
    OnElseExpr
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(65 (0x41))
    OnCompareExpr("i64.ge_s" (89))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(122 (0x7a))
    OnCompareExpr("i64.le_s" (87))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnI64ConstExpr(0 (0x0))
    OnLocalSetExpr(index: 24)
    OnBlockExpr(sig: void)
    OnLoopExpr(sig: void)
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(65 (0x41))
    OnCompareExpr("i64.ge_s" (89))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 16)
    OnI64ConstExpr(122 (0x7a))
    OnCompareExpr("i64.le_s" (87))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnConvertExpr("i32.wrap_i64" (167))
    OnConvertExpr("i32.eqz" (69))
    OnBrIfExpr(depth: 1)
    OnLocalGetExpr(index: 24)
    OnI64ConstExpr(8 (0x8))
    OnBinaryExpr("i64.shl" (134))
    OnLocalGetExpr(index: 16)
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 24)
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnBrExpr(depth: 0)
    OnEndExpr
    OnEndExpr
    OnI64ConstExpr(16 (0x10))
    OnLocalSetExpr(index: 25)
    OnBlockExpr(sig: void)
    OnLoopExpr(sig: void)
    OnLocalGetExpr(index: 25)
    OnLocalGetExpr(index: 18)
    OnCompareExpr("i64.lt_s" (83))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 25)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 24)
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnConvertExpr("i32.wrap_i64" (167))
    OnConvertExpr("i32.eqz" (69))
    OnBrIfExpr(depth: 1)
    OnLocalGetExpr(index: 25)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 25)
    OnBrExpr(depth: 0)
    OnEndExpr
    OnEndExpr
    OnLocalGetExpr(index: 25)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 24)
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 25)
    OnLocalGetExpr(index: 18)
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 18)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 18)
    OnEndExpr
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 24)
    OnI64ConstExpr(8 (0x8))
    OnBinaryExpr("i64.shl" (134))
    OnI64ConstExpr(120 (0x78))
    OnBinaryExpr("i64.or" (132))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 20)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 25)
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 20)
    OnElseExpr
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 16)
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 20)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 20)
    OnLocalGetExpr(index: 17)
    OnLocalSetExpr(index: 16)
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnBrExpr(depth: 0)
    OnEndExpr
    OnEndExpr
    OnLocalGetExpr(index: 19)
    OnLocalSetExpr(index: 26)
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(10 (0xa))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 27)
    OnLocalGetExpr(index: 27)
    OnLocalSetExpr(index: 29)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(100 (0x64))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 30)
    OnLocalGetExpr(index: 30)
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(1569598799879 (0x16d73610007))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(9152545913355436039 (0x7f04600414010007))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(27161242174979847 (0x607f017f7f7f07))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(6917667572020019463 (0x60007e01607e0107))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(8581048631115644935 (0x7716024402000007))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(7020675722745438471 (0x616e735f69736107))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(8097318676878028807 (0x705f746f68737007))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(3564429133719368199 (0x3177656976657207))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(7598266772917454855 (0x6972775f64660807))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(7023106333170037767 (0x6177160000657407))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(8097875046789903111 (0x70616e735f697307))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(8246195871405339399 (0x72705f746f687307))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(518326709569086727 (0x731776569766507))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(7233174018434819591 (0x646165725f646607))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(144399978817650695 (0x201030403000007))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(504684637587833607 (0x701000103050307))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(8029185229598102279 (0x6f6d656d06021307))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(8313370034260505095 (0x735f060002797207))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(721702340421317639 (0xa04007472617407))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(18296002389392135 (0x41001e033abb07))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(1159748377689211143 (0x1018410141004107))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(607034589970695 (0x22818411a0107))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(4685290910907647239 (0x41057f427e044507))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(1444260200133496839 (0x140b0b0000311007))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(16888636314288135 (0x3c002010410007))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(90353746718031879 (0x141004101410007))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(3124812316426502 (0xb1a00101c4106))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(33793 (0x8401))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(72340710765967879 (0x101017e40013a07))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnLocalSetExpr(index: 32)
    OnBlockExpr(sig: void)
    OnLoopExpr(sig: void)
    OnLocalGetExpr(index: 26)
    OnLocalGetExpr(index: 20)
    OnCompareExpr("i64.lt_s" (83))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnConvertExpr("i32.eqz" (69))
    OnBrIfExpr(depth: 1)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 28)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 26)
    OnI64ConstExpr(0 (0x0))
    OnLocalSetExpr(index: 33)
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(40 (0x28))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(40 (0x28))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 29)
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(41 (0x29))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnI64ConstExpr(1 (0x1))
    OnLocalSetExpr(index: 33)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.sub" (125))
    OnLocalSetExpr(index: 34)
    OnLocalGetExpr(index: 34)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 23)
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(0 (0x0))
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(1886745720 (0x70757478))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(200706 (0x31002))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnI64ConstExpr(0 (0x0))
    OnLocalSetExpr(index: 33)
    OnEndExpr
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.sub" (125))
    OnLocalSetExpr(index: 29)
    OnEndExpr
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(123 (0x7b))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.sub" (125))
    OnLocalSetExpr(index: 34)
    OnLocalGetExpr(index: 34)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 35)
    OnLocalGetExpr(index: 35)
    OnI64ConstExpr(63 (0x3f))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(1074046723 (0x4004a703))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 35)
    OnI64ConstExpr(64 (0x40))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(4517635844 (0x10d45a704))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnEndExpr
    OnEndExpr
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(125 (0x7d))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.sub" (125))
    OnLocalSetExpr(index: 29)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 35)
    OnLocalGetExpr(index: 35)
    OnI64ConstExpr(63 (0x3f))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnI64ConstExpr(58 (0x3a))
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(2817 (0xb01))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnEndExpr
    OnElseExpr
    OnLocalGetExpr(index: 35)
    OnI64ConstExpr(58 (0x3a))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(2817 (0xb01))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 35)
    OnI64ConstExpr(64 (0x40))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(47429192708 (0xb0b000c04))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(1734702200 (0x67657478))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 26)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(135170 (0x21002))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnLocalSetExpr(index: 33)
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(1886745720 (0x70757478))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(2 (0x2))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 26)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 28)
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 29)
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(64 (0x40))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(274932433412 (0x4003400204))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(64 (0x40))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 29)
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(63 (0x3f))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(63 (0x3f))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 29)
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(58 (0x3a))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(1281 (0x501))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(58 (0x3a))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 29)
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(42 (0x2a))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 28)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 26)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 28)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 26)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnI64ConstExpr(61 (0x3d))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(8193 (0x2001))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(8 (0x8))
    OnBinaryExpr("i64.shl" (134))
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.or" (132))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(719507898884 (0xa786034204))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(88 (0x58))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 29)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 26)
    OnElseExpr
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(8193 (0x2001))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(8 (0x8))
    OnBinaryExpr("i64.shl" (134))
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.or" (132))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(890224414769671 (0x329a786034207))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnLocalSetExpr(index: 33)
    OnEndExpr
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(59 (0x3b))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 29)
    OnLocalGetExpr(index: 27)
    OnCompareExpr("i64.gt_s" (85))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.sub" (125))
    OnLocalSetExpr(index: 29)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 23)
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(88 (0x58))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(210691 (0x33703))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(120 (0x78))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.sub" (125))
    OnLocalSetExpr(index: 29)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 24)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(8449 (0x2101))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 24)
    OnI64ConstExpr(8 (0x8))
    OnBinaryExpr("i64.shl" (134))
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.or" (132))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(63 (0x3f))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(64 (0x40))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.or" (132))
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(58 (0x3a))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.or" (132))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 29)
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(48 (0x30))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 28)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 26)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(16897 (0x4201))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(255 (0xff))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 28)
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnLocalSetExpr(index: 33)
    OnElseExpr
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(0 (0x0))
    OnBinaryExpr("i64.and" (131))
    OnLocalGetExpr(index: 36)
    OnI64ConstExpr(120 (0x78))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 28)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 26)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnI64ConstExpr(61 (0x3d))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 28)
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 29)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(120 (0x78))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 29)
    OnLocalGetExpr(index: 26)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 26)
    OnElseExpr
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(8193 (0x2001))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 28)
    OnI64ConstExpr(8 (0x8))
    OnBinaryExpr("i64.shl" (134))
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.or" (132))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnLocalSetExpr(index: 33)
    OnEndExpr
    OnElseExpr
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLocalGetExpr(index: 28)
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 29)
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnLocalGetExpr(index: 33)
    OnI64ConstExpr(1 (0x1))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.sub" (125))
    OnLocalSetExpr(index: 34)
    OnLocalGetExpr(index: 34)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 37)
    OnLocalGetExpr(index: 37)
    OnI64ConstExpr(40 (0x28))
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnLocalGetExpr(index: 37)
    OnI64ConstExpr(61 (0x3d))
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnLocalGetExpr(index: 37)
    OnI64ConstExpr(63 (0x3f))
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnLocalGetExpr(index: 37)
    OnI64ConstExpr(64 (0x40))
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnLocalGetExpr(index: 37)
    OnI64ConstExpr(58 (0x3a))
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnLocalGetExpr(index: 37)
    OnI64ConstExpr(120 (0x78))
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnLocalGetExpr(index: 37)
    OnI64ConstExpr(88 (0x58))
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnLocalGetExpr(index: 37)
    OnI64ConstExpr(1886745720 (0x70757478))
    OnCompareExpr("i64.ne" (82))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnBinaryExpr("i64.and" (131))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.sub" (125))
    OnLocalSetExpr(index: 29)
    OnLocalGetExpr(index: 29)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 23)
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(43 (0x2b))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(31745 (0x7c01))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(45 (0x2d))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(32001 (0x7d01))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(124 (0x7c))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(33793 (0x8401))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(38 (0x26))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(33537 (0x8301))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(15420 (0x3c3c))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(34305 (0x8601))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(15934 (0x3e3e))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(34817 (0x8801))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(15677 (0x3d3d))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(11292930 (0xac5102))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(8509 (0x213d))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(11293186 (0xac5202))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(60 (0x3c))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(11293442 (0xac5302))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(62 (0x3e))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(11293954 (0xac5502))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(15421 (0x3c3d))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(11294466 (0xac5702))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnElseExpr
    OnLocalGetExpr(index: 23)
    OnI64ConstExpr(15933 (0x3e3d))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(11294978 (0xac5902))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnBrExpr(depth: 0)
    OnEndExpr
    OnEndExpr
    OnI64ConstExpr(7420 (0x1cfc))
    OnLocalSetExpr(index: 38)
    OnLocalGetExpr(index: 31)
    OnLocalSetExpr(index: 39)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(4683747018559652103 (0x410003190b0b0107))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(68787306503 (0x10040b0007))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(285920339230727 (0x1040b04410007))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(75171690140663815 (0x10b104100000007))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnI64ConstExpr(1 (0x1))
    OnStoreExpr(opcode: "i64.store" (55), memidx: 0, align log2: 3, offset: 0)
    OnLocalGetExpr(index: 31)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 31)
    OnLocalGetExpr(index: 30)
    OnLocalSetExpr(index: 40)
    OnI64ConstExpr(0 (0x0))
    OnLocalSetExpr(index: 41)
    OnBlockExpr(sig: void)
    OnLoopExpr(sig: void)
    OnLocalGetExpr(index: 40)
    OnLocalGetExpr(index: 31)
    OnCompareExpr("i64.lt_s" (83))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnConvertExpr("i32.eqz" (69))
    OnBrIfExpr(depth: 1)
    OnLocalGetExpr(index: 40)
    OnLocalGetExpr(index: 32)
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnI64ConstExpr(0 (0x0))
    OnLocalSetExpr(index: 41)
    OnEndExpr
    OnLocalGetExpr(index: 40)
    OnLocalGetExpr(index: 39)
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 41)
    OnLocalSetExpr(index: 42)
    OnBlockExpr(sig: void)
    OnLoopExpr(sig: void)
    OnLocalGetExpr(index: 42)
    OnLocalGetExpr(index: 38)
    OnCompareExpr("i64.lt_s" (83))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnConvertExpr("i32.eqz" (69))
    OnBrIfExpr(depth: 1)
    OnLocalGetExpr(index: 43)
    OnI64ConstExpr(1 (0x1))
    OnLocalGetExpr(index: 42)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 42)
    OnBrExpr(depth: 0)
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnLocalGetExpr(index: 40)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 44)
    OnLocalGetExpr(index: 40)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 40)
    OnLocalGetExpr(index: 44)
    OnI64ConstExpr(255 (0xff))
    OnBinaryExpr("i64.and" (131))
    OnLocalSetExpr(index: 45)
    OnLocalGetExpr(index: 45)
    OnI64ConstExpr(255 (0xff))
    OnCompareExpr("i64.eq" (81))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnIfExpr(sig: void)
    OnLocalGetExpr(index: 40)
    OnI64ConstExpr(3 (0x3))
    OnBinaryExpr("i64.shl" (134))
    OnConvertExpr("i32.wrap_i64" (167))
    OnLoadExpr(opcode: "i64.load" (41), memidx: 0, align log2: 3, offset: 0)
    OnLocalSetExpr(index: 44)
    OnLocalGetExpr(index: 40)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 40)
    OnI64ConstExpr(0 (0x0))
    OnBlockExpr(sig: void)
    OnLoopExpr(sig: void)
    OnLocalGetExpr(index: 42)
    OnI64ConstExpr(9 (0x9))
    OnCompareExpr("i64.lt_s" (83))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnConvertExpr("i32.eqz" (69))
    OnBrIfExpr(depth: 1)
    OnLocalGetExpr(index: 43)
    OnI64ConstExpr(128 (0x80))
    OnLocalGetExpr(index: 44)
    OnI64ConstExpr(127 (0x7f))
    OnBinaryExpr("i64.and" (131))
    OnBinaryExpr("i64.or" (132))
    OnLocalGetExpr(index: 41)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 41)
    OnLocalGetExpr(index: 44)
    OnI64ConstExpr(7 (0x7))
    OnBinaryExpr("i64.shr_u" (136))
    OnLocalSetExpr(index: 44)
    OnLocalGetExpr(index: 42)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 42)
    OnBrExpr(depth: 0)
    OnEndExpr
    OnEndExpr
    OnLocalGetExpr(index: 43)
    OnLocalGetExpr(index: 44)
    OnI64ConstExpr(127 (0x7f))
    OnBinaryExpr("i64.and" (131))
    OnLocalSetExpr(index: 42)
    OnLocalGetExpr(index: 41)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 41)
    OnElseExpr
    OnBlockExpr(sig: void)
    OnLoopExpr(sig: void)
    OnLocalGetExpr(index: 45)
    OnI64ConstExpr(0 (0x0))
    OnCompareExpr("i64.gt_s" (85))
    OnConvertExpr("i64.extend_i32_s" (172))
    OnConvertExpr("i32.wrap_i64" (167))
    OnConvertExpr("i32.eqz" (69))
    OnBrIfExpr(depth: 1)
    OnLocalGetExpr(index: 44)
    OnI64ConstExpr(8 (0x8))
    OnBinaryExpr("i64.shr_u" (136))
    OnLocalSetExpr(index: 44)
    OnLocalGetExpr(index: 43)
    OnLocalGetExpr(index: 44)
    OnI64ConstExpr(255 (0xff))
    OnBinaryExpr("i64.and" (131))
    OnLocalGetExpr(index: 41)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.add" (124))
    OnLocalSetExpr(index: 41)
    OnLocalGetExpr(index: 45)
    OnI64ConstExpr(1 (0x1))
    OnBinaryExpr("i64.sub" (125))
    OnLocalSetExpr(index: 45)
    OnBrExpr(depth: 0)
    OnEndExpr
    OnEndExpr
    OnEndExpr
    OnBrExpr(depth: 0)
    OnEndExpr
    OnEndExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnNopExpr
    OnEndExpr
    EndFunctionBody(4)
  EndCodeSection
  BeginDataSection(25)
    OnDataSegmentCount(3)
    BeginDataSegment(index: 0, memory_index: 0, flags: 0)
    BeginDataSegmentInitExpr(0)
    OnI32ConstExpr(0 (0x0))
    OnEndExpr
    EndDataSegmentInitExpr(0)
    OnDataSegmentData(index:0, size:4)
    EndDataSegment(0)
    BeginDataSegment(index: 1, memory_index: 0, flags: 0)
    BeginDataSegmentInitExpr(1)
    OnI32ConstExpr(4 (0x4))
    OnEndExpr
    EndDataSegmentInitExpr(1)
    OnDataSegmentData(index:1, size:4)
    EndDataSegment(1)
    BeginDataSegment(index: 2, memory_index: 0, flags: 0)
    BeginDataSegmentInitExpr(2)
    OnI32ConstExpr(16 (0x10))
    OnEndExpr
    EndDataSegmentInitExpr(2)
    OnDataSegmentData(index:2, size:1)
    EndDataSegment(2)
  EndDataSection
EndModule
source.wasm:00012d2: error: type mismatch at end of `if false` branch, expected [] but got [i64]
source.wasm:0001870: error: type mismatch at end of `if true` branch, expected [] but got [i64, i64]
