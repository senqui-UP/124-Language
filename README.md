# [Your Programming Language Name]

## Creator

Authors: Hansen Maeve Quindao & EJ Tolentino


## Language Overview

The PyCraft Programming language is a Python-based language designed to mimic how command lines work in the game Minecraft. Its purpose is to serve as an experimental language and expand on how Minecraft commands already function. It serves as a gamified version of the Python language, encouraging those familiar with Minecraft to engage in programming and explore coding.

## Keywords

- /function	     _(for declaring functions)_
- /kill		       _(declaring the closure of a program)_
- /say		       _(print/output)_
- /input		     _(input)_
- /summon	       _(declaring a variable)_
- /expr		       _(declaring expressions)_
- /execute if	   _(if)_
- /execute for   _(for)_
- /execute while _(while)_
- run		         _(then statements)_
- /gamerule	     _(import)_
- /effect        _(return)_
- /team          _(class)_

## Operators

_same as Python_  
**Arithmetic:** +, -, *, $, $$, %, *  
**Comparison:** ==, !=, <, <=, >, >=  
**Logical:**    and, or, not  
**Assignment:** =, +=, -=, *=, $=, $$=, %=, **=  
**Membership:** in, not in  
**Identity:**   is, is not  
**Bitwise:**    &, |, ^, ~, <<, >>  

## Literals

[Describe the format and syntax for each type of literal value (e.g., numbers, strings, characters, etc.) your language supports]

## Identifiers

[Define the rules for valid identifiers (variable names, function names, etc.) and whether they are case-sensitive]

## Comments

[Describe the syntax for comments and whether nested comments are supported]

## Syntax Style

[Describe whether whitespace is significant, how statements are terminated, and what delimiters are used for blocks and grouping]

## Sample Code
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
  int var2 = 20  
  varname = var2  
  print(“varname is now ”, varname)  
else  
  varname = 0  
  print(“varname is now “, varname)*  


## Design Rationale
The rationale for our design choices was to make it similar to how Minecraft commands work. They already have their own syntaxes, but we expanded it to be more for coding and programming instead of just their functions in Minecraft. New commands/keywords are created so that it’s still easy to understand and not just simply obfuscating and changing keywords for the sake of changing them.
