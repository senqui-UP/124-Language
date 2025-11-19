# PyCraft
## Creators
Hansen Maeve Quindao & Edmundo Tolentino Jr


## Language Overview
The PyCraft Programming language is a Python-inspired language designed to mimic how command lines work in Minecraft. Its purpose is to serve as an experimental language and expand on how Minecraft commands already function. It serves as a gamified version of the Python language, encouraging those familiar with Minecraft to engage in programming and explore coding. AS much as the language is built on the foundations of Minecraft, some coding conventions override these rules for better readability.


## Syntaxes
import declare &emsp;&nbsp; ```.mcmeta <import>```  
 &emsp; &emsp; &emsp;&emsp;&emsp; &emsp; &emsp; ```.mcmeta <specific_function> from <import>```  
 &emsp; &emsp; &emsp;&emsp;&emsp; &emsp; &emsp; ```.mcmeta <import> as <#alias>```     
function declare &emsp; ```/function <function_name>(<parameters>) { #code here } ```     
variable declare &emsp; ```/summon <type> <@var>```    
 &emsp; &emsp; &emsp;&emsp;&emsp; &emsp; &emsp; ```/summon <type> <@var> <value>```    
 &emsp; &emsp; &emsp;&emsp;&emsp; &emsp; &emsp; ```/summon const <type> <@var> <value>```    
assignment stmt	&emsp; ```/set <expr>```    
	if &emsp;&emsp;&ensp; ```/execute if <expr> run { #code here }```    
	if else	&ensp; ```/execute if <expr> run { #code here } ```    
 &emsp; &emsp; &emsp; ```/execute else run { #code here }```  
	elif &emsp;&emsp; ```/execute if <expr> run { #code here }```  
 &emsp; &emsp; &emsp; ```/execute elif <expr> run { #code here }```  
 &emsp; &emsp; &emsp; ```/execute else run { #code here }```  
for	&emsp;&emsp;&emsp; ```/execute for <@var> in range (<expr>) run { #code here }```  
while &emsp;&emsp; ```/execute while <expr> run { #code here }```  
input stmt &nbsp;```/source <@input>```  
 &emsp;&emsp;&emsp;&emsp;&emsp; ```/source <type> <@input>```   
 &emsp;&emsp;&emsp;&emsp;&emsp; ```/source <@input> <optional string>```   
output stmt&ensp;```/say #string of text here```   
 &emsp;&emsp;&emsp;&emsp;&emsp;&ensp; ```/say <string here> {<@var>}  #string here```    
comment	&emsp; ```/whisper #string of text here```  
return &emsp;&ensp;&emsp; ```/return <expr>```   
break &emsp;&emsp; &ensp; ```/stop```   
continue &emsp;&nbsp; ```/skip```  
kill program ```/kill``` 

## Other Keywords
int, float, double, bool, char, String  
true, false, null, const  
run, in, range, as, from, in not in, is, is not, and, or, not  
@a, @s  

## Syntax Style
- all keywords start with "/" to mimic a command prompt; except import, which is inspired by one of the meta data files of Minecraft
- uses {} for code blocks, taken from Minecraft
- indentation as statement separators due to no use of semicolons
- Comments are single line only, nod to linearity of command blocks
- Division uses $$ to provide less confusion with slash commands

## Operators
_same as Python, but divide is \$_   
**Arithmetic:** +, -, &ast;, \$, \$\$, %, &ast;&ast;  
**Comparison:** ==, !=, <, <=, >, >=  
**Logical:** and, or, not  
**Inc/Dec:** ++, -- (postfix only)  
**Assignment:** =, +=, -=, &ast;=, $=, $$=, %=, &ast;&ast;=  
**Membership:** in, not in  
**Identity:** is, is not  
**Bitwise:** &, |, ^, ~, <<, >>  

## Identifiers
**Syntax**: ```@<id name>```   
_ex: @x, @wooden_sword, @variable, @function_name_   
**Special Variable Calls**     
&emsp; ```@a[@variable]```	global variable   
&emsp; ```@s[@variable]```	static variable  
- Case Sensitive
- Highly advised to use snake_case for naming variables as a nod to the game
- Name limit of 50 characters, similar to the game
- @a and @s are optional, will parse code as if it is local to the block it is in
- However, function and import names are called using #<id_name>			

## Literals
**Strings**  
- all literals are automatically assumed to be strings
- String Interpolation: variables and functions must be inside {} when in a string
  - _example: ```/say {@var}``` or ```/say {@function}```_

**Numbers**    
- when declared as an int/float/double in a variable declaration statement
- otherwise, type converted from string

## Type Conversion Rules
- **Weak but Dynamic Typing** based on Minecraft
- Default literal type are Strings
- **Implicit Conversion**
- Arithmetic Conversion:
  - if both can be parsed as numeric, convert to numeric
    - Stringed num + Stringed num
    - Stringed num + numeric
  - if one cannot, treat as concatenation
    - String + numeric = String
    - Hello + int(5) = Hello5
- Boolean Conversion:
  - 1 and 0 can be converted as boolean True and False respectively
  - used in comparison and control statements
- Loose (==) and Strict (===) Equality Comparisons
- Conversion Precedence: Boolean → Numeric → String

## Errors
```Unknown command```
Syntax error: ```unexpected ... at ....```
In line errors:  ```<--[HERE] at its end.```


## Sample Code
**Variable Declaration, Arithmetic, Output **  
x = 1  
y = 2  
x = y + 4  
print {“X is equal to: “, x}  

```
/whisper declare variables
/summon int @x 1
/summon int @y 2
/execute @x = @y + 4
/say X is equal to {@x}
```


**Recursive Function, If Else, For, Input**  
```
/whisper Fibonacci Sequence  
/function #fibonacci(@n) {  
	/execute if @n <= 1 run {  
		/return @n  
	}  
	/execute else run {  
		/return {#fibonacci(@n - 1)} + {#fibonacci(@n - 2)}  
	}  
}  
/source int @n Input How Many Fibonacci to Print:   
/execute for @i in range(@n) {  
	/say Fibonacci Sequence: {#fibonacci(@n)}  
}
```
