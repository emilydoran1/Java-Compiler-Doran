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

    private boolean lastResult = false;

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

    public void parseProgram(){
        System.out.println("PARSER: parseProgram()");
        cst.addNode("Program","root");
        parseBlock();
        lastResult = true;
        if(errorCount == 0) {
            checkToken("T_EOP");
            cst.addNode("$", "child");
        }
    }

    public void parseBlock(){
        System.out.println("PARSER: parseBlock()");
        cst.addNode("Block","branch");
        checkToken("T_L_BRACE");
        cst.addNode("{","child");
        parseStatementList();
        if(errorCount == 0) {
            checkToken("T_R_BRACE");
            cst.addNode("}", "child");
        }
        cst.moveParent();
    }

    public void parseStatementList(){
        System.out.println("PARSER: parseStatementList()");
        if(tokIndex < tokens.size() && tokens.get(tokIndex).getKind() != "T_R_BRACE"){
            cst.addNode("StatementList","branch");
            parseStatement();
            if(errorCount == 0)
                parseStatementList();
            cst.moveParent();
        }
        else if(tokIndex < tokens.size() && tokens.get(tokIndex).getKind() == "T_R_BRACE" && tokens.get(tokIndex-1).getKind() == "T_L_BRACE"){
            cst.addNode("StatementList","branch");
            cst.moveParent();
        }
    }

    public void parseStatement(){
        System.out.println("PARSER: parseStatement()");
        cst.addNode("Statement","branch");
        if(checkToken("T_PRINT")) {
            cst.addNode("PrintStatement","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            cst.addNode("(","child");
            parsePrintStatement();
        }
        else if(checkToken("T_ID"))
            parseAssignStatement();
        else if(checkToken("T_VARIABLE_TYPE"))
            parseVarDecl();
        else if(checkToken("T_WHILE"))
            parseWhileStatement();
        else if(checkToken("T_IF"))
            parseIfStatement();
        else if(tokens.get(tokIndex).getKind() == "T_L_BRACE")
            parseBlock();
        else {
            lastResult = true;
            checkToken("T_R_BRACE");
        }

        cst.moveParent();
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

    public void parseAssignStatement(){
        System.out.println("PARSER: parseAssignStatement()");
        cst.addNode("AssignStatement","branch");
        cst.addNode("Id","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        cst.moveParent();
        lastResult = true;
        if(checkToken("T_ASSIGN_OP")) {
            cst.addNode(tokens.get(tokIndex - 1).getValue(), "child");
            parseExpr();
        }
        cst.moveParent();
    }

    public void parseVarDecl(){
        System.out.println("PARSER: parseVarDecl()");
        cst.addNode("VarDecl","branch");
        parseType();
        cst.moveParent();
    }

    public void parseType(){
        System.out.println("PARSER: parseType()");
        cst.addNode("Type","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
        cst.moveParent();
        lastResult = true;
        if(checkToken("T_ID")){
            cst.addNode("Id","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(), "child");
            cst.moveParent();
        }
    }

    public void parseWhileStatement(){
        System.out.println("PARSER: parseWhileStatement()");
        cst.addNode("WhileStatement","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        parseBooleanExpr();
        parseBlock();
        cst.moveParent();
    }

    public void parseIfStatement(){
        System.out.println("PARSER: parseIfStatement()");
        cst.addNode("IfStatement","branch");
        cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        parseBooleanExpr();
        parseBlock();
        cst.moveParent();
    }

    public void parseExpr(){
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
        else{
            parseBooleanExpr();
        }

        cst.moveParent();
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

    public void parseBooleanExpr(){
        System.out.println("PARSER: parseBooleanExpr()");
        cst.addNode("BooleanExpression", "branch");
//        tokIndex--;

        if(tokens.get(tokIndex).getKind().equals("T_L_PAREN")){
            checkToken("T_L_PAREN");
            cst.addNode("(","child");
            parseExpr();
            parseBoolOp();
            parseExpr();
            checkToken("T_R_PAREN");
            cst.addNode(")","child");
        }
        else if(tokens.get(tokIndex).getKind().equals("T_BOOL_TRUE")){
            checkToken("T_BOOL_TRUE");
            cst.addNode("BoolVal","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            cst.moveParent();
        }
        else {
            checkToken("T_BOOL_FALSE");
            cst.addNode("BoolVal","branch");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
            cst.moveParent();
        }
        cst.moveParent();
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

    public void parseBoolOp(){
        System.out.println("PARSER: parseBoolOp()");
        cst.addNode("BoolOp","branch");

        if(tokens.get(tokIndex).getKind().equals("T_EQUALITY_OP")){
            checkToken("T_EQUALITY_OP");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        }
        else if(tokens.get(tokIndex).getKind().equals("T_INEQUALITY_OP")){
            checkToken("T_INEQUALITY_OP");
            cst.addNode(tokens.get(tokIndex-1).getValue(),"child");
        }
        cst.moveParent();
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
                lastResult = false;

            }
            else if (lastResult == true && errorCount == 0){

                System.out.println("PARSER: ERROR: Expected [" + expectedKind + "] got [" +
                        tokens.get(tokIndex).getKind() + "] with value '" + tokens.get(tokIndex).getValue()
                                + "' on line " + tokens.get(tokIndex).getLine());

                errorCount++;
                lastResult = false;
            }
        }

        return tokenMatch;
    }

}
