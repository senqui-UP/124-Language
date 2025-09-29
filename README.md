# 124-Language
>>> Authors: Hansen Maeve Quindao & EJ Tolentino Jr.

This is the [MINECRAFT COMMAND] programming language
Token ( type = IDENTIFIER , lexeme = This , literal = null , line =1)
Token ( type = IDENTIFIER , lexeme = is , literal = null , line =1)
Token ( type = IDENTIFIER , lexeme = the , literal = null , line =1)
Token ( type = IDENTIFIER , lexeme = ktlox , literal = null , line =1)
Token ( type = IDENTIFIER , lexeme = programming , literal = null , line =1)
Token ( type = IDENTIFIER , lexeme = language , literal = null , line =1)
Token ( type = EOF , lexeme = , literal = null , line =1)

> var someString = " I am scanning "
Token ( type = VAR , lexeme = var , literal = null , line =1)
Token ( type = IDENTIFIER , lexeme = someString , literal = null , line =1)
Token ( type = EQUAL , lexeme == , literal = null , line =1)
Token ( type = STRING , lexeme =" I am scanning " , literal = I am scanning , line =1)
Token ( type = EOF , lexeme = , literal = null , line =1)

> var someNumber = 3.1415 + 6.9420
Token ( type = VAR , lexeme = var , literal = null , line =1)
Token ( type = IDENTIFIER , lexeme = someNumber , literal = null , line =1)
Token ( type = EQUAL , lexeme == , literal = null , line =1)
Token ( type = NUMBER , lexeme =3.1415 , literal =3.1415 , line =1)
Token ( type = PLUS , lexeme =+ , literal = null , line =1)
Token ( type = NUMBER , lexeme =6.9420 , literal =6.942 , line =1)
Token ( type = EOF , lexeme = , literal = null , line =1)

> // This is a comment , so it is ignored
Token ( type = EOF , lexeme = , literal = null , line =1)

> var parenthesizedEquation = (1 + 3) - (2 * 5)
Token ( type = VAR , lexeme = var , literal = null , line =1)
Token ( type = IDENTIFIER , lexeme = parenthesizedEquation , literal = null , line =1)
Token ( type = EQUAL , lexeme == , literal = null , line =1)
Token ( type = LEFT_PAREN , lexeme =( , literal = null , line =1)
Token ( type = NUMBER , lexeme =1 , literal =1.0 , line =1)
Token ( type = PLUS , lexeme =+ , literal = null , line =1)
Token ( type = NUMBER , lexeme =3 , literal =3.0 , line =1)
Token ( type = RIGHT_PAREN , lexeme =) , literal = null , line =1)
Token ( type = MINUS , lexeme = - , literal = null , line =1)
Token ( type = LEFT_PAREN , lexeme =( , literal = null , line =1)
Token ( type = NUMBER , lexeme =2 , literal =2.0 , line =1)
Token ( type = STAR , lexeme =* , literal = null , line =1)
Token ( type = NUMBER , lexeme =5 , literal =5.0 , line =1)
Token ( type = RIGHT_PAREN , lexeme =) , literal = null , line =1)
Token ( type = EOF , lexeme = , literal = null , line =1)

> /* This is a C - style docstring */
Token ( type = EOF , lexeme = , literal = null , line =1)
>
