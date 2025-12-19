// Our backend parser looks for variables that are defined and declared but never used. If it finds any it underlines them with yellow underscore
val unusedVar = 42

val usedVar = 10
println("Used variable: $usedVar")

// There should be a red line that underlines unclosed string
//val broken = "unclosed string"
//println("Is the string finally closed? $broken")


// There should be red line that underlines function call for an undefined function
//undefinedFunction()

// Lets see the execution of this program
val numbers = listOf(1, 2, 3, 4, 5)
numbers.forEach { println(it) }

println("Test Complete!")

