(module
  (type (;0;) (func (param i32 i32 i32 i32) (result i32)))
  (type (;1;) (func (result i64)))
  (type (;2;) (func (param i64)))
  (type (;3;) (func))
  (import "wasi_snapshot_preview1" "fd_write" (func (;0;) (type 0)))
  (import "wasi_snapshot_preview1" "fd_read" (func (;1;) (type 0)))
  (func (;2;) (type 1) (result i64)
    i32.const 0
    i32.const 0
    i32.const 1
    i32.const 24
    call 1
    drop
    i32.const 24
    i32.load
    i32.eqz
    if  ;; label = @1
      i64.const -1
      return
    end
    i32.const 16
    i64.load8_u)
  (func (;3;) (type 2) (param i64)
    i32.const 16
    local.get 0
    i64.store8
    i32.const 1
    i32.const 0
    i32.const 1
    i32.const 28
    call 0
    drop)
  (func (;4;) (type 3)
    (local i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64 i64)
    nop
    call 2
    local.set 16
    i64.const 16
    local.set 17
    i64.const 256
    local.set 18
    local.get 18
    local.set 19
    i64.const 0
    local.set 20
    block  ;; label = @1
      loop  ;; label = @2
        local.get 20
        i64.const 10000
        i64.lt_s
        i64.extend_i32_s
        local.get 16
        i64.const 0
        i64.gt_s
        i64.extend_i32_s
        i64.and
        i32.wrap_i64
        i32.eqz
        br_if 1 (;@1;)
        local.get 20
        i64.const 1
        i64.add
        local.set 20
        local.get 16
        i64.const 32
        i64.eq
        i64.extend_i32_s
        local.get 16
        i64.const 10
        i64.eq
        i64.extend_i32_s
        i64.or
        i32.wrap_i64
        if  ;; label = @3
          call 2
          local.set 16
        else
          local.get 16
          i64.const 36
          i64.eq
          i64.extend_i32_s
          i32.wrap_i64
          if  ;; label = @4
            i64.const 0
            local.set 21
            call 2
            local.set 16
            block  ;; label = @5
              loop  ;; label = @6
                local.get 16
                i64.const 48
                i64.ge_s
                i64.extend_i32_s
                local.get 16
                i64.const 57
                i64.le_s
                i64.extend_i32_s
                i64.and
                local.get 16
                i64.const 97
                i64.ge_s
                i64.extend_i32_s
                local.get 16
                i64.const 102
                i64.le_s
                i64.extend_i32_s
                i64.and
                i64.or
                i32.wrap_i64
                i32.eqz
                br_if 1 (;@5;)
                local.get 16
                local.get 16
                i64.const 58
                i64.sub
                i64.const 8
                i64.shr_u
                i64.const 48
                i64.and
                i64.sub
                i64.const 96
                local.get 16
                i64.sub
                i64.const 8
                i64.shr_u
                i64.const 87
                i64.and
                i64.sub
                local.set 16
                local.get 21
                i64.const 4
                i64.shl
                local.get 16
                i64.or
                local.set 21
                call 2
                local.set 16
                br 0 (;@6;)
              end
            end
            local.get 19
            i64.const 3
            i64.shl
            i32.wrap_i64
            i64.const 48
            i64.store
            local.get 19
            i64.const 1
            i64.add
            local.set 19
            local.get 19
            i64.const 3
            i64.shl
            i32.wrap_i64
            local.get 21
            i64.store
            local.get 19
            i64.const 1
            i64.add
            local.set 19
          else
            local.get 16
            i64.const 39
            i64.eq
            i64.extend_i32_s
            i32.wrap_i64
            if  ;; label = @5
              i64.const 0
              local.set 21
              call 2
              local.set 16
              block  ;; label = @6
                loop  ;; label = @7
                  local.get 16
                  i64.const 39
                  i64.ne
                  i64.extend_i32_s
                  i32.wrap_i64
                  i32.eqz
                  br_if 1 (;@6;)
                  local.get 21
                  i64.const 8
                  i64.shl
                  local.get 16
                  i64.const 255
                  i64.and
                  i64.or
                  local.set 21
                  call 2
                  local.set 16
                  br 0 (;@7;)
                end
              end
              call 2
              local.set 16
              local.get 19
              i64.const 3
              i64.shl
              i32.wrap_i64
              i64.const 48
              i64.store
              local.get 19
              i64.const 1
              i64.add
              local.set 19
              local.get 19
              i64.const 3
              i64.shl
              i32.wrap_i64
              local.get 21
              i64.store
              local.get 19
              i64.const 1
              i64.add
              local.set 19
            else
              local.get 16
              i64.const 48
              i64.ge_s
              i64.extend_i32_s
              local.get 16
              i64.const 57
              i64.le_s
              i64.extend_i32_s
              i64.and
              i32.wrap_i64
              if  ;; label = @6
                local.get 16
                i64.const 48
                i64.sub
                local.set 21
                call 2
                local.set 16
                block  ;; label = @7
                  loop  ;; label = @8
                    local.get 16
                    i64.const 48
                    i64.ge_s
                    i64.extend_i32_s
                    local.get 16
                    i64.const 57
                    i64.le_s
                    i64.extend_i32_s
                    i64.and
                    i32.wrap_i64
                    i32.eqz
                    br_if 1 (;@7;)
                    local.get 21
                    i64.const 3
                    i64.shl
                    local.get 21
                    i64.const 1
                    i64.shl
                    i64.add
                    local.get 16
                    i64.const 48
                    i64.sub
                    i64.add
                    local.set 21
                    call 2
                    local.set 16
                    br 0 (;@8;)
                  end
                end
                local.get 19
                i64.const 3
                i64.shl
                i32.wrap_i64
                i64.const 48
                i64.store
                local.get 19
                i64.const 1
                i64.add
                local.set 19
                local.get 19
                i64.const 3
                i64.shl
                i32.wrap_i64
                local.get 21
                i64.store
                local.get 19
                i64.const 1
                i64.add
                local.set 19
              else
                local.get 16
                i64.const 61
                i64.eq
                i64.extend_i32_s
                local.get 16
                i64.const 60
                i64.eq
                i64.extend_i32_s
                i64.or
                local.get 16
                i64.const 62
                i64.eq
                i64.extend_i32_s
                i64.or
                local.get 16
                i64.const 33
                i64.eq
                i64.extend_i32_s
                i64.or
                i32.wrap_i64
                if  ;; label = @7
                  local.get 16
                  local.set 22
                  call 2
                  local.set 16
                  local.get 16
                  i64.const 61
                  i64.eq
                  i64.extend_i32_s
                  local.get 16
                  i64.const 60
                  i64.eq
                  i64.extend_i32_s
                  i64.or
                  local.get 16
                  i64.const 62
                  i64.eq
                  i64.extend_i32_s
                  i64.or
                  i32.wrap_i64
                  if  ;; label = @8
                    local.get 22
                    i64.const 8
                    i64.shl
                    local.get 16
                    i64.add
                    local.set 22
                    call 2
                    local.set 16
                  end
                  local.get 19
                  i64.const 3
                  i64.shl
                  i32.wrap_i64
                  local.get 22
                  i64.store
                  local.get 19
                  i64.const 1
                  i64.add
                  local.set 19
                else
                  local.get 16
                  i64.const 65
                  i64.ge_s
                  i64.extend_i32_s
                  local.get 16
                  i64.const 122
                  i64.le_s
                  i64.extend_i32_s
                  i64.and
                  i32.wrap_i64
                  if  ;; label = @8
                    i64.const 0
                    local.set 23
                    block  ;; label = @9
                      loop  ;; label = @10
                        local.get 16
                        i64.const 65
                        i64.ge_s
                        i64.extend_i32_s
                        local.get 16
                        i64.const 122
                        i64.le_s
                        i64.extend_i32_s
                        i64.and
                        i32.wrap_i64
                        i32.eqz
                        br_if 1 (;@9;)
                        local.get 23
                        i64.const 8
                        i64.shl
                        local.get 16
                        i64.add
                        local.set 23
                        call 2
                        local.set 16
                        br 0 (;@10;)
                      end
                    end
                    local.get 16
                    i64.const 40
                    i64.eq
                    i64.extend_i32_s
                    i32.wrap_i64
                    if  ;; label = @9
                      call 2
                      local.set 16
                      local.get 19
                      i64.const 3
                      i64.shl
                      i32.wrap_i64
                      i64.const 99
                      i64.store
                      local.get 19
                      i64.const 1
                      i64.add
                      local.set 19
                      local.get 19
                      i64.const 3
                      i64.shl
                      i32.wrap_i64
                      local.get 23
                      i64.store
                      local.get 19
                      i64.const 1
                      i64.add
                      local.set 19
                    else
                      i64.const 16
                      local.set 24
                      block  ;; label = @10
                        loop  ;; label = @11
                          local.get 24
                          local.get 17
                          i64.lt_s
                          i64.extend_i32_s
                          local.get 24
                          i64.const 3
                          i64.shl
                          i32.wrap_i64
                          i64.load
                          local.get 23
                          i64.ne
                          i64.extend_i32_s
                          i64.and
                          i32.wrap_i64
                          i32.eqz
                          br_if 1 (;@10;)
                          local.get 24
                          i64.const 1
                          i64.add
                          local.set 24
                          br 0 (;@11;)
                        end
                      end
                      local.get 24
                      i64.const 3
                      i64.shl
                      i32.wrap_i64
                      local.get 23
                      i64.store
                      local.get 24
                      local.get 17
                      i64.eq
                      i64.extend_i32_s
                      i32.wrap_i64
                      if  ;; label = @10
                        local.get 17
                        i64.const 1
                        i64.add
                        local.set 17
                      end
                      local.get 19
                      i64.const 3
                      i64.shl
                      i32.wrap_i64
                      i64.const 120
                      i64.store
                      local.get 19
                      i64.const 1
                      i64.add
                      local.set 19
                      local.get 19
                      i64.const 3
                      i64.shl
                      i32.wrap_i64
                      local.get 24
                      i64.store
                      local.get 19
                      i64.const 1
                      i64.add
                      local.set 19
                    end
                  else
                    local.get 19
                    i64.const 3
                    i64.shl
                    i32.wrap_i64
                    local.get 16
                    i64.store
                    local.get 19
                    i64.const 1
                    i64.add
                    local.set 19
                    call 2
                    local.set 16
                  end
                end
              end
            end
          end
        end
        br 0 (;@2;)
      end
    end
    local.get 18
    local.set 25
    local.get 27
    i64.const 10
    i64.add
    local.set 26
    local.get 26
    local.set 28
    local.get 28
    i64.const 100
    i64.add
    local.set 29
    local.get 29
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 1569598799879
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 9152545913355436039
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 27161242174979847
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 6917667572020019463
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 8581048631115644935
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 7020675722745438471
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 8097318676878028807
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 3564429133719368199
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 7598266772917454855
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 7023106333170037767
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 8097875046789903111
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 8246195871405339399
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 518326709569086727
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 7233174018434819591
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 144399978817650695
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 504684637587833607
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 8029185229598102279
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 8313370034260505095
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 721702340421317639
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 18296002389392135
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 1159748377689211143
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 607034589970695
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 4685290910907647239
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 1444260200133496839
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 16888636314288135
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 90353746718031879
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 3124812316426502
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 33793
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 72340710765967879
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    local.set 31
    block  ;; label = @1
      loop  ;; label = @2
        local.get 25
        local.get 19
        i64.lt_s
        i64.extend_i32_s
        i32.wrap_i64
        i32.eqz
        br_if 1 (;@1;)
        local.get 25
        i64.const 3
        i64.shl
        i32.wrap_i64
        i64.load
        local.set 27
        local.get 25
        i64.const 1
        i64.add
        local.set 25
        i64.const 0
        local.set 32
        local.get 27
        i64.const 40
        i64.eq
        i64.extend_i32_s
        i32.wrap_i64
        if  ;; label = @3
          local.get 28
          i64.const 3
          i64.shl
          i32.wrap_i64
          i64.const 40
          i64.store
          local.get 28
          i64.const 1
          i64.add
          local.set 28
        else
          local.get 27
          i64.const 41
          i64.eq
          i64.extend_i32_s
          i32.wrap_i64
          if  ;; label = @4
            i64.const 1
            local.set 32
            local.get 28
            i64.const 1
            i64.sub
            local.set 33
            local.get 33
            i64.const 3
            i64.shl
            i32.wrap_i64
            i64.load
            local.set 22
            local.get 22
            i64.const 0
            i64.ne
            i64.extend_i32_s
            i32.wrap_i64
            if  ;; label = @5
              local.get 22
              i64.const 99
              i64.eq
              i64.extend_i32_s
              i32.wrap_i64
              if  ;; label = @6
                local.get 30
                i64.const 3
                i64.shl
                i32.wrap_i64
                i64.const 200706
                i64.store
                local.get 30
                i64.const 1
                i64.add
                local.set 30
                i64.const 0
                local.set 32
              end
              local.get 28
              i64.const 1
              i64.sub
              local.set 28
            end
          else
            local.get 27
            i64.const 123
            i64.eq
            i64.extend_i32_s
            i32.wrap_i64
            if  ;; label = @5
              local.get 28
              i64.const 1
              i64.sub
              local.set 33
              local.get 33
              i64.const 3
              i64.shl
              i32.wrap_i64
              i64.load
              local.set 34
              local.get 34
              i64.const 63
              i64.eq
              i64.extend_i32_s
              i32.wrap_i64
              if  ;; label = @6
                local.get 30
                i64.const 3
                i64.shl
                i32.wrap_i64
                i64.const 1074046723
                i64.store
                local.get 30
                i64.const 1
                i64.add
                local.set 30
              else
                local.get 34
                i64.const 64
                i64.eq
                i64.extend_i32_s
                i32.wrap_i64
                if  ;; label = @7
                  local.get 30
                  i64.const 3
                  i64.shl
                  i32.wrap_i64
                  i64.const 4517635844
                  i64.store
                  local.get 30
                  i64.const 1
                  i64.add
                  local.set 30
                end
              end
            else
              local.get 27
              i64.const 125
              i64.eq
              i64.extend_i32_s
              i32.wrap_i64
              if  ;; label = @6
                local.get 28
                i64.const 1
                i64.sub
                local.set 28
                local.get 28
                i64.const 3
                i64.shl
                i32.wrap_i64
                i64.load
                local.set 34
                local.get 34
                i64.const 63
                i64.eq
                i64.extend_i32_s
                i32.wrap_i64
                if  ;; label = @7
                  local.get 25
                  i64.const 3
                  i64.shl
                  i32.wrap_i64
                  i64.load
                  i64.const 58
                  i64.ne
                  i64.extend_i32_s
                  i32.wrap_i64
                  if  ;; label = @8
                    local.get 30
                    i64.const 3
                    i64.shl
                    i32.wrap_i64
                    i64.const 2817
                    i64.store
                    local.get 30
                    i64.const 1
                    i64.add
                    local.set 30
                  end
                else
                  local.get 34
                  i64.const 58
                  i64.eq
                  i64.extend_i32_s
                  i32.wrap_i64
                  if  ;; label = @8
                    local.get 30
                    i64.const 3
                    i64.shl
                    i32.wrap_i64
                    i64.const 2817
                    i64.store
                    local.get 30
                    i64.const 1
                    i64.add
                    local.set 30
                  else
                    local.get 34
                    i64.const 64
                    i64.eq
                    i64.extend_i32_s
                    i32.wrap_i64
                    if  ;; label = @9
                      local.get 30
                      i64.const 3
                      i64.shl
                      i32.wrap_i64
                      i64.const 47429192708
                      i64.store
                      local.get 30
                      i64.const 1
                      i64.add
                      local.set 30
                    end
                  end
                end
              else
                local.get 27
                i64.const 64
                i64.eq
                i64.extend_i32_s
                i32.wrap_i64
                if  ;; label = @7
                  local.get 30
                  i64.const 3
                  i64.shl
                  i32.wrap_i64
                  i64.const 274932433412
                  i64.store
                  local.get 30
                  i64.const 1
                  i64.add
                  local.set 30
                  local.get 28
                  i64.const 3
                  i64.shl
                  i32.wrap_i64
                  i64.const 64
                  i64.store
                  local.get 28
                  i64.const 1
                  i64.add
                  local.set 28
                else
                  local.get 27
                  i64.const 63
                  i64.eq
                  i64.extend_i32_s
                  i32.wrap_i64
                  if  ;; label = @8
                    local.get 28
                    i64.const 3
                    i64.shl
                    i32.wrap_i64
                    i64.const 63
                    i64.store
                    local.get 28
                    i64.const 1
                    i64.add
                    local.set 28
                  else
                    local.get 27
                    i64.const 58
                    i64.eq
                    i64.extend_i32_s
                    i32.wrap_i64
                    if  ;; label = @9
                      local.get 30
                      i64.const 3
                      i64.shl
                      i32.wrap_i64
                      i64.const 1281
                      i64.store
                      local.get 30
                      i64.const 1
                      i64.add
                      local.set 30
                      local.get 28
                      i64.const 3
                      i64.shl
                      i32.wrap_i64
                      i64.const 58
                      i64.store
                      local.get 28
                      i64.const 1
                      i64.add
                      local.set 28
                    else
                      local.get 27
                      i64.const 42
                      i64.eq
                      i64.extend_i32_s
                      i32.wrap_i64
                      if  ;; label = @10
                        local.get 25
                        i64.const 3
                        i64.shl
                        i32.wrap_i64
                        i64.load
                        local.set 27
                        local.get 25
                        i64.const 1
                        i64.add
                        local.set 25
                        local.get 25
                        i64.const 3
                        i64.shl
                        i32.wrap_i64
                        i64.load
                        local.set 27
                        local.get 25
                        i64.const 1
                        i64.add
                        local.set 25
                        local.get 25
                        i64.const 3
                        i64.shl
                        i32.wrap_i64
                        i64.load
                        i64.const 61
                        i64.eq
                        i64.extend_i32_s
                        i32.wrap_i64
                        if  ;; label = @11
                          local.get 30
                          i64.const 3
                          i64.shl
                          i32.wrap_i64
                          i64.const 8193
                          i64.store
                          local.get 30
                          i64.const 1
                          i64.add
                          local.set 30
                          local.get 30
                          i64.const 3
                          i64.shl
                          i32.wrap_i64
                          local.get 27
                          i64.const 8
                          i64.shl
                          i64.const 1
                          i64.or
                          i64.store
                          local.get 30
                          i64.const 1
                          i64.add
                          local.set 30
                          local.get 30
                          i64.const 3
                          i64.shl
                          i32.wrap_i64
                          i64.const 719507898884
                          i64.store
                          local.get 30
                          i64.const 1
                          i64.add
                          local.set 30
                          local.get 28
                          i64.const 3
                          i64.shl
                          i32.wrap_i64
                          i64.const 88
                          i64.store
                          local.get 28
                          i64.const 1
                          i64.add
                          local.set 28
                          local.get 25
                          i64.const 1
                          i64.add
                          local.set 25
                        else
                          local.get 30
                          i64.const 3
                          i64.shl
                          i32.wrap_i64
                          i64.const 8193
                          i64.store
                          local.get 30
                          i64.const 1
                          i64.add
                          local.set 30
                          local.get 30
                          i64.const 3
                          i64.shl
                          i32.wrap_i64
                          local.get 27
                          i64.const 8
                          i64.shl
                          i64.const 1
                          i64.or
                          i64.store
                          local.get 30
                          i64.const 1
                          i64.add
                          local.set 30
                          local.get 30
                          i64.const 3
                          i64.shl
                          i32.wrap_i64
                          i64.const 890224414769671
                          i64.store
                          local.get 30
                          i64.const 1
                          i64.add
                          local.set 30
                          i64.const 1
                          local.set 32
                        end
                      else
                        local.get 27
                        i64.const 59
                        i64.eq
                        i64.extend_i32_s
                        i32.wrap_i64
                        if  ;; label = @11
                          local.get 28
                          local.get 26
                          i64.gt_s
                          i64.extend_i32_s
                          i32.wrap_i64
                          if  ;; label = @12
                            local.get 28
                            i64.const 1
                            i64.sub
                            local.set 28
                            local.get 28
                            i64.const 3
                            i64.shl
                            i32.wrap_i64
                            i64.load
                            local.set 22
                            local.get 22
                            i64.const 88
                            i64.eq
                            i64.extend_i32_s
                            i32.wrap_i64
                            if  ;; label = @13
                              local.get 30
                              i64.const 3
                              i64.shl
                              i32.wrap_i64
                              i64.const 210691
                              i64.store
                              local.get 30
                              i64.const 1
                              i64.add
                              local.set 30
                            else
                              local.get 22
                              i64.const 120
                              i64.eq
                              i64.extend_i32_s
                              i32.wrap_i64
                              if  ;; label = @14
                                local.get 28
                                i64.const 1
                                i64.sub
                                local.set 28
                                local.get 28
                                i64.const 3
                                i64.shl
                                i32.wrap_i64
                                i64.load
                                local.set 23
                                local.get 30
                                i64.const 3
                                i64.shl
                                i32.wrap_i64
                                i64.const 8449
                                i64.store
                                local.get 30
                                i64.const 1
                                i64.add
                                local.set 30
                                local.get 30
                                i64.const 3
                                i64.shl
                                i32.wrap_i64
                                local.get 23
                                i64.const 8
                                i64.shl
                                i64.const 1
                                i64.or
                                i64.store
                                local.get 30
                                i64.const 1
                                i64.add
                                local.set 30
                              else
                                local.get 22
                                i64.const 63
                                i64.eq
                                i64.extend_i32_s
                                local.get 22
                                i64.const 64
                                i64.eq
                                i64.extend_i32_s
                                i64.or
                                local.get 22
                                i64.const 58
                                i64.eq
                                i64.extend_i32_s
                                i64.or
                                i32.wrap_i64
                                if  ;; label = @15
                                  local.get 28
                                  i64.const 1
                                  i64.add
                                  local.set 28
                                end
                              end
                            end
                          end
                        else
                          local.get 27
                          i64.const 48
                          i64.eq
                          i64.extend_i32_s
                          i32.wrap_i64
                          if  ;; label = @12
                            local.get 25
                            i64.const 3
                            i64.shl
                            i32.wrap_i64
                            i64.load
                            local.set 27
                            local.get 25
                            i64.const 1
                            i64.add
                            local.set 25
                            local.get 30
                            i64.const 3
                            i64.shl
                            i32.wrap_i64
                            i64.const 16897
                            i64.store
                            local.get 30
                            i64.const 1
                            i64.add
                            local.set 30
                            local.get 30
                            i64.const 3
                            i64.shl
                            i32.wrap_i64
                            i64.const 255
                            i64.store
                            local.get 30
                            i64.const 1
                            i64.add
                            local.set 30
                            local.get 30
                            i64.const 3
                            i64.shl
                            i32.wrap_i64
                            local.get 27
                            i64.store
                            local.get 30
                            i64.const 1
                            i64.add
                            local.set 30
                            i64.const 1
                            local.set 32
                          else
                            local.get 27
                            i64.const 120
                            i64.eq
                            i64.extend_i32_s
                            i32.wrap_i64
                            if  ;; label = @13
                              local.get 25
                              i64.const 3
                              i64.shl
                              i32.wrap_i64
                              i64.load
                              local.set 27
                              local.get 25
                              i64.const 1
                              i64.add
                              local.set 25
                              local.get 25
                              i64.const 3
                              i64.shl
                              i32.wrap_i64
                              i64.load
                              i64.const 61
                              i64.eq
                              i64.extend_i32_s
                              i32.wrap_i64
                              if  ;; label = @14
                                local.get 28
                                i64.const 3
                                i64.shl
                                i32.wrap_i64
                                local.get 27
                                i64.store
                                local.get 28
                                i64.const 1
                                i64.add
                                local.set 28
                                local.get 28
                                i64.const 3
                                i64.shl
                                i32.wrap_i64
                                i64.const 120
                                i64.store
                                local.get 28
                                i64.const 1
                                i64.add
                                local.set 28
                                local.get 25
                                i64.const 1
                                i64.add
                                local.set 25
                              else
                                local.get 30
                                i64.const 3
                                i64.shl
                                i32.wrap_i64
                                i64.const 8193
                                i64.store
                                local.get 30
                                i64.const 1
                                i64.add
                                local.set 30
                                local.get 30
                                i64.const 3
                                i64.shl
                                i32.wrap_i64
                                local.get 27
                                i64.const 8
                                i64.shl
                                i64.const 1
                                i64.or
                                i64.store
                                local.get 30
                                i64.const 1
                                i64.add
                                local.set 30
                                i64.const 1
                                local.set 32
                              end
                            else
                              local.get 27
                              i64.const 99
                              i64.eq
                              i64.extend_i32_s
                              i32.wrap_i64
                              if  ;; label = @14
                                local.get 25
                                i64.const 3
                                i64.shl
                                i32.wrap_i64
                                i64.load
                                local.set 27
                                local.get 25
                                i64.const 1
                                i64.add
                                local.set 25
                                local.get 27
                                i64.const 6776180
                                i64.eq
                                i64.extend_i32_s
                                i32.wrap_i64
                                if  ;; label = @15
                                  local.get 30
                                  i64.const 3
                                  i64.shl
                                  i32.wrap_i64
                                  i64.const 135170
                                  i64.store
                                  local.get 30
                                  i64.const 1
                                  i64.add
                                  local.set 30
                                  i64.const 1
                                  local.set 32
                                  local.get 25
                                  i64.const 1
                                  i64.add
                                  local.set 25
                                else
                                  local.get 27
                                  i64.const 7370100
                                  i64.eq
                                  i64.extend_i32_s
                                  i32.wrap_i64
                                  if  ;; label = @16
                                    local.get 28
                                    i64.const 3
                                    i64.shl
                                    i32.wrap_i64
                                    i64.const 99
                                    i64.store
                                    local.get 28
                                    i64.const 1
                                    i64.add
                                    local.set 28
                                  end
                                end
                              else
                                local.get 28
                                i64.const 3
                                i64.shl
                                i32.wrap_i64
                                local.get 27
                                i64.store
                                local.get 28
                                i64.const 1
                                i64.add
                                local.set 28
                              end
                            end
                          end
                        end
                      end
                    end
                  end
                end
              end
            end
          end
        end
        local.get 32
        i64.const 1
        i64.eq
        i64.extend_i32_s
        i32.wrap_i64
        if  ;; label = @3
          local.get 28
          i64.const 1
          i64.sub
          local.set 33
          local.get 33
          i64.const 3
          i64.shl
          i32.wrap_i64
          i64.load
          local.set 35
          local.get 35
          i64.const 40
          i64.ne
          i64.extend_i32_s
          local.get 35
          i64.const 61
          i64.ne
          i64.extend_i32_s
          i64.and
          local.get 35
          i64.const 63
          i64.ne
          i64.extend_i32_s
          i64.and
          local.get 35
          i64.const 64
          i64.ne
          i64.extend_i32_s
          i64.and
          local.get 35
          i64.const 58
          i64.ne
          i64.extend_i32_s
          i64.and
          local.get 35
          i64.const 120
          i64.ne
          i64.extend_i32_s
          i64.and
          local.get 35
          i64.const 88
          i64.ne
          i64.extend_i32_s
          i64.and
          local.get 35
          i64.const 99
          i64.ne
          i64.extend_i32_s
          i64.and
          i32.wrap_i64
          if  ;; label = @4
            local.get 28
            i64.const 1
            i64.sub
            local.set 28
            local.get 28
            i64.const 3
            i64.shl
            i32.wrap_i64
            i64.load
            local.set 22
            local.get 22
            i64.const 43
            i64.eq
            i64.extend_i32_s
            i32.wrap_i64
            if  ;; label = @5
              local.get 30
              i64.const 3
              i64.shl
              i32.wrap_i64
              i64.const 31745
              i64.store
              local.get 30
              i64.const 1
              i64.add
              local.set 30
            else
              local.get 22
              i64.const 45
              i64.eq
              i64.extend_i32_s
              i32.wrap_i64
              if  ;; label = @6
                local.get 30
                i64.const 3
                i64.shl
                i32.wrap_i64
                i64.const 32001
                i64.store
                local.get 30
                i64.const 1
                i64.add
                local.set 30
              else
                local.get 22
                i64.const 124
                i64.eq
                i64.extend_i32_s
                i32.wrap_i64
                if  ;; label = @7
                  local.get 30
                  i64.const 3
                  i64.shl
                  i32.wrap_i64
                  i64.const 33793
                  i64.store
                  local.get 30
                  i64.const 1
                  i64.add
                  local.set 30
                else
                  local.get 22
                  i64.const 38
                  i64.eq
                  i64.extend_i32_s
                  i32.wrap_i64
                  if  ;; label = @8
                    local.get 30
                    i64.const 3
                    i64.shl
                    i32.wrap_i64
                    i64.const 33537
                    i64.store
                    local.get 30
                    i64.const 1
                    i64.add
                    local.set 30
                  else
                    local.get 22
                    i64.const 15420
                    i64.eq
                    i64.extend_i32_s
                    i32.wrap_i64
                    if  ;; label = @9
                      local.get 30
                      i64.const 3
                      i64.shl
                      i32.wrap_i64
                      i64.const 34305
                      i64.store
                      local.get 30
                      i64.const 1
                      i64.add
                      local.set 30
                    else
                      local.get 22
                      i64.const 15934
                      i64.eq
                      i64.extend_i32_s
                      i32.wrap_i64
                      if  ;; label = @10
                        local.get 30
                        i64.const 3
                        i64.shl
                        i32.wrap_i64
                        i64.const 34817
                        i64.store
                        local.get 30
                        i64.const 1
                        i64.add
                        local.set 30
                      else
                        local.get 22
                        i64.const 15677
                        i64.eq
                        i64.extend_i32_s
                        i32.wrap_i64
                        if  ;; label = @11
                          local.get 30
                          i64.const 3
                          i64.shl
                          i32.wrap_i64
                          i64.const 11292930
                          i64.store
                          local.get 30
                          i64.const 1
                          i64.add
                          local.set 30
                        else
                          local.get 22
                          i64.const 8509
                          i64.eq
                          i64.extend_i32_s
                          i32.wrap_i64
                          if  ;; label = @12
                            local.get 30
                            i64.const 3
                            i64.shl
                            i32.wrap_i64
                            i64.const 11293186
                            i64.store
                            local.get 30
                            i64.const 1
                            i64.add
                            local.set 30
                          else
                            local.get 22
                            i64.const 60
                            i64.eq
                            i64.extend_i32_s
                            i32.wrap_i64
                            if  ;; label = @13
                              local.get 30
                              i64.const 3
                              i64.shl
                              i32.wrap_i64
                              i64.const 11293442
                              i64.store
                              local.get 30
                              i64.const 1
                              i64.add
                              local.set 30
                            else
                              local.get 22
                              i64.const 62
                              i64.eq
                              i64.extend_i32_s
                              i32.wrap_i64
                              if  ;; label = @14
                                local.get 30
                                i64.const 3
                                i64.shl
                                i32.wrap_i64
                                i64.const 11293954
                                i64.store
                                local.get 30
                                i64.const 1
                                i64.add
                                local.set 30
                              else
                                local.get 22
                                i64.const 15421
                                i64.eq
                                i64.extend_i32_s
                                i32.wrap_i64
                                if  ;; label = @15
                                  local.get 30
                                  i64.const 3
                                  i64.shl
                                  i32.wrap_i64
                                  i64.const 11294466
                                  i64.store
                                  local.get 30
                                  i64.const 1
                                  i64.add
                                  local.set 30
                                else
                                  local.get 22
                                  i64.const 15933
                                  i64.eq
                                  i64.extend_i32_s
                                  i32.wrap_i64
                                  if  ;; label = @16
                                    local.get 30
                                    i64.const 3
                                    i64.shl
                                    i32.wrap_i64
                                    i64.const 11294978
                                    i64.store
                                    local.get 30
                                    i64.const 1
                                    i64.add
                                    local.set 30
                                  end
                                end
                              end
                            end
                          end
                        end
                      end
                    end
                  end
                end
              end
            end
          end
        end
        br 0 (;@2;)
      end
    end
    i64.const 7420
    local.set 36
    local.get 30
    local.set 37
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 4683747018559652103
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 68787306503
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 285920339230727
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 75171690140663815
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 30
    i64.const 3
    i64.shl
    i32.wrap_i64
    i64.const 1
    i64.store
    local.get 30
    i64.const 1
    i64.add
    local.set 30
    local.get 29
    local.set 38
    i64.const 0
    local.set 39
    block  ;; label = @1
      loop  ;; label = @2
        local.get 38
        local.get 30
        i64.lt_s
        i64.extend_i32_s
        i32.wrap_i64
        i32.eqz
        br_if 1 (;@1;)
        local.get 38
        local.get 31
        i64.eq
        i64.extend_i32_s
        i32.wrap_i64
        if  ;; label = @3
          i64.const 0
          local.set 39
        end
        local.get 38
        local.get 37
        i64.eq
        i64.extend_i32_s
        i32.wrap_i64
        if  ;; label = @3
          local.get 39
          local.set 40
          block  ;; label = @4
            loop  ;; label = @5
              local.get 40
              local.get 36
              i64.lt_s
              i64.extend_i32_s
              i32.wrap_i64
              i32.eqz
              br_if 1 (;@4;)
              i64.const 1
              call 3
              local.get 40
              i64.const 1
              i64.add
              local.set 40
              br 0 (;@5;)
            end
          end
        end
        local.get 38
        i64.const 3
        i64.shl
        i32.wrap_i64
        i64.load
        local.set 41
        local.get 38
        i64.const 1
        i64.add
        local.set 38
        local.get 41
        i64.const 255
        i64.and
        local.set 42
        local.get 42
        i64.const 255
        i64.eq
        i64.extend_i32_s
        i32.wrap_i64
        if  ;; label = @3
          local.get 38
          i64.const 3
          i64.shl
          i32.wrap_i64
          i64.load
          local.set 41
          local.get 38
          i64.const 1
          i64.add
          local.set 38
          block  ;; label = @4
            loop  ;; label = @5
              local.get 41
              i64.const 7
              i64.shr_u
              i64.const 0
              i64.ne
              i64.extend_i32_s
              i32.wrap_i64
              i32.eqz
              br_if 1 (;@4;)
              i64.const 128
              local.get 41
              i64.const 127
              i64.and
              i64.or
              call 3
              local.get 39
              i64.const 1
              i64.add
              local.set 39
              local.get 41
              i64.const 7
              i64.shr_u
              local.set 41
              br 0 (;@5;)
            end
          end
          local.get 41
          i64.const 255
          i64.and
          call 3
          local.get 39
          i64.const 1
          i64.add
          local.set 39
        else
          block  ;; label = @4
            loop  ;; label = @5
              local.get 42
              i64.const 0
              i64.gt_s
              i64.extend_i32_s
              i32.wrap_i64
              i32.eqz
              br_if 1 (;@4;)
              local.get 41
              i64.const 8
              i64.shr_u
              local.set 41
              local.get 41
              i64.const 255
              i64.and
              call 3
              local.get 39
              i64.const 1
              i64.add
              local.set 39
              local.get 42
              i64.const 1
              i64.sub
              local.set 42
              br 0 (;@5;)
            end
          end
        end
        br 0 (;@2;)
      end
    end)
  (memory (;0;) 1)
  (export "memory" (memory 0))
  (export "_start" (func 4))
  (data (;0;) (i32.const 0) "\10\00\00\00")
  (data (;1;) (i32.const 4) "\01\00\00\00")
  (data (;2;) (i32.const 16) "\00"))
