# Java Compiler
Compiler for Marist Spring 2021 Compilers Course

## Clone Repository
`git clone https://github.com/emilydoran1/Java-Compiler-Doran.git` 

## Running Semantic Analysis
* Redirect to the filepath `Java-Compiler-Doran/src`
* Compile SemanticAnalyzer.java `javac SemanticAnalyzer.java`
* Compile Parser.java `javac Parser.java`
* Compile Compiler.java `javac Compiler.java`
* Compile SymbolTable.java `javac SymbolTable.java`
* Compile Scope.java `javac Scope.java`
* Run program with test cases as parameter `java Compiler pathToFile` 
  * ex: `java Compiler ../testCases/SemanticAnalysis/basic.txt`
* The compiler will call the lexer to provide the lexical analysis.
* After generating the token sequence for a program, the lexer will call the parser to verify the tokens.
  * If the lexer produced any errors, the parsing and CST will be skipped.
* The parser will call Semantic Analysis to do scope checking, type checking, generate an AST, and generate a symbol table.
  * If the parser produced any errors, the semantic analysis will be skipped.
  * If semantic analysis produces any errors, the AST and symbol table will be skipped.
* To Run program in verbose test mode, make sure line 17 in compiler.java is `Lexer lex = new Lexer(args[0], true);`
  * To change to non-verbose test mode, change line 17 in compiler.java to be `Lexer lex = new Lexer(args[0], false);`
  * If running in verbose test mode, you will see the detailed token output for each program, detailed parser stage tracing, semantic analysis debugging messages, as well as warnings, error messages, and messages letting you know if the program passed or failed each stage. 
```
 Output For Program: {int a}$
 Output in Verbose Test Mode:
    INFO  Lexer - Lexing program 1...
    DEBUG Lexer - T_L_BRACE [ { ] found at (1:1)
    DEBUG Lexer - T_VARIABLE_TYPE [ int ] found at (1:2)
    DEBUG Lexer - T_ID [ a ] found at (1:6)
    DEBUG Lexer - T_R_BRACE [ } ] found at (1:7)
    DEBUG Lexer - T_EOP [ $ ] found at (1:8)
    INFO  Lexer - Lex completed with 0 errors

    PARSER: Parsing program 1 ...
    PARSER: parse()
    PARSER: parseProgram()
    PARSER: parseBlock()
    PARSER: parseStatementList()
    PARSER: parseStatement()
    PARSER: parseVarDecl()
    PARSER: parseType()
    PARSER: parseStatementList()
    PARSER: Parse completed successfully

    CST for program 1 ...
    <Program> 
    -<Block> 
    --[{]
    --<StatementList> 
    ---<Statement> 
    ----<VarDecl> 
    -----<Type> 
    ------[int]
    -----<Id> 
    ------[a]
    --[}]
    -[$]

    SEMANTIC ANALYSIS: Beginning Semantic Analysis on Program 1 ...
    SEMANTIC ANALYSIS: New Scope [ 0 ] has been entered at line: 1.
    SEMANTIC ANALYSIS: Variable [ a ] has been declared at (1:2)
    SEMANTIC ANALYSIS: WARNING: Variable [ a ] is declared but never initialized or used.

    Program 1 Semantic Analysis produced 0 error(s) and 1 warning(s).

    AST for program 1 ...
    <BLOCK> 
    -<VariableDeclaration> 
    --[int]
    --[a]

    Program 1 Symbol Table
    ---------------------------
    Name  Type     Scope  Line
    ---------------------------
    a     int      0      1   
```
  * If not running in verbose test mode, you will only see messages letting you know when you are beginning to lex, parse, and perform semantic analysis on a new program, the CST (if no errors are thrown), the AST (if no errors are thrown), the symbol table (if no errors are thrown), any error messages or warnings, and a message letting you know if the program passed or failed each stage. Each token, parser stage tracing, and semantic analysis debugging variable and scope messages will not be printed out.
```
Output For Program: {print(b)x = 1}$
Output in Non-Verbose Test Mode:
   INFO  Lexer - Lexing program 1...
   INFO  Lexer - Lex completed with 0 errors

   PARSER: Parsing program 1 ...
   PARSER: Parse completed successfully

   CST for program 1 ...
   <Program> 
   -<Block> 
   --[{]
   --<StatementList> 
   ---<Statement> 
   ----<PrintStatement> 
   -----[print]
   -----[(]
   -----<Expression> 
   ------<Id> 
   -------[b]
   -----[)]
   ---<StatementList> 
   ----<Statement> 
   -----<AssignStatement> 
   ------<Id> 
   -------[x]
   ------[=]
   ------<Expression> 
   -------<IntegerExpression> 
   --------<Digit> 
   ---------[1]
   --[}]
   -[$]

   SEMANTIC ANALYSIS: Beginning Semantic Analysis on Program 1 ...
   SEMANTIC ANALYSIS: ERROR: Undeclared variable [ b ] was used at (1:8) before being declared.
   SEMANTIC ANALYSIS: ERROR: Undeclared variable [ x ] was assigned a value at (1:10) before being declared.
   SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ x ] of type [  ] was assigned to type [ int ] at (1:14).

   Program 1 Semantic Analysis produced 3 error(s) and 0 warning(s).

   AST for program 1: Skipped due to SEMANTIC ANALYSIS error(s)

   Symbol Table for program 1: Skipped due to SEMANTIC ANALYSIS error(s)
```

### Sample Test Cases
* Sample test case .txt files can be found in the testCases folder. Programs that throw Semantic Analysis-specific errors can be found within the testCases/SemanticAnalysis folder
* A PDF with a write up of the testing results can also be found in the testCases/Parser folder. The document is titled SemanticAnalysisTestingResults.pdf

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
* To Run program in verbose test mode, make sure line 17 in compiler.java is `Lexer lex = new Lexer(args[0], true);`
  * To change to non-verbose test mode, change line 17 in compiler.java to be `Lexer lex = new Lexer(args[0], false);`
  * If running in verbose test mode, you will see the detailed token output for each program, detailed parser stage tracing, as well as warnings, error messages, and messages letting you know if the program passed or failed each stage. 
 ```
 Output For Program: {}$
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
  * ex: `java Compiler ../testCases/Lexer/testCasesLexWithoutSpaces.txt`
* The compiler will call the lexer to provide the lexical analysis
* To Run program in verbose test mode, make sure line 17 in compiler.java is `Lexer lex = new Lexer(args[0], true);`
  * To change to non-verbose test mode, change line 17 in compiler.java to be `Lexer lex = new Lexer(args[0], false);`
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
* Sample test case .txt files can be found in the testCases folder. Programs that throw lex-specific errors can be found within the testCases/Lexer folder
* A PDF with a write up of the testing results can also be found in the testCases/Lexer folder. The document is titled LexerTestingResults.pdf