package main

import (
    "flag"
    "fmt"
    "time"
    "strconv"
)

// ===== Interfaces =====

type Hashable interface {
    Hash() int32
}

type Expression interface {
    Hashable
    Eval() int32
}

type LeftValue interface {
    Expression
    Store(v int32) int32
}

type Statement interface {
    Hashable
    Execute() int32
}

// ===== Benchmark =====

func main() {
    fmt.Println("Go")
    size := 20_000_000
    flag.Parse()
    if flag.NArg() > 0 {
        size, _ = strconv.Atoi(flag.Arg(0))
    }
    expressions := make([]Expression, 0, size)
    statements := make([]Statement, 0, size)

    for i := 0; i < size; i++ {
        var expr Expression

        switch random(int32(i), 9) {
        case 0:
            expr = &Variable{value: int32(random(int32(i), 1000))}
        case 1:
            expr = &NumberLiteral{}
        case 2:
            expr = &StringLiteral{}
        case 3:
            expr = &NewExpr{}
        case 4:
            expr = &NullValue{}
        case 5:
            expr = &ArrayAccess{}
        case 6:
            expr = &Call{}
        case 7:
            expr = &Operation{}
        case 8:
            expr = &Comparison{}
        default:
            panic("invalid")
        }

        expressions = append(expressions, expr)

        var stmt Statement
        switch random(int32(i + size), 8) {
        case 0:
            stmt = &IfStmt{}
        case 1:
            stmt = &WhileStmt{}
        case 2:
            stmt = &DoWhileStmt{}
        case 3:
            stmt = &Assignment{value: int32(random(int32(i), 2000))}
        case 4:
            stmt = &SwitchStmt{}
        case 5:
            stmt = &ReturnStmt{}
        case 6:
            stmt = &BreakStmt{}
        case 7:
            stmt = &ContinueStmt{}
        default:
            panic("invalid")
        }

        statements = append(statements, stmt)
    }

    for test := 0; test < 3; test++ {
        start := time.Now()

        var dummy int64 = run(expressions, statements)

        duration := time.Since(start)
        fmt.Printf("Run %d: %d ms; dummy: %d (Go)\n",
            test,
            duration.Milliseconds(),
            dummy)
    }
}

func run(expressions []Expression, statements []Statement) int64 {
    var sum int64

    for i := 0; i < len(expressions); i++ {
        sum += int64(expressions[i].Eval())
        sum += int64(expressions[i].Hash())
    }
    for i := 0; i < len(statements); i++ {
        sum += int64(statements[i].Execute())
        sum += int64(statements[i].Hash())
    }

    return sum
}

// ===== Random =====

func random(x int32, max uint32) int {
    ux := uint32(x)
    ux = ((ux >> 16) ^ ux) * 0x45d9f3b
    ux = ((ux >> 16) ^ ux) * 0x45d9f3b
    ux = ((ux >> 16) ^ ux) & 0x0fffffff
    return int(ux % max)
}

// ===== Expression Implementations =====

type Variable struct {
    value int32
}

func (v *Variable) Hash() int32 { return v.value + 11 }
func (v *Variable) Eval() int32 { return v.value }
func (v *Variable) Store(val int32) int32 {
    v.value = val
    return val
}

type NumberLiteral struct{}
func (n *NumberLiteral) Hash() int32 { return 12 }
func (n *NumberLiteral) Eval() int32 { return 2 }

type StringLiteral struct{}
func (s *StringLiteral) Hash() int32 { return 13 }
func (s *StringLiteral) Eval() int32 { return 3 }

type NewExpr struct{}
func (n *NewExpr) Hash() int32 { return 14 }
func (n *NewExpr) Eval() int32 { return 4 }

type NullValue struct{}
func (n *NullValue) Hash() int32 { return 15 }
func (n *NullValue) Eval() int32 { return 5 }

type ArrayAccess struct{}
func (a *ArrayAccess) Hash() int32 { return 16 }
func (a *ArrayAccess) Eval() int32 { return 6 }

type Call struct{}
func (c *Call) Hash() int32 { return 17 }
func (c *Call) Eval() int32 { return 7 }

type Operation struct{}
func (o *Operation) Hash() int32 { return 18 }
func (o *Operation) Eval() int32 { return 8 }

type Comparison struct{}
func (c *Comparison) Hash() int32 { return 19 }
func (c *Comparison) Eval() int32 { return 9 }

// ===== Statement Implementations =====

type IfStmt struct{}
func (i *IfStmt) Hash() int32    { return 31 }
func (i *IfStmt) Execute() int32 { return 21 }

type WhileStmt struct{}
func (w *WhileStmt) Hash() int32    { return 32 }
func (w *WhileStmt) Execute() int32 { return 22 }

type DoWhileStmt struct{}
func (d *DoWhileStmt) Hash() int32    { return 33 }
func (d *DoWhileStmt) Execute() int32 { return 23 }

type Assignment struct {
    value int32
}
func (a *Assignment) Hash() int32    { return a.value + 34 }
func (a *Assignment) Execute() int32 { return a.value + 24 }
func (a *Assignment) Eval() int32    { return a.value + 25 }

type SwitchStmt struct{}
func (s *SwitchStmt) Hash() int32    { return 36 }
func (s *SwitchStmt) Execute() int32 { return 26 }

type ReturnStmt struct{}
func (r *ReturnStmt) Hash() int32    { return 37 }
func (r *ReturnStmt) Execute() int32 { return 27 }

type BreakStmt struct{}
func (b *BreakStmt) Hash() int32    { return 38 }
func (b *BreakStmt) Execute() int32 { return 28 }

type ContinueStmt struct{}
func (c *ContinueStmt) Hash() int32    { return 39 }
func (c *ContinueStmt) Execute() int32 { return 29 }