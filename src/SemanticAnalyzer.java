import java.util.ArrayList;

/**
 * This program provides semantic analysis for the program and generates an Abstract
 * Syntax Tree based on the tokens generated in Lex
 *
 * @author Emily Doran
 *
 */
public class SemanticAnalyzer {

    private ArrayList<Token> tokens;
    private boolean verboseMode;
    private int tokIndex = 0;

    private ConcreteSyntaxTree ast = new ConcreteSyntaxTree();

    int errorCount = 0;
    int warningCount = 0;

    public SemanticAnalyzer(ArrayList<Token> tokens, boolean verboseMode, boolean passedLex, boolean passedParse, int programNum) {
        this.tokens = tokens;
        this.verboseMode = verboseMode;

        if(passedLex && passedParse){
            block();
            System.out.println("\nAST for program " + programNum + " ...");
            System.out.println(ast.toString());

            System.out.println("\nProgram " + programNum + " Semantic Analysis produced " + errorCount + " error(s) and " +
                    warningCount + " warning(s).");

        }
        else if(!passedLex){
            System.out.println("\nAST for program " + programNum + ": Skipped due to LEXER error(s)");
            System.out.println("\nCompilation stopped due to LEXER error(s) . . .");
        }
        else{
            System.out.println("\nAST for program " + programNum + ": Skipped due to PARSER error(s)");
            System.out.println("\nCompilation stopped due to PARSER error(s) . . .");
        }
    }

    public void block() {
        ast.addNode("BLOCK","branch");
        tokIndex++;
        stmt();
    }

    public void stmt(){
        // we have a PrintStatement
        if(checkToken("T_PRINT")) {
            printStmt();
        }
        // we have an AssignStatement (which begins with id)
        else if(checkToken("T_ID")) {
            assignStmt();
        }
        // we have a VarDecl (which begins with var type)
        else if(checkToken("T_VARIABLE_TYPE")){
            varDecl();
        }
        // we have a WhileStatement
        else if(checkToken("T_WHILE")) {
            whileStmt();
        }
        // we have an IfStatement
        else if(checkToken("T_IF")) {
            ifStmt();
        }
        // we have a Block (which begins with left brace)
        else if(checkToken("T_L_BRACE")){
            tokIndex--;
            block();
        }

        // check if we have another statement next
        if(!checkToken("T_R_BRACE")){
            stmt();
        }
    }

    public void printStmt() {
        ast.addNode("Print Statement","branch");
        // skip the opening parenthesis
        tokIndex++;
        expr();
        // skip the closing parenthesis
        tokIndex++;
    }

    public void assignStmt(){
        ast.addNode("Assign Statement","branch");
        ast.addNode(tokens.get(tokIndex-1).getValue(),"child");
        // we already matched Id in prev function, so next item to match is "="
        if(checkToken("T_ASSIGN_OP")) {
            // parseExpr() or other function calls threw an error
            expr();
        }
        ast.moveParent();
    }

    public void varDecl() {
        ast.addNode("Variable Declaration","branch");
        ast.addNode(tokens.get(tokIndex-1).getValue(), "child");
        ast.addNode(tokens.get(tokIndex).getValue(), "child");
        tokIndex++;
        ast.moveParent();
        ast.moveParent();
    }

    public void whileStmt() {
        ast.addNode("While Statement","branch");
        booleanExpr();
        block();
        ast.moveParent();
    }

    public void ifStmt() {
        ast.addNode("If Statement","branch");
        ast.addNode(tokens.get(tokIndex).getValue(), "child");
        tokIndex++;
        ast.moveParent();
        ast.moveParent();
    }

    public void expr(){
        // check if we have an IntExpr
        if(checkToken("T_DIGIT")){
            intExpr();
        }
        // check if we have a StringExpr
        else if(checkToken("T_QUOTE")) {
            stringExpr();
        }
        // check if we have an Id
        else if(checkToken("T_ID")){
            ast.addNode(tokens.get(tokIndex-1).getValue(), "child");
//            ast.moveParent();
        }
        // check if we have a BooleanExpr
        else if(tokens.get(tokIndex).getKind().equals("T_L_PAREN") ||
                tokens.get(tokIndex).getKind().equals("T_BOOL_TRUE") ||
                tokens.get(tokIndex).getKind().equals("T_BOOL_FALSE")){
            booleanExpr();
            ast.moveParent();
        }
    }

    public void intExpr(){
        // check if it's an intop
        if(tokens.get(tokIndex).getKind().equals("T_ADDITION_OP")) {
            ast.addNode("Addition","branch");
            ast.addNode(tokens.get(tokIndex-1).getValue(), "child");
            tokIndex++;
            expr();

        }
        // we do not have an intop, so add node for just digit
        else{
            ast.addNode(tokens.get(tokIndex-1).getValue(), "child");
        }
        ast.moveParent();
    }

    public void stringExpr(){
        ast.addNode("CharList","branch");
        String charList = "";

        // check if we have a character (or space)
        while(checkToken("T_CHAR")){
            charList += tokens.get(tokIndex-1).getValue();
        }
        checkToken("T_QUOTE");
        ast.addNode(charList, "child");
        ast.moveParent();
    }

    public void booleanExpr(){
        // check if we have a left parenthesis
        if(tokens.get(tokIndex).getKind().equals("T_L_PAREN")){
            checkToken("T_L_PAREN");
            ast.addNode("Boolean Expression","branch");
            expr();
            if(checkToken("T_EQUALITY_OP")){
                ast.addNode("Equals","child");
            }
            else{
                if(checkToken("T_INEQUALITY_OP")){
                    ast.addNode("Not Equals","child");
                }
            }
            expr();
            checkToken("T_R_PAREN");
        }
        // check if we have boolval true
        else if(tokens.get(tokIndex).getKind().equals("T_BOOL_TRUE")){
            checkToken("T_BOOL_TRUE");
            ast.addNode(tokens.get(tokIndex-1).getValue(),"child");
//            ast.moveParent();
        }
        // check if we have boolval false
        else {
            if(checkToken("T_BOOL_FALSE")) {
                ast.addNode(tokens.get(tokIndex - 1).getValue(), "child");
//                ast.moveParent();
            }
        }
    }

    public boolean checkToken(String expectedKind) {
        boolean tokenMatch = false;

        if (tokens.get(tokIndex).getKind().equals(expectedKind)) {
            tokenMatch = true;
            tokIndex++;
        }

        return tokenMatch;
    }
}
