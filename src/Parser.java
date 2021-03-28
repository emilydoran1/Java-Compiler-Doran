/**
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

    public void parse(){
        System.out.println("PARSER: parse()");

        parseProgram();

    }

    public boolean parseProgram(){
        boolean passedProgram = true;

        System.out.println("PARSER: parseProgram()");
        cst.addNode("Program","root");
        if(parseBlock()){
            if(checkToken("T_EOP"))
                cst.addNode("$", "child");
            else{
                throwErr("Expected [EOP] got '" + tokens.get(tokIndex).getValue());
            }
        }
        else{
            passedProgram = false;
        }

        return passedProgram;
    }

    public boolean parseBlock(){
        boolean passedBlock = true;

        System.out.println("PARSER: parseBlock()");
        cst.addNode("Block","branch");
        checkToken("T_L_BRACE");
        cst.addNode("{","child");
        if(parseStatementList()){
            checkToken("T_R_BRACE");
            cst.addNode("}", "child");
        }
        else{
            passedBlock = false;
        }
        cst.moveParent();

        return passedBlock;
    }

    public boolean parseStatementList(){
        boolean passedStatementList = true;

        System.out.println("PARSER: parseStatementList()");
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
        else if(tokIndex < tokens.size() && tokens.get(tokIndex).getKind() == "T_R_BRACE" && tokens.get(tokIndex-1).getKind() == "T_L_BRACE"){
            cst.addNode("StatementList","branch");
            cst.moveParent();
        }
        else if(tokIndex >= tokens.size()){
            throwErr("Expected [Statement] got 'end of stream");
            passedStatementList = false;
        }

        if(errorCount > 0)
            passedStatementList = false;

        return passedStatementList;
    }

    public boolean parseStatement(){
        boolean passedStatement = true;

        System.out.println("PARSER: parseStatement()");
        cst.addNode("Statement","branch");
        if(checkToken("T_PRINT")) {
            cst.addNode("PrintStatement","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            cst.addNode("(","child");
            parsePrintStatement();
        }
        else if(checkToken("T_ID")) {
            if(!parseAssignStatement()){
                passedStatement = false;
            }
        }
        else if(checkToken("T_VARIABLE_TYPE")){
            if(!parseVarDecl()) {
                passedStatement = false;
            }
        }
        else if(checkToken("T_WHILE"))
            parseWhileStatement();
        else if(checkToken("T_IF"))
            parseIfStatement();
        else if(tokens.get(tokIndex).getKind() == "T_L_BRACE")
            parseBlock();
        else {
            throwErr("Expected [PrintStatement, AssignStatement, VarDecl, WhileStatement, " +
                    "IfStatement, Block] got '" + tokens.get(tokIndex).getValue());
            passedStatement = false;
        }

        cst.moveParent();

        return passedStatement;
    }

    public void parsePrintStatement(){
        System.out.println("PARSER: parsePrintStatement()");
        if(checkToken("T_L_PAREN")) {
            parseExpr();
            if(checkToken("T_R_PAREN")) {
                cst.addNode(")", "child");
            }
        }
        cst.moveParent();
    }

    public boolean parseAssignStatement(){
        boolean passedAssignStatement = true;

        System.out.println("PARSER: parseAssignStatement()");
        cst.addNode("AssignStatement","branch");
        cst.addNode("Id","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        cst.moveParent();

        if(checkToken("T_ASSIGN_OP")) {
            cst.addNode(tokens.get(tokIndex - 1).getValue(), "child");
            parseExpr();
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
            parseIntExpr();
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

    public void parseIntExpr(){
        System.out.println("PARSER: parseIntExpr()");
        cst.addNode("IntegerExpression", "branch");
        if(tokens.get(tokIndex).getKind().equals("T_ADDITION_OP")) {
            checkToken("T_ADDITION_OP");
            parseExpr();
        }
        else{
            cst.addNode("Digit","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
            cst.moveParent();
        }
        cst.moveParent();
    }

    public void parseStringExpr(){
        System.out.println("PARSER: parseStringExpr()");
        if(tokens.get(tokIndex).getKind().equals("T_CHAR"))
            parseCharList();
        checkToken("T_QUOTE");
        cst.addNode("\"","child");
        cst.moveParent();
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

    public void parseCharList(){
        System.out.println("PARSER: parseCharList()");
        cst.addNode("CharList","branch");
        if(checkToken("T_CHAR")){
            cst.addNode("Char","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            cst.moveParent();
            if(tokens.get(tokIndex).getKind().equals("T_CHAR"))
                parseCharList();
        }
        cst.moveParent();
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
