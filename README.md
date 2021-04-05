# Java Compiler
Compiler for Marist Spring 2021 Compilers Course

## Clone Repository
`git clone https://github.com/emilydoran1/Java-Compiler-Doran.git` 

## Running Parser and CST
* Redirect to the filepath `Java-Compiler-Doran/src`
* Compile Parser.java `javac Parser.java`
* Compile Compiler.java `javac Compiler.java`
* Run program with test cases as parameter `java Compiler pathToFile` 
  * ex: `java Compiler ../testCases/Parser/basicParse.txt`
* The compiler will call the lexer to provide the lexical analysis.
* After generating the token sequence for a program, the lexer will call the parser to verify the tokens.
  * If the lexer produced any errors, the parsing and CST will be skipped.
  * If the parser produces any errors, the CST will not be output.
* The program will prompt asking if you would like to run in verbose test mode. Respond Y/N.
  * If running in verbose test mode, you will see the detailed token output for each program, detailed parser stage tracing, as well as warnings, error messages, and messages letting you know if the program passed or failed each stage. 
 ```
 Parser/Lexer Output For Program: {}$
 Output in Verbose Test Mode:
     INFO  Lexer - Lexing program 1...
     DEBUG Lexer - T_L_BRACE [ { ] found at (1:1)
     DEBUG Lexer - T_R_BRACE [ } ] found at (1:2)
     DEBUG Lexer - T_EOP [ $ ] found at (1:3)
     INFO  Lexer - Lex completed with 0 errors

     PARSER: Parsing program 1 ...
     PARSER: parse()
     PARSER: parseProgram()
     PARSER: parseBlock()
     PARSER: parseStatementList()
     PARSER: Parse completed successfully

     CST for program 1 ...
     <Program> 
     -<Block> 
     --[{]
     --[StatementList]
     --[}]
     -[$]
```
 * If not running in verbose test mode, you will only see messages letting you know when you are beginning to lex and parse a new program, the CST (if no errors are thrown), any error messages or warnings, and a message letting you know if the program passed or failed each stage. Each token and parser stage tracing will not be printed out.
```
 Parser/Lexer Output For Program: {int int}$
 Output in Verbose Test Mode:
     INFO  Lexer - Lexing program 1...
     INFO  Lexer - Lex completed with 0 errors

     PARSER: Parsing program 1 ...
     PARSER: ERROR: Expected [id] got 'int' on line 1
     PARSER: Parse failed with 1 error(s)

     CST for program 1: Skipped due to PARSER error(s)
```
### Sample Test Cases
* Sample test case .txt files can be found in the testCases folder. Programs that throw parse-specific errors can be found within the testCases/Parser folder
* A PDF with a write up of the testing results can also be found in the testCases/Parser folder. The document is titled ParserTestingResults.pdf

## Running Lexical Analysis 
* Redirect to the filepath `Java-Compiler-Doran/src`
* Compile Compiler.java `javac Compiler.java`
* Run program with test cases as parameter `java Compiler pathToFile` 
  * ex: `java Compiler ../testCases/testCasesLexWithoutSpaces.txt`
* The compiler will call the lexer to provide the lexical analysis
* The program will prompt asking if you would like to run in verbose test mode. Respond Y/N.
  * If running in verbose test mode, you will see the detailed token output for each program as well as warnings, error messages, and a message letting you know if the program passed or failed. 
 ```
 Lexical Analysis for program: {}$
 Output in Verbose Test Mode:
     INFO  Lexer - Lexing program 1...
     DEBUG Lexer - T_L_BRACE [ { ] found at (1:1)
     DEBUG Lexer - T_R_BRACE [ } ] found at (1:2)
     DEBUG Lexer - T_EOP [ $ ] found at (1:3)
     INFO  Lexer - Lex completed with 0 errors 
```
  * If not running in verbose test mode, you will only see messages letting you know when you are beginning a new program, any error messages or warnings, and a message letting you know if the program passed or failed. Each token will not be printed out.
 ```
 Lexical Analysis for program: {/* comments are still ignored */  int @}$
 Output Not in Verbose Test Mode:
     INFO  Lexer - Lexing program 1...
     ERROR Lexer - Error: 1:40 Unrecognized Token: @
     ERROR Lexer - Lex failed with 1 error(s)
```
### Sample Test Cases
* Sample test case .txt files can be found in the testCases folder
* A PDF with a write up of the testing results can also be found in the testCases folder. The document is titled LexerTestingResults.pdf
