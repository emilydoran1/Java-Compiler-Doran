# Java Compiler
Compiler for Marist Spring 2021 Compilers Course

## Clone Repository
`git clone https://github.com/emilydoran1/Java-Compiler-Doran.git` 

## Running Lexical Analysis 
* Redirect to the filepath `Java-Compiler-Doran/src`
* Compile Compiler.java `javac Compiler.java`
* Run program with test cases as parameter `java Compiler pathToFile` 
  * ex: `java Compiler ../testCasesLexWithoutSpaces.txt`
* The program will prompt asking if you would like to run in verbose test mode. Respond Y/N.
  * If running in verbose test mode, you will see the detailed token output for each program as well as warnings, error messages, and a message letting you know if the program passed or failed. 
 ```
 ex: INFO  Lexer - Lexing program 1...
     DEBUG Lexer - T_L_BRACE [ { ] found at (1:1)
     DEBUG Lexer - T_R_BRACE [ } ] found at (1:2)
     DEBUG Lexer - T_EOP [ $ ] found at (1:3)
     INFO  Lexer - Lex completed with 0 errors 
```
  * If not running in verbose test mode, you will only see messages letting you know when you are beginning a new program, any error messages or warnings, and a message letting you know if the program passed or failed. Each token will not be printed out.
