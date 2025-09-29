<<<<<<< HEAD
# PyCraft
=======
# [PyCraft]
>>>>>>> 80393cabe322b899e0e3064525fdc62e1cab6ebe

## Creator
Authors: Hansen Maeve Quindao & EJ Tolentino
## Language Overview
The PyCraft Programming language is a Python-based language designed to mimic how command lines work in the game Minecraft. Its purpose is to serve as an experimental language and expand on how Minecraft commands already function. It serves as a gamified version of the Python language, encouraging those familiar with Minecraft to engage in programming and explore coding.


<<<<<<< HEAD
[List all reserved words that cannot be used as identifiers - include the keyword and a brief description of its purpose]
=======
## Keywords
- /function	     _(for declaring functions)_
- /kill		     _(declaring the closure of a program)_
- /say		     _(print/output)_
- /input	     _(input)_
- /summon	     _(declaring a variable)_
- /expr		     _(declaring expressions)_
- /execute if    _(if)_
- else			 _(else)_
- /execute for   _(for)_
- /execute while _(while)_
- run		     _(then statements)_
- /gamerule	     _(import)_
- /effect        _(return)_
- /team          _(class)_
>>>>>>> 80393cabe322b899e0e3064525fdc62e1cab6ebe


<<<<<<< HEAD
[List all operators organized by category (arithmetic, comparison, logical, assignment, etc.)]
=======
## Operators
_same as Python, but divide is $_  
**Arithmetic:** +, -, &ast;, $, $$, %, &ast;&ast;
**Comparison:** ==, !=, <, <=, >, >=  
**Logical:** and, or, not  
**Assignment:** =, +=, -=, &ast;=, $=, $$=, %=, &ast;&ast;=  
**Membership:** in, not in  
**Identity:** is, is not  
**Bitwise:** &, |, ^, ~, <<, >>  

>>>>>>> 80393cabe322b899e0e3064525fdc62e1cab6ebe

## Literals
Strings - all literals are automatically assumed to be strings  
Numbers - requires () to be recognized as a numeric value  


## Identifiers
- All identifiers will start with an @
- Case Sensitive
- Must start with a letter and is at most 50 characters

<<<<<<< HEAD
-Variable names start with @
-Variables names are case-sensitive
-Keywords can't be vaariables
=======
>>>>>>> 80393cabe322b899e0e3064525fdc62e1cab6ebe

## Comments
```
/whisper “the comment here
this way this kind of comment supports multiline”
```  

<<<<<<< HEAD
Comments follow the formatting /whisper “insert comment”
=======
>>>>>>> 80393cabe322b899e0e3064525fdc62e1cab6ebe

## Syntax Style
- Similar to Python: Indentation based, no semicolons
- Keywords will always start with /
- Curly brackets are used for grouping expressions and function calls


## Sample Code
<<<<<<< HEAD
=======
```/say hello world```     
*print(“hello world”)*  

```
/summon int @varname (10)  
/expr @varname {@varname*(2)}  
/say @varname  
```   
_int varname = 10  
varname *= 2  
print(varname) = 20_  

```
/execute if @varname==(10) run
	/summon int @var2 (20)
	/expr @varname {@var2}
    /say varname is now @varname
else
	/expr @varname {0}		
	/say varname is now @varname
```
*if (varname==10)  
&emsp;int var2 = 20  
&emsp;varname = var2  
&emsp;print(“varname is now ”, varname)  
else  
&emsp;varname = 0  
&emsp;print(“varname is now “, varname)*  
>>>>>>> 80393cabe322b899e0e3064525fdc62e1cab6ebe

[Provide a few examples of valid code in your language to demonstrate the syntax and features]

## Design Rationale

[Explain the reasoning behind your design choices]