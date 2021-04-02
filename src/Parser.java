/**
 * This program validates the tokens produced by the lexical analysis and
 * displays errors and warnings according to our grammar. If the parse is
 * successful, it creates a Concrete Syntax Tree (CST).
 *
 * @author Emily Doran
 *
 */

import java.util.ArrayList;

public class Parser {
    private ArrayList<Token> tokens;
    private boolean verboseMode;
    private int tokIndex = 0;

    private ConcreteSyntaxTree cst = new ConcreteSyntaxTree();

    int errorCount = 0;

    public Parser(ArrayList<Token> tokens, boolean verboseMode, boolean passLex, int programNum) {
        this.tokens = tokens;
        this.verboseMode = verboseMode;

        if(passLex){
            System.out.println("\nPARSER: Parsing program " + programNum + " ...");
            parse();

            if(errorCount == 0) {
                System.out.println("PARSER: Parse completed successfully");
                System.out.println("\nCST for program " + programNum + " ...");
                System.out.println(cst.toString());
            }
            else{
                System.out.println("PARSER: Parse failed with " +  errorCount + " error(s)");
                System.out.println("\nCST for program " + programNum + ": Skipped due to PARSER error(s)");
            }
        }
        else{
            System.out.println("\nPARSER: Skipped due to LEXER error(s)");

            System.out.println("\nCST for program " + programNum + ": Skipped due to LEXER error(s)");
        }

    }

    /**
     * Calls parseProgram to begin parsing sequence
     */
    public void parse(){
        if(verboseMode)
            System.out.println("PARSER: parse()");

        parseProgram();

    }

    /**
     * Verifies that the token sequence is correct for a Program
     * Program ::== Block $
     * @return boolean passedProgram token sequence matches that of Program and there are
     * no errors in internal function calls
     */
    public boolean parseProgram(){
        boolean passedProgram = true;

        if(verboseMode)
            System.out.println("PARSER: parseProgram()");
        cst.addNode("Program","root");

        // check if there was an error in other grammar program call
        if(parseBlock()){
            if(checkToken("T_EOP"))
                cst.addNode("$", "child");
            else{
                throwErr("Expected [EOP] got '" + tokens.get(tokIndex).getValue());
            }
        }
        // error was thrown in other function, so we don't want to continue
        else{
            passedProgram = false;
        }

        return passedProgram;
    }

    /**
     * Verifies that the token sequence is correct for a Block
     * Block ::== { StatementList }
     * @return boolean passedBlock token sequence matches that of Block and there are
     * no errors in internal function calls
     */
    public boolean parseBlock(){
        boolean passedBlock = true;

        if(verboseMode)
            System.out.println("PARSER: parseBlock()");
        cst.addNode("Block","branch");

        // check that first token is left brace
        if(checkToken("T_L_BRACE")){
            cst.addNode("{","child");
            if(parseStatementList()){
                checkToken("T_R_BRACE");
                cst.addNode("}", "child");
            }
            // error was thrown in other function, so we don't want to continue
            else{
                passedBlock = false;
            }
        }
        // left brace was not entered, we don't have a block
        else{
            throwErr("Expected [{] got '" + tokens.get(tokIndex).getValue());
            passedBlock = false;
        }

        cst.moveParent();

        return passedBlock;
    }

    /**
     * Verifies that the token sequence is correct for a StatementList
     * StatementList ::== Statement StatementList
     *               ::== Epsilon
     * @return boolean passedStatementList token sequence matches that of StatementList and there are
     * no errors in internal function calls
     */
    public boolean parseStatementList(){
        boolean passedStatementList = true;

        if(verboseMode)
            System.out.println("PARSER: parseStatementList()");

        // we haven't reached end of stream and don't have right brace
        if(tokIndex < tokens.size() && tokens.get(tokIndex).getKind() != "T_R_BRACE"){
            cst.addNode("StatementList","branch");
            if(parseStatement()) {
                parseStatementList();
            }
            else{
                passedStatementList = false;
            }
            cst.moveParent();
        }
        // we haven't reached end of stream and have nothing in StatementList
        else if(tokIndex < tokens.size() && tokens.get(tokIndex).getKind() == "T_R_BRACE" && tokens.get(tokIndex-1).getKind() == "T_L_BRACE"){
            cst.addNode("StatementList","branch");
            cst.moveParent();
        }
        // we have reached end of stream
        else if(tokIndex >= tokens.size()){
            throwErr("Expected [StatementList] got 'end of stream");
            passedStatementList = false;
        }

        // check that we don't have any previous errors because we don't want to overwrite passedStatementList during
        // recursive calls
        if(errorCount > 0)
            passedStatementList = false;

        return passedStatementList;
    }

    /**
     * Verifies that the token sequence is correct for a Statement
     * Statement ::== PrintStatement | AssignStatement | VarDecl | WhileStatement | IfStatement | Block
     * @return boolean passedStatement token sequence matches that of Statement and there are
     * no errors in internal function calls
     */
    public boolean parseStatement(){
        boolean passedStatement = true;

        if(verboseMode)
            System.out.println("PARSER: parseStatement()");
        cst.addNode("Statement","branch");

        // we have a PrintStatement
        if(checkToken("T_PRINT")) {
            cst.addNode("PrintStatement","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            if(!parsePrintStatement()){
                passedStatement = false;
            }

        }
        // we have an AssignStatement (which begins with id)
        else if(checkToken("T_ID")) {
            // make sure parseAssignStatement() didn't throw any errors
            if(!parseAssignStatement()){
                passedStatement = false;
            }
        }
        // we have a VarDecl (which begins with var type)
        else if(checkToken("T_VARIABLE_TYPE")){
            // make sure parseVarDecl() didn't throw any errors
            if(!parseVarDecl()) {
                passedStatement = false;
            }
        }
        // we have a WhileStatement
        else if(checkToken("T_WHILE")) {
            // make sure parseWhileStatement() didn't throw any errors
            if(!parseWhileStatement()) {
                passedStatement = false;
            }
        }
        // we have an IfStatement
        else if(checkToken("T_IF")) {
            // make sure parseIfStatement() didn't throw any errors
            if (!parseIfStatement()) {
                passedStatement = false;
            }
        }
        // we have a Block (which begins with left brace)
        else if(tokens.get(tokIndex).getKind() == "T_L_BRACE"){
            // make sure parseBlock() didn't throw any errors
            if (!parseBlock()) {
                passedStatement = false;
            }
        }
        // current token does not match Statement -> throw error
        else {
            throwErr("Expected [PrintStatement, AssignStatement, VarDecl, WhileStatement, " +
                    "IfStatement, Block] got '" + tokens.get(tokIndex).getValue());
            passedStatement = false;
        }

        cst.moveParent();

        return passedStatement;
    }

    /**
     * Verifies that the token sequence is correct for a PrintStatement
     * PrintStatement ::== print ( Expr )
     * @return boolean passedPrintStatement token sequence matches that of PrintStatement and there are
     * no errors in internal function calls
     */
    public boolean parsePrintStatement(){
        boolean passedPrintStatement = true;

        if(verboseMode)
            System.out.println("PARSER: parsePrintStatement()");

        // we already matched print in previous function call to call this method, so next thing to match
        // is the left parenthesis
        if(checkToken("T_L_PAREN")) {
            cst.addNode("(","child");
            // make sure parseExpr() didn't throw any errors
            if(parseExpr()) {
                // match closing parenthesis
                if (checkToken("T_R_PAREN")) {
                    cst.addNode(")", "child");
                } else {
                    passedPrintStatement = false;
                    throwErr("Expected [)] got '" + tokens.get(tokIndex).getValue());
                }
            }
            // parseExpr() or other function calls threw an error
            else{
                passedPrintStatement = false;
            }
        }
        // we don't have a left parenthesis, throw error
        else{
            passedPrintStatement = false;
            throwErr("Expected [(] got '" + tokens.get(tokIndex).getValue());
        }
        cst.moveParent();

        return passedPrintStatement;
    }

    /**
     * Verifies that the token sequence is correct for an AssignStatement
     * AssignStatement ::== Id = Expr
     * @return boolean passedAssignStatement token sequence matches that of AssignStatement and there are
     * no errors in internal function calls
     */
    public boolean parseAssignStatement(){
        boolean passedAssignStatement = true;

        if(verboseMode)
            System.out.println("PARSER: parseAssignStatement()");
        cst.addNode("AssignStatement","branch");
        cst.addNode("Id","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        cst.moveParent();

        // we already matched Id in prev function, so next item to match is "="
        if(checkToken("T_ASSIGN_OP")) {
            cst.addNode(tokens.get(tokIndex - 1).getValue(), "child");
            // parseExpr() or other function calls threw an error
            if(!parseExpr())
                passedAssignStatement = false;
        }
        // current token is not "=", throw error
        else{
            throwErr("Expected [=] got '" + tokens.get(tokIndex).getValue());
            passedAssignStatement = false;
        }
        cst.moveParent();

        return passedAssignStatement;
    }

    /**
     * Verifies that the token sequence is correct for a Variable Declaration
     * VarDecl ::== type Id
     * @return boolean passedVarDecl token sequence matches that of VarDecl and there are
     * no errors in internal function calls
     */
    public boolean parseVarDecl(){
        boolean passedVarDecl = true;

        if(verboseMode)
            System.out.println("PARSER: parseVarDecl()");
        cst.addNode("VarDecl","branch");

        // we already matched that current token is type to get here, but call parseType()
        // to get the token printed
        parseType();

        // check that current token following type is Id
        if(checkToken("T_ID")){
            cst.addNode("Id","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
            cst.moveParent();
        }
        // token was not an Id -> throw error
        else{
            passedVarDecl = false;
            throwErr("Expected [id] got '" + tokens.get(tokIndex).getValue());
        }

        cst.moveParent();

        return passedVarDecl;
    }

    /**
     * Verifies that the token sequence is correct for a type
     * type ::== int | string | boolean
     */
    public void parseType(){
        if(verboseMode)
            System.out.println("PARSER: parseType()");
        cst.addNode("Type","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
        cst.moveParent();

    }

    /**
     * Verifies that the token sequence is correct for a While Statement
     * WhileStatement ::== while BooleanExpr Block
     * @return boolean passedWhileStatement token sequence matches that of WhileStatement and there are
     * no errors in internal function calls
     */
    public boolean parseWhileStatement(){
        boolean passedWhileStatement = true;

        if(verboseMode)
            System.out.println("PARSER: parseWhileStatement()");
        cst.addNode("WhileStatement","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(),"child");

        // we already matched "while", so check if we have a boolean expression next
        // if this is true, we check for block
        if(parseBooleanExpr()){
            if(!parseBlock()){
                passedWhileStatement = false;
            }
        }
        // there was an error in parseBooleanExpr() or other internal function calls
        else{
            passedWhileStatement = false;
        }
        cst.moveParent();

        return passedWhileStatement;
    }

    /**
     * Verifies that the token sequence is correct for an If Statement
     * IfStatement ::== if BooleanExpr Block
     * @return boolean passedIfStatement token sequence matches that of IfStatement and there are
     * no errors in internal function calls
     */
    public boolean parseIfStatement(){
        boolean passedIfStatement = true;

        if(verboseMode)
            System.out.println("PARSER: parseIfStatement()");
        cst.addNode("IfStatement","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(),"child");

        // we already matched "if", so check if we have a boolean expression next
        // if this is true, we check for block
        if(parseBooleanExpr()){
            if(!parseBlock())
                passedIfStatement = false;
        }
        // there was an error in parseBooleanExpr() or other internal function calls
        else{
            passedIfStatement = false;
        }
        cst.moveParent();

        return passedIfStatement;
    }

    /**
     * Verifies that the token sequence is correct for an Expression
     * Expr ::== IntExpr | StringExpr | BooleanExpr | Id
     * @return boolean passedExpr token sequence matches that of Expr and there are
     * no errors in internal function calls
     */
    public boolean parseExpr(){
        boolean passedExpr = true;

        if(verboseMode)
            System.out.println("PARSER: parseExpr()");
        cst.addNode("Expression", "branch");

        // check if we have an IntExpr
        if(checkToken("T_DIGIT")){
            if(!parseIntExpr())
                passedExpr = false;
        }
        // check if we have a StringExpr
        else if(checkToken("T_QUOTE")) {
            cst.addNode("StringExpression","branch");
            cst.addNode("\"","child");
            if(!parseStringExpr()){
                passedExpr = false;
            }
        }
        // check if we have an Id
        else if(checkToken("T_ID")){
            cst.addNode("Id","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
            cst.moveParent();
        }
        // check if we have a BooleanExpr
        else if(tokens.get(tokIndex).getKind().equals("T_L_PAREN") ||
                tokens.get(tokIndex).getKind().equals("T_BOOL_TRUE") ||
                tokens.get(tokIndex).getKind().equals("T_BOOL_FALSE")){
            parseBooleanExpr();
        }
        // we don't have any matches ->  throw error
        else{
            passedExpr = false;
            throwErr("Expected [IntExpr, StringExpr, BooleanExpr, Id] got '" + tokens.get(tokIndex).getValue());
        }

        cst.moveParent();

        return passedExpr;
    }

    /**
     * Verifies that the token sequence is correct for an Int Expression
     * IntExpr ::== digit intop Expr
     *         ::== digit
     * @return boolean passedIntExpr token sequence matches that of IntExpr and there are
     * no errors in internal function calls
     */
    public boolean parseIntExpr(){
        boolean passedIntExpr = true;

        if(verboseMode)
            System.out.println("PARSER: parseIntExpr()");
        cst.addNode("IntegerExpression", "branch");

        // we already matched the digit to get here, so check if next token is an intop
        // intop ::== +
        if(tokens.get(tokIndex).getKind().equals("T_ADDITION_OP")) {
            cst.addNode("Digit","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
            cst.moveParent();
            checkToken("T_ADDITION_OP");
            cst.addNode("IntOp","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
            cst.moveParent();
            // make sure no errors were thrown in parseExpr
            if(!parseExpr())
                passedIntExpr = false;

        }
        // we do not have an intop, so add node for just digit
        else{
            cst.addNode("Digit","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
            cst.moveParent();
        }
        cst.moveParent();

        return passedIntExpr;
    }

    /**
     * Verifies that the token sequence is correct for a String Expression
     * StringExpr ::== " CharList "
     * @return boolean passedStringExpr token sequence matches that of StringExpr and there are
     * no errors in internal function calls
     */
    public boolean parseStringExpr(){
        boolean passedStringExpr = true;

        if(verboseMode)
            System.out.println("PARSER: parseStringExpr()");

        // we already matched the opening quote, so check if next token is a char
        if(tokIndex < tokens.size() && tokens.get(tokIndex).getKind().equals("T_CHAR")){
            if(!parseCharList()){
                passedStringExpr = false;
            }
        }
        // check we don't have unclosed string and if next token is quote
        if(tokIndex < tokens.size() && checkToken("T_QUOTE")) {
            cst.addNode("\"", "child");
        }
        // we have an unclosed string
        else{
            if(errorCount == 0)
                throwErr("Expected [\"] got 'end of stream");

            passedStringExpr = false;
        }
        cst.moveParent();

        return passedStringExpr;
    }

    /**
     * Verifies that the token sequence is correct for a Boolean Expression
     * BooleanExpr ::== ( Expr boolop Expr )
     *             ::== boolVal
     * @return boolean passedBooleanExpr token sequence matches that of BooleanExpr and there are
     * no errors in internal function calls
     */
    public boolean parseBooleanExpr(){
        boolean passedBooleanExpr = true;

        if(verboseMode)
            System.out.println("PARSER: parseBooleanExpr()");
        cst.addNode("BooleanExpression", "branch");

        // check if we have a left parenthesis
        if(tokens.get(tokIndex).getKind().equals("T_L_PAREN")){
            checkToken("T_L_PAREN");
            cst.addNode("(","child");

            // call parseExpr() to check for Expr, make sure no errors thrown there
            if(parseExpr()){
                // check for boolop, make sure no errors thrown there
                if(parseBoolOp()){
                    // check for expression, make sure no errors thrown there
                    if(parseExpr()){
                        // check for closing parenthesis
                        if(checkToken("T_R_PAREN"))
                            cst.addNode(")", "child");
                        else{
                            passedBooleanExpr = false;
                            throwErr("Expected [)] got '" + tokens.get(tokIndex).getValue());
                        }
                    }
                    // error thrown in parseExpr() or one of it's function calls
                    else{
                        passedBooleanExpr = false;
                    }
                }
                // error thrown in parseBoolOp() or one of it's function calls
                else{
                    passedBooleanExpr = false;
                }
            }
            // error thrown in parseExpr() or one of it's function calls
            else{
                passedBooleanExpr = false;
            }

        }
        // check if we have boolval true
        else if(tokens.get(tokIndex).getKind().equals("T_BOOL_TRUE")){
            checkToken("T_BOOL_TRUE");
            cst.addNode("BoolVal","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            cst.moveParent();
        }
        // check if we have boolval false
        else {
            if(checkToken("T_BOOL_FALSE")) {
                cst.addNode("BoolVal", "branch");
                cst.addNode(tokens.get(tokIndex - 1).getValue(), "child");
                cst.moveParent();
            }
            // no boolean expression was found
            else{
                passedBooleanExpr = false;
                throwErr("Expected [BooleanExpression] got '" + tokens.get(tokIndex).getValue());
            }
        }
        cst.moveParent();

        return passedBooleanExpr;
    }

    /**
     * Verifies that the token sequence is correct for a Char List
     * CharList ::== char CharList
     *          ::== space CharList
     *          ::==
     * @return boolean passedCharList token sequence matches that of CharList and there are
     * no errors in internal function calls
     */
    public boolean parseCharList(){
        boolean passedCharList = true;

        if(verboseMode)
            System.out.println("PARSER: parseCharList()");
        cst.addNode("CharList","branch");

        // check if we have a character (or space)
        if(checkToken("T_CHAR")){
            cst.addNode("Char","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            cst.moveParent();
            if(tokIndex < tokens.size() && tokens.get(tokIndex).getKind().equals("T_CHAR"))
                parseCharList();

        }
        // check if we have nothing (closing quote)
        else if(tokIndex < tokens.size() && !tokens.get(tokIndex).getKind().equals("T_QUOTE")){
            passedCharList = false;
            throwErr("Expected [Char, CharList, Space, \"] got '" + tokens.get(tokIndex).getValue());
        }

        cst.moveParent();

        return passedCharList;
    }

    /**
     * Verifies that the token sequence is correct for a Bool Op
     * boolop    ::== == | !=
     * @return boolean passedBoolOp token sequence matches that of boolop and there are
     * no errors in internal function calls
     */
    public boolean parseBoolOp(){
        boolean passedBoolOp = true;

        if(verboseMode)
            System.out.println("PARSER: parseBoolOp()");
        cst.addNode("BoolOp","branch");

        // check if we have "=="
        if(checkToken("T_EQUALITY_OP")){
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        }
        else{
            // check if we have "!="
            if(checkToken("T_INEQUALITY_OP")){
                cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            }
            // we don't have a boolop -> throw error
            else{
                passedBoolOp = false;
                throwErr("Expected BoolOp got '" + tokens.get(tokIndex).getValue());
            }
        }
        cst.moveParent();

        return passedBoolOp;
    }

    /**
     * Checks if the expected token is equal to the current token
     * @param expectedKind expected token kind
     * @return boolean if current token matches the expected token
     */
    public boolean checkToken(String expectedKind){
        boolean tokenMatch = false;

        // checks if we have reached end of stream and are still expecting a token
        if (tokIndex >= tokens.size() && errorCount == 0) {
            System.out.println("PARSER: ERROR: Expected [" + expectedKind + "] got end of stream.");
            errorCount++;
        }

        // check if current token == expected token
        else{
            if(tokens.get(tokIndex).getKind().equals(expectedKind)) {
                tokenMatch = true;
                tokIndex++;
            }
        }

        return tokenMatch;
    }

    /**
     * Throws an error message that the expected token was not equal to the current
     * and updates the errorCount
     * @param expectedKind expected token kind
     */
    public void throwErr(String expectedKind){
        // check for out of bounds
        if(tokIndex < tokens.size())
            System.out.println("PARSER: ERROR: " + expectedKind + "' on line " + tokens.get(tokIndex).getLine());
        else
            System.out.println("PARSER: ERROR: " + expectedKind + "' on line " + tokens.get(tokIndex-1).getLine());
        errorCount++;
    }
}
