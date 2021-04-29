import java.util.ArrayList;
import java.util.Hashtable;

/**
 * This program provides semantic analysis for the program and generates an Abstract
 * Syntax Tree based on the tokens generated in Lex and creates a symbol table if semantic
 * analysis completes without errors.
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
    private int prevScope;

    public SemanticAnalyzer(ArrayList<Token> tokens, boolean verboseMode, boolean passedLex, boolean passedParse, int programNum) {
        this.tokens = tokens;
        this.verboseMode = verboseMode;

        if(passedLex && passedParse){

            System.out.println("SEMANTIC ANALYSIS: Beginning Semantic Analysis on Program " + programNum + " ...");
            block();
            warningCount += symbolTable.printWarnings();
            System.out.println("\nProgram " + programNum + " Semantic Analysis produced " + errorCount + " error(s) and " +
                    warningCount + " warning(s).");

            if(errorCount == 0){
                System.out.println("\nAST for program " + programNum + " ...");
                System.out.println(ast.toString());
                System.out.println("Program " + programNum + " Symbol Table");
                System.out.println("---------------------------");
                System.out.printf("%-6s%-9s%-7s%-4s\n", "Name", "Type", "Scope", "Line");
                System.out.println("---------------------------");
                symbolTable.printSymbolTable();
            }
            else{
                System.out.println("\nAST for program " + programNum + ": Skipped due to SEMANTIC ANALYSIS error(s)");
                System.out.println("\nSymbol Table for program " + programNum + ": Skipped due to SEMANTIC ANALYSIS error(s)");
            }

        }
        else if(!passedLex){
            System.out.println("\nSemantic Analysis for program " + programNum + ": Skipped due to LEXER error(s)");
            System.out.println("\nAST for program " + programNum + ": Skipped due to LEXER error(s)");
            System.out.println("\nSymbol Table for program " + programNum + ": Skipped due to LEXER error(s)");
        }
        else{
            System.out.println("\nSemantic Analysis for program " + programNum + ": Skipped due to PARSER error(s)");
            System.out.println("\nAST for program " + programNum + ": Skipped due to PARSER error(s)");
            System.out.println("\nSymbol Table for program " + programNum + ": Skipped due to PARSER error(s)");
        }
    }

    /**
     * Begin creating AST and symbol table, call stmt() after adding block
     * Block ::== { StatementList }
     */
    public void block() {
        // add block to AST
        ast.addNode("BLOCK","branch");
        tokIndex++;

        // add a new scope to the symbol table and increment scopeCount and currentScope vars
        Hashtable<String, SymbolTableItem> newHash = new Hashtable<String, SymbolTableItem>();
        Scope tempScope = new Scope(scopeCount, newHash);
        prevScope = currentScope;
        symbolTable.addScope(tempScope);
        scopeCount++;
        currentScope = scopeCount-1;
        // if we are not the first scope, set parent scope to be previous scope
        if(currentScope != 0){
            symbolTable.get(currentScope).setParent(symbolTable.get(prevScope));
        }
        //
        stmt();
    }

    /**
     * get the statement kind based on current token and call function to handle each case and build AST, symbol table,
     * and perform scope/type checking
     * Statement ::== PrintStatement | AssignStatement | VarDecl | WhileStatement | IfStatement | Block
     */
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
        else{
            if(symbolTable.get(currentScope).getParent() != null) {
                currentScope = symbolTable.get(currentScope).getParent().getScopeNum();
            }
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
        if(symbolTable.get(currentScope).getScopeItems().get(tokens.get(tokIndex-1).getValue()) != null) {

        }
        else if(symbolTable.get(currentScope).getParent() != null){
            int tempScope = currentScope;
            while(symbolTable.get(tempScope).getParent() != null){
                if(symbolTable.get(tempScope).getParent().getScopeItems().get(tokens.get(tokIndex-1).getValue()) != null) {
                    tempScope = 0;
                }
                else{
                    tempScope = symbolTable.get(tempScope).getParent().getScopeNum();
                    if(tempScope == 0){
                        System.out.println("SEMANTIC ANALYSIS: ERROR: Undeclared variable [ " + tokens.get(tokIndex-1).getValue() +
                                " ] was assigned a value at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                tokens.get(tokIndex - 1).getPosition() + ") before being declared.");
                        errorCount++;
                    }
                }
            }
        }
        else{
            System.out.println("SEMANTIC ANALYSIS: ERROR: Undeclared variable [ " + tokens.get(tokIndex-1).getValue() +
                    " ] was assigned a value at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                    tokens.get(tokIndex - 1).getPosition() + ") before being declared.");
            errorCount++;
        }
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
        if(symbolTable.get(currentScope).getScopeItems().get(tokens.get(tokIndex).getValue()) == null){
            SymbolTableItem newItem = new SymbolTableItem(tokens.get(tokIndex-1).getValue(), tokens.get(tokIndex-1).getLine());
            symbolTable.get(currentScope).addItem(tokens.get(tokIndex).getValue(), newItem);
            if(verboseMode) {
                System.out.println("SEMANTIC ANALYSIS: Variable [ " + tokens.get(tokIndex).getValue()
                        + " ] has been declared at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                        tokens.get(tokIndex - 1).getPosition() + ")");
            }
        }
        else{
            System.out.println("SEMANTIC ANALYSIS: ERROR: Duplicate Variable [ " + tokens.get(tokIndex).getValue() +
                    " ] was declared at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                    tokens.get(tokIndex - 1).getPosition() + ").");
            errorCount++;
        }
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

            // make sure variable exists before it is used
            if(symbolTable.get(currentScope).getScopeItems().get(tokens.get(tokIndex-1).getValue()) != null) {
                // set variable is used boolean
                symbolTable.get(currentScope).getScopeItems().get(tokens.get(tokIndex-1).getValue()).setUsed();
                if (verboseMode) {
                    System.out.println("SEMANTIC ANALYSIS: Variable [ " + tokens.get(tokIndex-1).getValue()
                            + " ] has been used at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ")");
                }

                // see if we are using the variable in a boolean expression or Addition
                if((ast.getCurrent().getName().equals("isEqual") || ast.getCurrent().getName().equals("isNotEqual")
                        || ast.getCurrent().getName().equals("Addition")) && ast.getCurrent().getChildren().size() > 1) {
                    String boolExpType = ast.getCurrent().getChildren().get(0).getName();
                    // check if other node is bool val
                    if(boolExpType.equals("true") || boolExpType.equals("false")){
                        boolExpType = "boolean";
                    }
                    // check if other node is digit
                    else if(boolExpType.matches("[0-9]")){
                        boolExpType = "int";
                    }
                    // check if other node is string
                    else if(boolExpType.matches("[a-z]+[a-z]+")){
                        boolExpType = "string";
                    }
                    // check if other node is variable
                    else if(boolExpType.matches("[a-z]")){
                        boolExpType = getVariableType(boolExpType);
                    }

                    // get the other node's type
                    String boolExpType2 = ast.getCurrent().getChildren().get(1).getName();
                    if(boolExpType2.equals("true") || boolExpType2.equals("false")){
                        boolExpType2 = "boolean";
                    }
                    // other node is digit
                    else if(boolExpType2.matches("[0-9]")){
                        boolExpType2 = "int";
                    }
                    // other node is string
                    else if(boolExpType2.matches("[a-z]+[a-z]+")){
                        boolExpType2 = "string";
                    }
                    // other node is variable
                    else if(boolExpType2.matches("[a-z]")){
                        boolExpType2 = getVariableType(boolExpType2);
                    }

                    if(!boolExpType.equals(boolExpType2)){
                        System.out.println("SEMANTIC ANALYSIS: ERROR: Incorrect Type Comparison - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                                " ] of type [ " + boolExpType + " ] was compared to type [ " + boolExpType2 + " ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                tokens.get(tokIndex - 1).getPosition() + ").");
                        errorCount++;
                    }

                }
            }
            else if(symbolTable.get(currentScope).getParent() != null){
                int tempScope = currentScope;
                while(symbolTable.get(tempScope).getParent() != null){

                    if(symbolTable.get(tempScope).getParent().getScopeItems().get(tokens.get(tokIndex-1).getValue()) != null) {
                        // set variable is used boolean
                        symbolTable.get(tempScope).getParent().getScopeItems().get(tokens.get(tokIndex-1).getValue()).setUsed();
                        if (verboseMode) {
                            System.out.println("SEMANTIC ANALYSIS: Variable [ " + tokens.get(tokIndex-1).getValue()
                                    + " ] has been used at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                    tokens.get(tokIndex - 1).getPosition() + ")");
                        }

                        // see if we are using the variable in a boolean expression or Addition
                        if((ast.getCurrent().getName().equals("isEqual") || ast.getCurrent().getName().equals("isNotEqual")
                                || ast.getCurrent().getName().equals("Addition")) && ast.getCurrent().getChildren().size() > 1) {
                            String boolExpType = ast.getCurrent().getChildren().get(0).getName();
                            // check if other node is bool val
                            if(boolExpType.equals("true") || boolExpType.equals("false")){
                                boolExpType = "boolean";
                            }
                            // check if other node is digit
                            else if(boolExpType.matches("[0-9]")){
                                boolExpType = "int";
                            }
                            // check if other node is string
                            else if(boolExpType.matches("[a-z]+[a-z]+")){
                                boolExpType = "string";
                            }
                            // check if other node is variable
                            else if(boolExpType.matches("[a-z]")){
                                boolExpType = getVariableType(boolExpType);
                            }

                            // get the other node's type
                            String boolExpType2 = ast.getCurrent().getChildren().get(1).getName();
                            if(boolExpType2.equals("true") || boolExpType2.equals("false")){
                                boolExpType2 = "boolean";
                            }
                            // other node is digit
                            else if(boolExpType2.matches("[0-9]")){
                                boolExpType2 = "int";
                            }
                            // other node is string
                            else if(boolExpType2.matches("[a-z]+[a-z]+")){
                                boolExpType2 = "string";
                            }
                            // other node is variable
                            else if(boolExpType2.matches("[a-z]")){
                                boolExpType2 = getVariableType(boolExpType2);
                            }

                            if(!boolExpType.equals(boolExpType2)){
                                System.out.println("SEMANTIC ANALYSIS: ERROR: Incorrect Type Comparison - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                                        " ] of type [ " + boolExpType + " ] was compared to type [ " + boolExpType2 + " ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                        tokens.get(tokIndex - 1).getPosition() + ").");
                                errorCount++;
                            }

                        }

                        tempScope = 0;
                    }
                    else{
                        tempScope = symbolTable.get(tempScope).getParent().getScopeNum();
                        if(tempScope == 0){
                            System.out.println("SEMANTIC ANALYSIS: ERROR: Undeclared variable [ " + tokens.get(tokIndex-1).getValue() +
                                    " ] was used at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                    tokens.get(tokIndex - 1).getPosition() + ") before being declared.");
                            errorCount++;
                        }
                    }
                }
            }
            else{
                System.out.println("SEMANTIC ANALYSIS: ERROR: Undeclared variable [ " + tokens.get(tokIndex-1).getValue() +
                        " ] was used at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                        tokens.get(tokIndex - 1).getPosition() + ") before being declared.");
                errorCount++;
            }

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

            // check if it is an assign op and that var is an int
            if(symbolTable.get(currentScope).getScopeItems().get(ast.getCurrent().getParent().getChildren().get(0).getName()) != null) {
                String varType = symbolTable.get(currentScope).getScopeItems().get(ast.getCurrent().getParent().getChildren().get(0).getName()).getType();
                if(varType.equals("int")){
                    symbolTable.get(currentScope).getScopeItems().get(ast.getCurrent().getParent().getChildren().get(0).getName()).setInitialized();
                    if(verboseMode) {
                        System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getParent().getChildren().get(0).getName()
                                + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                tokens.get(tokIndex - 1).getPosition() + ")");
                    }
                }
                else{
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getParent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was assigned to type [ int ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }
            // check parent to see if var exists
            else if(symbolTable.get(currentScope).getParent() != null){
                int tempScope = currentScope;
                // keep checking parent scopes for variable while parent scope exists
                while(symbolTable.get(tempScope).getParent() != null){
                    if(symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getParent().getChildren().get(0).getName()) != null) {

                        String varType = symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getParent().getChildren().get(0).getName()).getType();
                        if(varType.equals("int")){
                            symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getParent().getChildren().get(0).getName()).setInitialized();                            if(verboseMode) {
                            if(verboseMode) {
                                System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getParent().getChildren().get(0).getName()
                                        + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                        tokens.get(tokIndex - 1).getPosition() + ")");
                            }
                                tempScope = 0;
                            }
                        }
                        else{
                            System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getParent().getChildren().get(0).getName() +
                                    " ] of type [ " + varType + " ] was assigned to type [ int ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                    tokens.get(tokIndex - 1).getPosition() + ").");
                            errorCount++;
                            tempScope = 0;
                        }
                    }
                    else{
                        tempScope = symbolTable.get(tempScope).getParent().getScopeNum();
                        if(tempScope == 0 && symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getParent().getChildren().get(0).getName()) != null){
                            String varType = symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getParent().getChildren().get(0).getName()).getType();

                            if (varType.equals("int")) {
                                symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getParent().getChildren().get(0).getName()).setInitialized();
                                if(verboseMode) {
                                    System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getParent().getChildren().get(0).getName()
                                            + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                            tokens.get(tokIndex - 1).getPosition() + ")");
                                }
                                tempScope = 0;
                            }
                            else {
                                System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getParent().getChildren().get(0).getName() +
                                        " ] of type [ " + varType + " ] was assigned to type [ int ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                        tokens.get(tokIndex - 1).getPosition() + ").");
                                errorCount++;
                                tempScope = 0;
                            }
                        }
                    }
                }
            }

            tokIndex++;
            expr();
            ast.moveParent();

        }
        // we do not have an intop, so add node for just digit
        else{
            ast.addNode(tokens.get(tokIndex-1).getValue(), "child");

            if(symbolTable.get(currentScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()) != null) {
                String varType = symbolTable.get(currentScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).getType();
                if(varType.equals("int")){
                    symbolTable.get(currentScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setInitialized();
                    if(verboseMode) {
                        System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getChildren().get(0).getName()
                                + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                tokens.get(tokIndex - 1).getPosition() + ")");
                    }
                }
                else{
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was assigned to type [ int ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }
            else if(symbolTable.get(currentScope).getParent() != null){
                int tempScope = currentScope;
                while(symbolTable.get(tempScope).getParent() != null){
                    if(symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()) != null) {

                        String varType = symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).getType();
                        if(varType.equals("int")){
                            symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setInitialized();                            if(verboseMode) {
                            if(verboseMode) {
                                System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getChildren().get(0).getName()
                                        + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                        tokens.get(tokIndex - 1).getPosition() + ")");
                            }
                                tempScope = 0;
                            }
                        }
                        else{
                            System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                                    " ] of type [ " + varType + " ] was assigned to type [ int ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                    tokens.get(tokIndex - 1).getPosition() + ").");
                            errorCount++;
                            tempScope = 0;
                        }
                    }
                    else{
                        tempScope = symbolTable.get(tempScope).getParent().getScopeNum();
                        if(tempScope == 0 && symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()) != null){
                            String varType = symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).getType();

                            if (varType.equals("int")) {
                                symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setInitialized();
                                if(verboseMode) {
                                    System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getChildren().get(0).getName()
                                            + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                            tokens.get(tokIndex - 1).getPosition() + ")");
                                }
                                tempScope = 0;
                            }
                            else {
                                System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                                        " ] of type [ " + varType + " ] was assigned to type [ int ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                        tokens.get(tokIndex - 1).getPosition() + ").");
                                errorCount++;
                                tempScope = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    public void stringExpr(){
        String charList = "";

        // check if we have a string expression within an assign so that we can type check the variable
        if(ast.getCurrent().getChildren().size() > 0){
            // check if variable is declared within current scope
            if (symbolTable.get(currentScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()) != null) {
                String varType = symbolTable.get(currentScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).getType();
                if (varType.equals("string")) {
                    symbolTable.get(currentScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setInitialized();
                    if (verboseMode) {
                        System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getChildren().get(0).getName()
                                + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                tokens.get(tokIndex - 1).getPosition() + ")");
                    }
                } else {
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was assigned to type [ string ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }
            // variable was not declared in current scope, check if parent scope exists and search there for variable
            else if (symbolTable.get(currentScope).getParent() != null) {
                int tempScope = currentScope;
                while (symbolTable.get(tempScope).getParent() != null) {
                    if (symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()) != null) {

                        String varType = symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).getType();
                        if (varType.equals("string")) {
                            symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setInitialized();
                            if (verboseMode) {
                                System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getChildren().get(0).getName()
                                        + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                        tokens.get(tokIndex - 1).getPosition() + ")");
                                tempScope = 0;
                            }
                        } else {
                            System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                                    " ] of type [ " + varType + " ] was assigned to type [ string ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                    tokens.get(tokIndex - 1).getPosition() + ").");
                            errorCount++;
                            tempScope = 0;
                        }
                    } else {
                        tempScope = symbolTable.get(tempScope).getParent().getScopeNum();
                        if (tempScope == 0 && symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()) != null) {
                            String varType = symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).getType();

                            if (varType.equals("string")) {
                                symbolTable.get(tempScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setInitialized();
                                if(verboseMode) {
                                    System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getChildren().get(0).getName()
                                            + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                            tokens.get(tokIndex - 1).getPosition() + ")");
                                }
                                tempScope = 0;
                            } else {
                                System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                                        " ] of type [ " + varType + " ] was assigned to type [ string ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                        tokens.get(tokIndex - 1).getPosition() + ").");
                                errorCount++;
                                tempScope = 0;
                            }
                        }
                    }
                }
            }
        }

        // check if we have a character (or space)
        while(checkToken("T_CHAR")){
            charList += tokens.get(tokIndex-1).getValue();
        }

        checkToken("T_QUOTE");
        ast.addNode(charList, "child");
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

            // see if we have another boolean expression
            if((ast.getCurrent().getName().equals("isEqual") || ast.getCurrent().getName().equals("isNotEqual"))
                    && ast.getCurrent().getChildren().size() > 1) {
                String boolExpType = ast.getCurrent().getChildren().get(0).getName();
                // other node is bool val
                if(boolExpType.equals("true") || boolExpType.equals("false")){
                    boolExpType = "boolean";
                }
                // other node is digit
                else if(boolExpType.matches("[0-9]")){
                    boolExpType = "int";
                }
                // other node is string
                else if(boolExpType.matches("[a-z]+[a-z]+")){
                    boolExpType = "string";
                }
                // other node is variable
                else if(boolExpType.matches("[a-z]")){
                    boolExpType = getVariableType(boolExpType);
                }

                // get the other node's type
                String boolExpType2 = ast.getCurrent().getChildren().get(1).getChildren().get(0).getName();
                if(boolExpType2.equals("true") || boolExpType2.equals("false")){
                    boolExpType2 = "boolean";
                }
                // other node is digit
                else if(boolExpType2.matches("[0-9]")){
                    boolExpType2 = "int";
                }
                // other node is string
                else if(boolExpType2.matches("[a-z]+[a-z]+")){
                    boolExpType2 = "string";
                }
                // other node is variable
                else if(boolExpType2.matches("[a-z]")){
                    boolExpType2 = getVariableType(boolExpType2);
                }

                if(!boolExpType.equals(boolExpType2)){
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Incorrect Type Comparison - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + boolExpType + " ] was compared to type [ " + boolExpType2 + " ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }

            }


        }
        // check if we have boolval true
        else if(tokens.get(tokIndex).getKind().equals("T_BOOL_TRUE")){
            checkToken("T_BOOL_TRUE");
            ast.addNode(tokens.get(tokIndex-1).getValue(),"child");

            // check if we are currently doing an assign statement
            if(ast.getCurrent().getName().equals("Assign")) {
                // get the variable type
                String varType = getVariableType(ast.getCurrent().getChildren().get(0).getName());
                // make sure the type is boolean since we are setting it equal to true
                if (varType.equals("boolean")) {
                    int varScope = getVariableScope(ast.getCurrent().getChildren().get(0).getName());
                    symbolTable.get(varScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setInitialized();
                    if (verboseMode) {
                        System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getChildren().get(0).getName()
                                + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                tokens.get(tokIndex - 1).getPosition() + ")");
                    }
                }
                // throw error -> type mismatch
                else {
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was assigned to type [ boolean ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }

            // type check the boolean expression
            else if((ast.getCurrent().getName().equals("isEqual") || ast.getCurrent().getName().equals("isNotEqual"))
                    && ast.getCurrent().getChildren().size() > 1) {

                String varType;
                if(ast.getCurrent().getChildren().get(0).getName().equals("true") ||
                        ast.getCurrent().getChildren().get(0).getName().equals("false")){
                    varType = "boolean";
                }
                else {
                    varType = getVariableType(ast.getCurrent().getChildren().get(0).getName());
                }

                if (varType.equals("boolean")) {
                    int varScope = getVariableScope(ast.getCurrent().getChildren().get(0).getName());
                    if(varScope != -1) {
                        symbolTable.get(varScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setUsed();
                    }
                } else {
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Incorrect Type Comparison - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was compared to type [ boolean ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }
        }
        // check if we have boolval false
        else if(tokens.get(tokIndex).getKind().equals("T_BOOL_FALSE")){
            checkToken("T_BOOL_FALSE");
            ast.addNode(tokens.get(tokIndex-1).getValue(),"child");

            // check if we are currently doing an assign statement
            if(ast.getCurrent().getName().equals("Assign")) {
                // get the variable type
                String varType = getVariableType(ast.getCurrent().getChildren().get(0).getName());
                // make sure the type is boolean since we are setting it equal to true
                if (varType.equals("boolean")) {
                    int varScope = getVariableScope(ast.getCurrent().getChildren().get(0).getName());
                    symbolTable.get(varScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setInitialized();
                    if (verboseMode) {
                        System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getChildren().get(0).getName()
                                + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                tokens.get(tokIndex - 1).getPosition() + ")");
                    }
                }
                // throw error -> type mismatch
                else {
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was assigned to type [ boolean ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }

            // type check the boolean expression
            else if((ast.getCurrent().getName().equals("isEqual") || ast.getCurrent().getName().equals("isNotEqual"))
                    && ast.getCurrent().getChildren().size() > 1) {

                String varType;
                if(ast.getCurrent().getChildren().get(0).getName().equals("true") ||
                        ast.getCurrent().getChildren().get(0).getName().equals("false")){
                    varType = "boolean";
                }
                else {
                    varType = getVariableType(ast.getCurrent().getChildren().get(0).getName());
                }

                if (varType.equals("boolean")) {
                    int varScope = getVariableScope(ast.getCurrent().getChildren().get(0).getName());
                    if(varScope != -1) {
                        symbolTable.get(varScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setUsed();
                    }
                } else {
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Incorrect Type Comparison - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was compared to type [ boolean ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }
        }
    }

    /**
     * Checks if current token equals the passed in token (stolen from my parser but useful for creating AST)
     * @param expectedKind expected token kind
     * @return boolean if token matches
     */
    public boolean checkToken(String expectedKind) {
        boolean tokenMatch = false;

        if (tokens.get(tokIndex).getKind().equals(expectedKind)) {
            tokenMatch = true;
            tokIndex++;
        }

        return tokenMatch;
    }

    /**
     * Check if variable exists in scope and get variable type
     * @param var name
     * @return variable type
     */
    public String getVariableType(String var) {
        String varType = "";

        if (symbolTable.get(currentScope).getScopeItems().get(var) != null) {
            varType = symbolTable.get(currentScope).getScopeItems().get(var).getType();

        } else if (symbolTable.get(currentScope).getParent() != null) {
            int tempScope = currentScope;
            while (symbolTable.get(tempScope).getParent() != null) {
                if (symbolTable.get(tempScope).getScopeItems().get(var) != null) {
                    varType = symbolTable.get(tempScope).getScopeItems().get(var).getType();
                } else {
                    tempScope = symbolTable.get(tempScope).getParent().getScopeNum();
                    if (tempScope == 0 && symbolTable.get(tempScope).getScopeItems().get(var) != null) {
                        varType = symbolTable.get(tempScope).getScopeItems().get(var).getType();
                    }
                }
            }
        }
        return varType;
    }

    /**
     * Check get variable scope
     * @param var name
     * @return variable scope num
     */
    public int getVariableScope(String var) {
       int varScope = -1;

        if (symbolTable.get(currentScope).getScopeItems().get(var) != null) {
            varScope = currentScope;

        } else if (symbolTable.get(currentScope).getParent() != null) {
            int tempScope = currentScope;
            while (symbolTable.get(tempScope).getParent() != null) {
                if (symbolTable.get(tempScope).getScopeItems().get(var) != null) {
                    varScope = tempScope;
                } else {
                    tempScope = symbolTable.get(tempScope).getParent().getScopeNum();
                    if (tempScope == 0 && symbolTable.get(tempScope).getScopeItems().get(var) != null) {
                        varScope = tempScope;
                    }
                }
            }
        }
        return varScope;
    }

}
