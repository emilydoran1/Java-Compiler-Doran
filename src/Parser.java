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

        System.out.println("PARSER: parseAssignStatement()");
        cst.addNode("AssignStatement","branch");
        cst.addNode("Id","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        cst.moveParent();

        if(checkToken("T_ASSIGN_OP")) {
            cst.addNode(tokens.get(tokIndex - 1).getValue(), "child");
            if(!parseExpr())
                passedAssignStatement = false;
        }
        else{
            throwErr("Expected [=] got '" + tokens.get(tokIndex).getValue());
            passedAssignStatement = false;
        }
        cst.moveParent();

        return passedAssignStatement;
    }

    public boolean parseVarDecl(){
        boolean passedVarDecl = true;

        System.out.println("PARSER: parseVarDecl()");
        cst.addNode("VarDecl","branch");
        if(!parseType())
            passedVarDecl = false;
        cst.moveParent();

        return passedVarDecl;
    }

    public boolean parseType(){
        boolean passedType = true;

        System.out.println("PARSER: parseType()");
        cst.addNode("Type","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
        cst.moveParent();

        if(checkToken("T_ID")){
            cst.addNode("Id","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
            cst.moveParent();
        }
        else{
            passedType = false;
            throwErr("Expected [id] got '" + tokens.get(tokIndex).getValue());
        }

        return passedType;
    }

    public boolean parseWhileStatement(){
        boolean passedWhileStatement = true;

        System.out.println("PARSER: parseWhileStatement()");
        cst.addNode("WhileStatement","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        if(parseBooleanExpr()){
            parseBlock();
        }
        else{
            passedWhileStatement = false;
        }
        cst.moveParent();

        return passedWhileStatement;
    }

    public boolean parseIfStatement(){
        boolean passedIfStatement = true;

        System.out.println("PARSER: parseIfStatement()");
        cst.addNode("IfStatement","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        if(parseBooleanExpr()){
            parseBlock();
        }
        else{
            passedIfStatement = false;
        }
        cst.moveParent();

        return passedIfStatement;
    }

    public boolean parseExpr(){
        boolean passedExpr = true;

        System.out.println("PARSER: parseExpr()");
        cst.addNode("Expression", "branch");
        if(checkToken("T_DIGIT")){
            if(!parseIntExpr())
                passedExpr = false;
        }
        else if(checkToken("T_QUOTE")) {
            cst.addNode("StringExpression","branch");
            cst.addNode("\"","child");
            parseStringExpr();
        }
        else if(checkToken("T_ID")){
            cst.addNode("Id","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
            cst.moveParent();
        }
        else if(tokens.get(tokIndex).getKind().equals("T_L_PAREN") ||
                tokens.get(tokIndex).getKind().equals("T_BOOL_TRUE") ||
                tokens.get(tokIndex).getKind().equals("T_BOOL_FALSE")){
            parseBooleanExpr();
        }
        else{
            passedExpr = false;
            throwErr("Expected [IntExpr, StringExpr, BooleanExpr, Id] got '" + tokens.get(tokIndex).getValue());
        }

        cst.moveParent();

        return passedExpr;
    }

    public boolean parseIntExpr(){
        boolean passedIntExpr = true;

        System.out.println("PARSER: parseIntExpr()");
        cst.addNode("IntegerExpression", "branch");
        if(tokens.get(tokIndex).getKind().equals("T_ADDITION_OP")) {
            cst.addNode("Digit","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
            cst.moveParent();
            if(checkToken("T_ADDITION_OP")){
                cst.addNode("IntOp","branch");
                cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
                cst.moveParent();
                if(!parseExpr())
                    passedIntExpr = false;
            }
            else{
                passedIntExpr = false;
            }
        }
        else{
            cst.addNode("Digit","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
            cst.moveParent();
        }
        cst.moveParent();

        return passedIntExpr;
    }

    public boolean parseStringExpr(){
        boolean passedStringExpr = true;

        System.out.println("PARSER: parseStringExpr()");
        if(tokens.get(tokIndex).getKind().equals("T_CHAR")){
            if(!parseCharList()){
                passedStringExpr = false;
            }
        }
        if(checkToken("T_QUOTE")) {
            cst.addNode("\"", "child");
        }
        else{
            passedStringExpr = false;
        }
        cst.moveParent();

        return passedStringExpr;
    }

    public boolean parseBooleanExpr(){
        boolean passedBooleanExpr = true;

        System.out.println("PARSER: parseBooleanExpr()");
        cst.addNode("BooleanExpression", "branch");

        if(tokens.get(tokIndex).getKind().equals("T_L_PAREN")){
            checkToken("T_L_PAREN");
            cst.addNode("(","child");
            parseExpr();
            if(parseBoolOp()){
                parseExpr();
                checkToken("T_R_PAREN");
                cst.addNode(")", "child");
            }
            else{
                passedBooleanExpr = false;
            }
        }
        else if(tokens.get(tokIndex).getKind().equals("T_BOOL_TRUE")){
            checkToken("T_BOOL_TRUE");
            cst.addNode("BoolVal","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            cst.moveParent();
        }
        else {
            if(checkToken("T_BOOL_FALSE")) {
                cst.addNode("BoolVal", "branch");
                cst.addNode(tokens.get(tokIndex - 1).getValue(), "child");
                cst.moveParent();
            }
            else{
                passedBooleanExpr = false;
            }
        }
        cst.moveParent();

        return passedBooleanExpr;
    }

    public boolean parseCharList(){
        boolean passedCharList = true;

        System.out.println("PARSER: parseCharList()");
        cst.addNode("CharList","branch");
        if(checkToken("T_CHAR")){
            cst.addNode("Char","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            cst.moveParent();
            if(tokens.get(tokIndex).getKind().equals("T_CHAR"))
                parseCharList();
        }
        else if(!tokens.get(tokIndex).getKind().equals("T_QUOTE")){
            passedCharList = false;
            throwErr("Expected [Char, CharList, Space, Nothing] got '" + tokens.get(tokIndex).getValue());
        }
        cst.moveParent();

        return passedCharList;
    }

    public boolean parseBoolOp(){
        boolean passedBoolOp = true;

        System.out.println("PARSER: parseBoolOp()");
        cst.addNode("BoolOp","branch");

        if(checkToken("T_EQUALITY_OP")){
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        }
        else{
            if(checkToken("T_INEQUALITY_OP")){
                cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            }
            else{
                passedBoolOp = false;
                throwErr("Expected BoolOp got '" + tokens.get(tokIndex).getValue());
            }
        }
        cst.moveParent();

        return passedBoolOp;
    }

    public boolean checkToken(String expectedKind){
        boolean tokenMatch = false;

        if (tokens.size() <= tokIndex && errorCount == 0) {
            System.out.println("PARSER: ERROR: Expected [" + expectedKind + "] got end of stream.");
            errorCount++;
        }

        else{
            if(tokens.get(tokIndex).getKind().equals(expectedKind)) {
                tokenMatch = true;
                tokIndex++;
            }

        }

        return tokenMatch;
    }

    public void throwErr(String expectedKind){
        if(tokIndex < tokens.size())
            System.out.println("PARSER: ERROR: " + expectedKind + "' on line " + tokens.get(tokIndex).getLine());
        else
            System.out.println("PARSER: ERROR: " + expectedKind + "' on line " + tokens.get(tokIndex-1).getLine());
        errorCount++;
    }

}
