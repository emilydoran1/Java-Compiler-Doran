import java.util.ArrayList;
import java.util.Set;
import java.util.Hashtable;

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

    private SyntaxTree ast = new SyntaxTree();

    int errorCount = 0;
    int warningCount = 0;

    SymbolTable symbolTable = new SymbolTable();
    private int scopeCount = 0;
    private int currentScope = 0;

    public SemanticAnalyzer(ArrayList<Token> tokens, boolean verboseMode, boolean passedLex, boolean passedParse, int programNum) {
        this.tokens = tokens;
        this.verboseMode = verboseMode;

        if(passedLex && passedParse){
            block();
            System.out.println("\nAST for program " + programNum + " ...");
            System.out.println(ast.toString());

            System.out.println("SCOPE SIZE: " + symbolTable.size());
            for(int i = 0; i < symbolTable.size(); i++){
                Set<String> keys = symbolTable.get(i).getScopeItems().keySet();
                for(String key: keys){
                    System.out.println("value: " + key +
                            "  type: " + symbolTable.get(i).getScopeItems().get(key).getType() +
                            "  isUsed: " + symbolTable.get(i).getScopeItems().get(key).getIsUsed() +
                            "  isInitialized: " + symbolTable.get(i).getScopeItems().get(key).getIsInitialized());
                }

            }

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
        Hashtable<String, SymbolTableItem> newHash = new Hashtable<String, SymbolTableItem>();
        Scope tempScope = new Scope(scopeCount, newHash);
        symbolTable.addScope(tempScope);
        scopeCount++;
        currentScope = scopeCount-1;
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
        ast.addNode("Print","branch");
        // skip the opening parenthesis
        tokIndex++;
        expr();
        // skip the closing parenthesis
        tokIndex++;
        ast.moveParent();
    }

    public void assignStmt(){
        ast.addNode("Assign","branch");
        ast.addNode(tokens.get(tokIndex-1).getValue(),"child");
        // we already matched Id in prev function, so next item to match is "="
        if(checkToken("T_ASSIGN_OP")) {
            // parseExpr() or other function calls threw an error
            expr();
        }
        ast.moveParent();
    }

    public void varDecl() {
        ast.addNode("VariableDeclaration","branch");
        ast.addNode(tokens.get(tokIndex-1).getValue(), "child");
        ast.addNode(tokens.get(tokIndex).getValue(), "child");
        SymbolTableItem newItem = new SymbolTableItem(tokens.get(tokIndex-1).getValue());
        symbolTable.get(currentScope).addItem(tokens.get(tokIndex).getValue(), newItem);
        tokIndex++;
        ast.moveParent();
//        ast.moveParent();
    }

    public void whileStmt() {
        ast.addNode("While","branch");
        booleanExpr();
        block();
        ast.moveParent();
        ast.moveParent();
    }

    public void ifStmt() {
        ast.addNode("If","branch");
        booleanExpr();
        block();
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
        }
        // check if we have a BooleanExpr
        else if(tokens.get(tokIndex).getKind().equals("T_L_PAREN") ||
                tokens.get(tokIndex).getKind().equals("T_BOOL_TRUE") ||
                tokens.get(tokIndex).getKind().equals("T_BOOL_FALSE")){
            booleanExpr();
        }
    }

    public void intExpr(){
        // check if it's an intop
        if(tokens.get(tokIndex).getKind().equals("T_ADDITION_OP")) {
            ast.addNode("Addition","branch");
            ast.addNode(tokens.get(tokIndex-1).getValue(), "child");
            tokIndex++;
            expr();
            ast.moveParent();

        }
        // we do not have an intop, so add node for just digit
        else{
            ast.addNode(tokens.get(tokIndex-1).getValue(), "child");
        }
    }

    public void stringExpr(){
        String charList = "";

        // check if we have a character (or space)
        while(checkToken("T_CHAR")){
            charList += tokens.get(tokIndex-1).getValue();
        }
        checkToken("T_QUOTE");
        ast.addNode(charList, "child");
//        ast.moveParent();
    }

    public void booleanExpr(){
        // check if we have a left parenthesis
        if(tokens.get(tokIndex).getKind().equals("T_L_PAREN")){
            checkToken("T_L_PAREN");
            int count = 0;
            while (!tokens.get(tokIndex).getKind().equals("T_EQUALITY_OP") &&
                    !tokens.get(tokIndex).getKind().equals("T_INEQUALITY_OP")){
                tokIndex++;
                count++;
            }
            if(checkToken("T_EQUALITY_OP")){
                ast.addNode("isEqual","branch");
                // reset token count
                tokIndex = tokIndex - count - 1;
            }
            else{
                if(checkToken("T_INEQUALITY_OP")){
                    ast.addNode("isNotEqual","branch");
                    // reset token count
                    tokIndex = tokIndex - count - 1;
                }
            }
            expr();
            if(checkToken("T_EQUALITY_OP")){

            }
            else{
                if(checkToken("T_INEQUALITY_OP")){

                }
            }
            expr();
            checkToken("T_R_PAREN");
            ast.moveParent();
        }
        // check if we have boolval true
        else if(tokens.get(tokIndex).getKind().equals("T_BOOL_TRUE")){
            checkToken("T_BOOL_TRUE");
            ast.addNode(tokens.get(tokIndex-1).getValue(),"child");
        }
        // check if we have boolval false
        else {
            if(checkToken("T_BOOL_FALSE")) {
                ast.addNode(tokens.get(tokIndex - 1).getValue(), "child");
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
