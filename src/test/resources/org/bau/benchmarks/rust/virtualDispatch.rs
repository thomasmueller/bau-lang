use std::time::Instant;

const SIZE: usize = 1_000_000;
const ITERATIONS: usize = 20;

fn main() {
    let mut expressions: Vec<Box<dyn Expression>> = Vec::with_capacity(SIZE);
    let mut statements: Vec<Box<dyn Statement>> = Vec::with_capacity(SIZE);

    for i in 0..SIZE {
        let expr: Box<dyn Expression> = match random(i as i32, 9) {
            0 => Box::new(Variable { value: random(i as i32, 1000) as i32 }),
            1 => Box::new(NumberLiteral),
            2 => Box::new(StringLiteral),
            3 => Box::new(NewExpr),
            4 => Box::new(NullValue),
            5 => Box::new(ArrayAccess),
            6 => Box::new(Call),
            7 => Box::new(Operation),
            8 => Box::new(Comparison),
            _ => panic!()
        };
        expressions.push(expr);

        let stmt: Box<dyn Statement> = match random((i + SIZE) as i32, 8) {
            0 => Box::new(IfStmt),
            1 => Box::new(WhileStmt),
            2 => Box::new(DoWhileStmt),
            3 => Box::new(Assignment),
            4 => Box::new(SwitchStmt),
            5 => Box::new(ReturnStmt),
            6 => Box::new(BreakStmt),
            7 => Box::new(ContinueStmt),
            _ => panic!()
        };
        statements.push(stmt);
    }

    for test in 0..5 {
        let start = Instant::now();

        let mut dummy: i64 = 0;
        for _ in 0..ITERATIONS {
            dummy += run(&expressions, &statements);
        }

        let duration = start.elapsed();
        println!("Run {}: {:?} ms; dummy: {}",
                 test,
                 duration.as_millis(),
                 dummy);
    }
}


// ===== Benchmark =====

fn run(expressions: &[Box<dyn Expression>],
       statements: &[Box<dyn Statement>]) -> i64 {

    let mut sum: i64 = 0;

    for i in 0..expressions.len() {
        let mut j = random(i as i32, expressions.len() as u32);
        sum += expressions[j].eval() as i64;
        sum += statements[j].execute() as i64;
        j = random(-(i as i32), expressions.len() as u32);
        sum += expressions[j].hash() as i64;
        sum += statements[j].hash() as i64;
    }

    sum
}

fn random(x: i32, max: u32) -> usize {
    // https://stackoverflow.com/questions/664014/what-integer-hash-function-are-good-that-accepts-an-integer-hash-key
    let mut ux = x as u32;
    ux = ((ux >> 16) ^ ux).wrapping_mul(0x45d9f3b);
    ux = ((ux >> 16) ^ ux).wrapping_mul(0x45d9f3b);
    ux = ((ux >> 16) ^ ux) & 0xfffffffu32;
    // println!("result {}", (ux % max) as usize);
    (ux % max) as usize
}

// ===== Traits =====

trait Hashable {
    fn hash(&self) -> i32;
}

trait Expression: Hashable {
    fn eval(&self) -> i32;
}

trait LeftValue: Expression {
    fn store(&mut self, v: i32) -> i32;
}

trait Statement: Hashable {
    fn execute(&self) -> i32;
}

// ===== Expression Implementations =====

struct Variable {
    value: i32,
}

impl Hashable for Variable {
    fn hash(&self) -> i32 { self.value + 11 }
}

impl Expression for Variable {
    fn eval(&self) -> i32 { self.value }
}

impl LeftValue for Variable {
    fn store(&mut self, v: i32) -> i32 {
        self.value = v;
        v
    }
}

struct NumberLiteral;
impl Hashable for NumberLiteral { fn hash(&self) -> i32 { 12 } }
impl Expression for NumberLiteral { fn eval(&self) -> i32 { 2 } }

struct StringLiteral;
impl Hashable for StringLiteral { fn hash(&self) -> i32 { 13 } }
impl Expression for StringLiteral { fn eval(&self) -> i32 { 3 } }

struct NewExpr;
impl Hashable for NewExpr { fn hash(&self) -> i32 { 14 } }
impl Expression for NewExpr { fn eval(&self) -> i32 { 4 } }

struct NullValue;
impl Hashable for NullValue { fn hash(&self) -> i32 { 15 } }
impl Expression for NullValue { fn eval(&self) -> i32 { 5 } }

struct ArrayAccess;
impl Hashable for ArrayAccess { fn hash(&self) -> i32 { 16 } }
impl Expression for ArrayAccess { fn eval(&self) -> i32 { 6 } }

struct Call;
impl Hashable for Call { fn hash(&self) -> i32 { 17 } }
impl Expression for Call { fn eval(&self) -> i32 { 7 } }

struct Operation;
impl Hashable for Operation { fn hash(&self) -> i32 { 18 } }
impl Expression for Operation { fn eval(&self) -> i32 { 8 } }

struct Comparison;
impl Hashable for Comparison { fn hash(&self) -> i32 { 19 } }
impl Expression for Comparison { fn eval(&self) -> i32 { 9 } }

// ===== Statement Implementations =====

struct IfStmt;
impl Hashable for IfStmt { fn hash(&self) -> i32 { 31 } }
impl Statement for IfStmt { fn execute(&self) -> i32 { 21 } }

struct WhileStmt;
impl Hashable for WhileStmt { fn hash(&self) -> i32 { 32 } }
impl Statement for WhileStmt { fn execute(&self) -> i32 { 22 } }

struct DoWhileStmt;
impl Hashable for DoWhileStmt { fn hash(&self) -> i32 { 33 } }
impl Statement for DoWhileStmt { fn execute(&self) -> i32 { 23 } }

struct Assignment;
impl Hashable for Assignment { fn hash(&self) -> i32 { 34 } }
impl Statement for Assignment { fn execute(&self) -> i32 { self.hash() + 24 } }
impl Expression for Assignment { fn eval(&self) -> i32 { self.hash() + 25 } }

struct SwitchStmt;
impl Hashable for SwitchStmt { fn hash(&self) -> i32 { 36 } }
impl Statement for SwitchStmt { fn execute(&self) -> i32 { 26 } }

struct ReturnStmt;
impl Hashable for ReturnStmt { fn hash(&self) -> i32 { 37 } }
impl Statement for ReturnStmt { fn execute(&self) -> i32 { 27 } }

struct BreakStmt;
impl Hashable for BreakStmt { fn hash(&self) -> i32 { 38 } }
impl Statement for BreakStmt { fn execute(&self) -> i32 { 28 } }

struct ContinueStmt;
impl Hashable for ContinueStmt { fn hash(&self) -> i32 { 39 } }
impl Statement for ContinueStmt { fn execute(&self) -> i32 { 29 } }
