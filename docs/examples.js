// Bau Language Examples
const bauExamples = [
    {
        value: 'factorial',
        label: 'Factorial',
        code: `fun factorial(x int) int
    if x <= 1
        return 1
    return x * factorial(x - 1)

for i:= range(0 20)
    println(factorial(i))
`
    },
    {
        value: 'conditionsLoops',
        label: 'Conditions and Loops',
        code: `a := 1
if a = 0
    println('zero')
elif a = 1
    println('one')
else
    println('many')
    
# prints 1 to 4
for i := range(0 10)
    break i = 5
    println(i)
`
    },
    {
        value: 'switch',
        label: 'Switch',
        code: `import org.bau.Utils

for i := range(1, 10)
    switch Utils.random() & 7
    case 0
        println('zero')
    case 1
        println('one')
    case 2, 3
        println('2 or 3')
    else  
        println('other') 
`
    },
    {
        value: 'typesAndConstants',
        label: 'Types and Constants',
        code: `PI : 3.1415
println('Pi: ' PI)

a := 10_000_000
b := 3
println('integer a/b: ', a / b)
println('float   a/b: ', float(a) / b)
`
    },
    {
        value: 'functionsOnTypes',
        label: 'Functions on Types',
        code: `fun int square() int
    return this * this

println(12.square())
`
    },
    {
        value: 'types',
        label: 'Types',
        code: `type point
    x int
    y int

p := point()
p.x = 10
p.y = 20
println('x=' p.x ' y=' p.y)
`
    },
    {
        value: 'arrays',
        label: 'Arrays',
        code: `data : i8[10]
if data.len
    i := 0..data.len
    while
        data[i]! = i * i
        next : i + 1
        break next >= data.len
        i = next
 for j := until(data.len)
    println('#' j ' = ' data[j])
`
    },
    {
        value: 'list',
        label: 'List',
        code: `import org.bau.List
    List
    newList

list := newList(int)
list.add(100)
list.add(80)
println('size: ' list.size)
println('[0]:  ' list.array[0])
`
    },
    {
        value: 'enum',
        label: 'Enums',
        code: `enum weekday
    sunday
    monday
    tuesday
    wednesday
    thursday
    friday
    saturday

for a := until(weekday.saturday + 1)
    switch a
    case weekday.sunday
        println('sunday')
    case weekday.monday
        println('monday')
    else
        println('some other day: #' a)
`
    },
    {
        value: 'exceptions',
        label: 'Exceptions',
        code: `import org.bau.Exception
    exception

fun square(x int) int throws exception
    if x > 3_000_000_000
        throw exception('Too big')
    return x * x
    
for i := range(2_999_999_999, 3_000_000_002)
    x := square(i)
    println(i '^2 = ' x)
    catch e
      println(i '^2 = ' e.message)
`
    },
    {
        value: 'macros',
        label: 'Macros',
        code: `fun if(cond int, a T, b T) macro T
    if cond
        return a
    else
        return b

fun processMessage(a i8[]) i8[]
    println('process message: ' a)
    return a

for i := until(2)
    x : if(i, processMessage('not zero'), processMessage('zero'))
    println(i ': ' x)
`
    },
    {
        value: 'sort',
        label: 'Sort with Templates',
        code: `fun insertionSort(a T[])
    for i := range(1, a.len)
        t := a[i]
        j := i - 1
        while j >= 0 and a[j] > t
            a[j + 1] = a[j]
            j -= 1
        a[j + 1] = t

import org.bau.Utils
x : int[10]
for i := until(x.len)
    x[i] = Utils.random()
insertionSort(x)
for i := until(x.len)
    println('x[' i '] = ' x[i])

y : float[10]
for i := until(y.len)
    y[i] = float(Utils.random()) / 1_000_000_000_000
insertionSort(y)
for i := until(y.len)
    println('y[' i '] = ' y[i])
`
    }
];

// Function to populate the examples dropdown
function populateExamples() {
    const select = document.getElementById('example');
    if (!select) return;
    
    // Clear existing options
    select.innerHTML = '';
    
    // Add each example as an option
    bauExamples.forEach(example => {
        const option = document.createElement('option');
        option.value = example.value;
        option.textContent = example.label;
        select.appendChild(option);
    });
    
    // Set the first example as selected (but don't load it yet)
    if (bauExamples.length > 0) {
        select.value = bauExamples[0].value;
    }
}

// Updated example function
function example(obj) {
    const selectedExample = bauExamples.find(ex => ex.value === obj.value);
    if (!selectedExample) return;
    
    const text = selectedExample.code;
    const source = document.getElementById("source");
    source.value = text;
    
    const width = (window.innerWidth > 0) ? window.innerWidth : document.documentElement.clientWidth;
    if (width > 900) {
        const codeMirror = document.querySelector('.CodeMirror');
        if (codeMirror && codeMirror.CodeMirror) {
            codeMirror.CodeMirror.setValue(text);
        }
    }
} 