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

    /**
     * Creates a new instance of Semantic Analysis
     * @param tokens, verboseMode, passedLex, passedParse, programNum
     */
    public SemanticAnalyzer(ArrayList<Token> tokens, boolean verboseMode, boolean passedLex, boolean passedParse, int programNum) {
        this.tokens = tokens;
        this.verboseMode = verboseMode;

        // make sure Lex and Parse didn't throw any errors before we begin Semantic Analysis
        if(passedLex && passedParse){

            System.out.println("SEMANTIC ANALYSIS: Beginning Semantic Analysis on Program " + programNum + " ...");
            block();
            // get the warnings for unused/uninitialized variables
            warningCount += symbolTable.printWarnings();
            System.out.println("\nProgram " + programNum + " Semantic Analysis produced " + errorCount + " error(s) and " +
                    warningCount + " warning(s).");

            // if no errors were thrown, print AST and symbol table
            if(errorCount == 0){
                System.out.println("\nAST for program " + programNum + " ...");
                System.out.println(ast.toString());
                System.out.println("Program " + programNum + " Symbol Table");
                System.out.println("---------------------------");
                System.out.printf("%-6s%-9s%-7s%-4s\n", "Name", "Type", "Scope", "Line");
                System.out.println("---------------------------");
                symbolTable.printSymbolTable();
            }
            // errors thrown -> stop compilation
            else{
                System.out.println("\nAST for program " + programNum + ": Skipped due to SEMANTIC ANALYSIS error(s)");
                System.out.println("\nSymbol Table for program " + programNum + ": Skipped due to SEMANTIC ANALYSIS error(s)");
            }

        }
        // Lex failed, so don't do semantic analysis
        else if(!passedLex){
            System.out.println("\nSemantic Analysis for program " + programNum + ": Skipped due to LEXER error(s)");
            System.out.println("\nAST for program " + programNum + ": Skipped due to LEXER error(s)");
            System.out.println("\nSymbol Table for program " + programNum + ": Skipped due to LEXER error(s)");
        }
        // Parse failed, so don't do semantic analysis
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
        // output entering new scope
        if(verboseMode) {
            System.out.println("SEMANTIC ANALYSIS: New Scope [ " + currentScope + " ] has been entered at line: "
                    + tokens.get(tokIndex - 1).getLine() + ".");
        }
        // if we are not the first scope, set parent scope to be previous scope
        if(currentScope != 0){
            symbolTable.get(currentScope).setParent(symbolTable.get(prevScope));
            // output parent scope set if not first scope
            if(verboseMode) {
                System.out.println("SEMANTIC ANALYSIS: Scope [ " + currentScope + " ] parent scope has been set to [ "
                        + symbolTable.get(currentScope).getParent().getScopeNum() + " ] at line: " + tokens.get(tokIndex - 1).getLine() + ".");
            }
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
                if(verboseMode) {
                    System.out.println("SEMANTIC ANALYSIS: Exiting scope [ " + currentScope + " ] and entering scope [ "
                            + symbolTable.get(currentScope).getParent().getScopeNum() + " ] at line: " + tokens.get(tokIndex - 1).getLine() + ".");
                }
                currentScope = symbolTable.get(currentScope).getParent().getScopeNum();
                ast.moveParent();
            }
        }
    }

    /**
     * add node to AST for print and call expr() to get content being printed
     * PrintStatement ::== print ( Expr )
     */
    public void printStmt() {
        ast.addNode("Print","branch");
        // skip the opening parenthesis
        tokIndex++;
        expr();
        // skip the closing parenthesis
        tokIndex++;
        ast.moveParent();
    }

    /**
     * add node to AST for assign, Id, and call expr() to get content Id is assigned to.
     * make sure that the Id exists in our scope prior to initializing it
     * AssignStatement ::== Id = Expr
     */
    public void assignStmt(){
        ast.addNode("Assign","branch");
        ast.addNode(tokens.get(tokIndex-1).getValue(),"child");

        // get variable type
        String varType = getVariableType(tokens.get(tokIndex-1).getValue());

        // make sure variable exists (type != empty string) -> else throw error
        if(varType.equals("")){
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

    /**
     * add nodes to AST for variable declaration and make sure that variable is not already declared in scope
     * VarDecl ::== type Id
     */
    public void varDecl() {
        ast.addNode("VariableDeclaration","branch");
        ast.addNode(tokens.get(tokIndex-1).getValue(), "child");
        ast.addNode(tokens.get(tokIndex).getValue(), "child");
        // check that variable doesn't already exist in current scope
        if(symbolTable.get(currentScope).getScopeItems().get(tokens.get(tokIndex).getValue()) == null){
            // add var to symbol table
            SymbolTableItem newItem = new SymbolTableItem(tokens.get(tokIndex-1).getValue(), tokens.get(tokIndex-1).getLine());
            symbolTable.get(currentScope).addItem(tokens.get(tokIndex).getValue(), newItem);
            if(verboseMode) {
                System.out.println("SEMANTIC ANALYSIS: Variable [ " + tokens.get(tokIndex).getValue()
                        + " ] has been declared at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                        tokens.get(tokIndex - 1).getPosition() + ")");
            }
        }
        // variable already exists -> throw error
        else{
            System.out.println("SEMANTIC ANALYSIS: ERROR: Duplicate Variable [ " + tokens.get(tokIndex).getValue() +
                    " ] was declared at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                    tokens.get(tokIndex - 1).getPosition() + ").");
            errorCount++;
        }
        tokIndex++;
        ast.moveParent();
    }

    /**
     * add node to AST for while and call booleanExpr() and block() to continue semantic analysis
     * WhileStatement ::== while BooleanExpr Block
     */
    public void whileStmt() {
        ast.addNode("While","branch");
        booleanExpr();
        block();
        ast.moveParent();
        ast.moveParent();
    }

    /**
     * add node to AST for if and call booleanExpr() and block() to continue semantic analysis
     * IfStatement ::== if BooleanExpr Block
     */
    public void ifStmt() {
        ast.addNode("If","branch");
        booleanExpr();
        block();
        ast.moveParent();
        ast.moveParent();
    }

    /**
     * check type of current token and call corresponding functions. Type/Scope check Ids and add AST nodes.
     * Expr ::== IntExpr | StringExpr | BooleanExpr | Id
     */
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

            // check current scope for variable
            if(symbolTable.get(currentScope).getScopeItems().get(tokens.get(tokIndex-1).getValue()) != null) {
                // set variable is used boolean
                symbolTable.get(currentScope).getScopeItems().get(tokens.get(tokIndex-1).getValue()).setUsed();
                if (verboseMode) {
                    System.out.println("SEMANTIC ANALYSIS: Variable [ " + tokens.get(tokIndex-1).getValue()
                            + " ] has been used at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ")");
                }

                // see if we are using the variable in a Boolean Expression, Addition, or Assign and that the other
                // node is already declared in the tree
                if((ast.getCurrent().getName().equals("isEqual") || ast.getCurrent().getName().equals("isNotEqual")
                         || ast.getCurrent().getName().equals("Assign"))
                        && ast.getCurrent().getChildren().size() > 1) {
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
                    else if(boolExpType.charAt(0) == '"'){
                        boolExpType = "string";
                    }
                    // check if other node is variable
                    else if(boolExpType.matches("[a-z]")){
                        boolExpType = getVariableType(boolExpType);
                    }
                    // check if other node is addition
                    else if(boolExpType.equals("Addition")){
                        boolExpType = "int";
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
                    else if(boolExpType2.charAt(0) == '"'){
                        boolExpType2 = "string";
                    }
                    // other node is variable
                    else if(boolExpType2.matches("[a-z]")){
                        boolExpType2 = getVariableType(boolExpType2);
                    }
                    // check if other node is addition
                    else if(boolExpType2.equals("Addition")){
                        boolExpType2 = "int";
                    }
                    // make sure the two types are equivalent
                    if(!boolExpType.equals(boolExpType2)){
                        if(ast.getCurrent().getChildren().get(0).getName().equals("Addition")){
                            boolExpType = "int";
                        }
                        // throw error -> types not equivalent
                        System.out.println("SEMANTIC ANALYSIS: ERROR: Incorrect Type Comparison - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                                " ] of type [ " + boolExpType + " ] was compared to type [ " + boolExpType2 + " ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                tokens.get(tokIndex - 1).getPosition() + ").");
                        errorCount++;
                    }

                }
            }
            // check parent scope for variable
            else if(symbolTable.get(currentScope).getParent() != null){
                int tempScope = currentScope;
                // while parent scope exists, check for variable existance
                while(symbolTable.get(tempScope).getParent() != null){

                    if(symbolTable.get(tempScope).getParent().getScopeItems().get(tokens.get(tokIndex-1).getValue()) != null) {
                        // set variable is used boolean
                        symbolTable.get(tempScope).getParent().getScopeItems().get(tokens.get(tokIndex-1).getValue()).setUsed();
                        if (verboseMode) {
                            System.out.println("SEMANTIC ANALYSIS: Variable [ " + tokens.get(tokIndex-1).getValue()
                                    + " ] has been used at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                    tokens.get(tokIndex - 1).getPosition() + ")");
                        }

                        // see if we are using the variable in a Boolean Expression, Addition, or Assign and that the other
                        // node is already declared in the tree
                        if((ast.getCurrent().getName().equals("isEqual") || ast.getCurrent().getName().equals("isNotEqual")
                                || ast.getCurrent().getName().equals("Assign"))
                                && ast.getCurrent().getChildren().size() > 1) {
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
                            else if(boolExpType.charAt(0) == '"'){
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
                            else if(boolExpType2.charAt(0) == '"'){
                                boolExpType2 = "string";
                            }
                            // other node is variable
                            else if(boolExpType2.matches("[a-z]")){
                                boolExpType2 = getVariableType(boolExpType2);
                            }

                            // make sure the boolean types are equivalent, if NOT throw error
                            if(!boolExpType.equals(boolExpType2)){
                                if(ast.getCurrent().getChildren().get(0).getName().equals("Addition")){
                                    boolExpType = "int";
                                }
                                // types not equivalent -> throw error
                                System.out.println("SEMANTIC ANALYSIS: ERROR: Incorrect Type Comparison - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                                        " ] of type [ " + boolExpType + " ] was compared to type [ " + boolExpType2 + " ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                        tokens.get(tokIndex - 1).getPosition() + ").");
                                errorCount++;
                            }

                        }

                        tempScope = 0;
                    }
                    // get next parent scope (if exists)
                    else{
                        tempScope = symbolTable.get(tempScope).getParent().getScopeNum();
                        // variable was not declared in parent scope either -> throw error
                        if(tempScope == 0){
                            System.out.println("SEMANTIC ANALYSIS: ERROR: Undeclared variable [ " + tokens.get(tokIndex-1).getValue() +
                                    " ] was used at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                    tokens.get(tokIndex - 1).getPosition() + ") before being declared.");
                            errorCount++;
                        }
                    }
                }
            }
            // variable was not declared  -> throw error
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

    /**
     * add node to AST for digit and check if we have an intOp (if yes, call expr() and type check if we are computing
     * calculations on a variable)
     * IntExpr ::== digit intop Expr
     *         ::== digit
     */
    public void intExpr(){
        // we have an intop
        if(tokens.get(tokIndex).getKind().equals("T_ADDITION_OP")) {
            ast.addNode("Addition","branch");
            ast.addNode(tokens.get(tokIndex-1).getValue(), "child");

            tokIndex++;
            expr();

            // make sure the other half of the int Expression is of type integer
            String boolExpType = ast.getCurrent().getChildren().get(1).getName();

            // other node is bool val
            if(boolExpType.equals("true") || boolExpType.equals("false")){
                boolExpType = "boolean";
            }
            // other node is a boolean expression
            if(boolExpType.equals("isNotEqual") || boolExpType.equals("isEqual")){
                boolExpType = "BooleanExpression";
            }
            // other node is digit
            else if(boolExpType.matches("[0-9]")){
                boolExpType = "int";
            }
            // other node is string
            else if(boolExpType.charAt(0) == '"'){
                boolExpType = "string";
            }
            // other node is variable
            else if(boolExpType.matches("[a-z]")){
                boolExpType = getVariableType(boolExpType);
            }
            // other node is an intOp
            else if(boolExpType.equals("Addition")){
                boolExpType = "int";
            }
            // if the other expression type is NOT int -> throw error
            if(!boolExpType.equals("int")){
                System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - [ IntOp ] of type [ int ] was assigned to type [ " + boolExpType + " ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                        tokens.get(tokIndex - 1).getPosition() + ").");
                errorCount++;
            }

            // check if we are in an assign statement and that the variable exists
            if(ast.getCurrent().getParent().getName().equals("Assign") &&
                    ast.getCurrent().getParent().getChildren().get(0).getName().matches("[a-z]")) {
                // get variable type
                String varType = getVariableType(ast.getCurrent().getParent().getChildren().get(0).getName());

                // make sure type is "int" since we are assigning an int op
                if (varType.equals("int")) {
                    // get scope of variable so we can set it to initialized
                    int varScope = getVariableScope(ast.getCurrent().getParent().getChildren().get(0).getName());
                    symbolTable.get(varScope).getScopeItems().get(ast.getCurrent().getParent().getChildren().get(0).getName()).setInitialized();
                    if (verboseMode) {
                        System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getParent().getChildren().get(0).getName()
                                + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                tokens.get(tokIndex - 1).getPosition() + ")");
                    }
                }
                // variable type was not int -> throw error for type mismatch
                else {
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getParent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was assigned to type [ int ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }
            ast.moveParent();

        }
        // we do not have an intop, so add node for just digit
        else{
            ast.addNode(tokens.get(tokIndex-1).getValue(), "child");
            // check if we are assigning a digit to a variable
            if(ast.getCurrent().getChildren().get(0).getName().matches("[a-z]")) {
                // get variable type
                String varType = getVariableType(ast.getCurrent().getChildren().get(0).getName());
                // make sure type is "int" since we are assigning a number to it
                if (varType.equals("int")) {
                    // get scope of variable so we can set it to initialized
                    int varScope = getVariableScope(ast.getCurrent().getChildren().get(0).getName());
                    symbolTable.get(varScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setInitialized();
                    if (verboseMode) {
                        System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getChildren().get(0).getName()
                                + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                                tokens.get(tokIndex - 1).getPosition() + ")");
                    }
                }
                // variable type was not int -> throw error for type mismatch
                else {
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was assigned to type [ int ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }
            // see if we are using the variable in a Boolean Expression, Addition, or Assign and that the other
            // node is already declared in the tree
            else if((ast.getCurrent().getName().equals("isEqual") || ast.getCurrent().getName().equals("isNotEqual"))
                    && ast.getCurrent().getChildren().size() > 1) {
                String expType = ast.getCurrent().getChildren().get(0).getName();
                // check if other node is bool val
                if(expType.equals("true") || expType.equals("false")){
                    expType = "boolean";
                }
                // check if other node is digit
                else if(expType.matches("[0-9]")){
                    expType = "int";
                }
                // check if other node is string
                else if(expType.charAt(0) == '"'){
                    expType = "string";
                }
                // check if other node is variable
                else if(expType.matches("[a-z]")){
                    expType = getVariableType(expType);
                }
                // check if other node is an intOp
                else if(expType.equals("Addition")){
                    expType = "int";
                }

                // make sure the two types are equivalent
                if(!expType.equals("int")){
                    // throw error -> types not equivalent
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Incorrect Type Comparison - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + expType + " ] was compared to type [ int ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }

            }
        }
    }

    /**
     * add node to AST for CharList and make sure that if we are using a stringExpr with a variable, that the variable
     * is also of type string
     * StringExpr ::== " CharList "
     */
    public void stringExpr(){
        String charList = "";

        // check if we have a string expression within an expression so that we can type check the variable
        if(ast.getCurrent().getChildren().size() > 0){
            // check if variable is declared within current scope
            String varType;
            if(ast.getCurrent().getChildren().get(0).getName().charAt(0) == '"'){
                varType = "string";
            }
            else{
                varType = getVariableType(ast.getCurrent().getChildren().get(0).getName());
            }
            // the variable exists -> is it a string?
            if (varType.equals("string")) {
                int varScope = getVariableScope(ast.getCurrent().getChildren().get(0).getName());
                if(varScope != -1) {
                    symbolTable.get(varScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setInitialized();
                }
                if (verboseMode) {
                    System.out.println("SEMANTIC ANALYSIS: Variable [ " + ast.getCurrent().getChildren().get(0).getName()
                            + " ] has been initialized at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ")");
                }
            }
            // not a string -> throw error
            else {
                // throw error for type mismatch in assign op
                if(ast.getCurrent().getName().equals("Assign")){
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was assigned to type [ string ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
                // throw error for type mismatch in boolean expression
                else if (!ast.getCurrent().getName().equals("Addition")){
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Incorrect Type Comparison - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was compared to type [ string ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }
        }

        // check if we have a character (or space) and append to charList
        while(checkToken("T_CHAR")){
            charList += tokens.get(tokIndex-1).getValue();
        }

        checkToken("T_QUOTE");
        ast.addNode("\"" + charList + "\"", "child");
    }

    /**
     * check for boolVals and isEqual, isNotEqual expressions and add values to AST and perform type and scope checking
     * when necessary
     * BooleanExpr ::== ( Expr boolop Expr )
     *             ::== boolVal
     */
    public void booleanExpr(){
        // check if we have a left parenthesis (signifies beginning of ( Expr boolop Expr ))
        if(tokens.get(tokIndex).getKind().equals("T_L_PAREN")){
            checkToken("T_L_PAREN");
            int count = 0;
            // look ahead to get the equality or inequality op to add to the AST before adding the two expressions
            while (!tokens.get(tokIndex).getKind().equals("T_EQUALITY_OP") &&
                    !tokens.get(tokIndex).getKind().equals("T_INEQUALITY_OP")){
                tokIndex++;
                count++;
            }
            // add isEqual node to AST and reset tokIndex to get first expr
            if(checkToken("T_EQUALITY_OP")){
                ast.addNode("isEqual","branch");
                // reset token count
                tokIndex = tokIndex - count - 1;
            }
            // ad isNot equal node to AST and reset tokIndex to get first expr
            else{
                if(checkToken("T_INEQUALITY_OP")){
                    ast.addNode("isNotEqual","branch");
                    // reset token count
                    tokIndex = tokIndex - count - 1;
                }
            }
            // get first expression in boolean expression
            expr();

            // skip over the equality/inequality op since we already added it to AST before
            if(checkToken("T_EQUALITY_OP")){ }
            else{
                if(checkToken("T_INEQUALITY_OP")){}
            }
            // get second expression in boolean expression
            expr();
            checkToken("T_R_PAREN");
            ast.moveParent();

            // check if we have another boolean expression
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
                else if(boolExpType.charAt(0) == '"'){
                    boolExpType = "string";
                }
                // other node is variable
                else if(boolExpType.matches("[a-z]")){
                    boolExpType = getVariableType(boolExpType);
                }
                // check if other node is an intOp
                else if(boolExpType.equals("Addition")){
                    boolExpType = "int";
                }

                // make sure other node is of type boolean. If it's not -> throw error
                if(!boolExpType.equals("boolean")){
                    if(ast.getCurrent().getChildren().get(0).getName().equals("Addition")){
                        boolExpType = "int";
                    }
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Incorrect Type Comparison - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + boolExpType + " ] was compared to type [ BooleanExpression ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }

            }
            // check if we are currently doing an assign statement and make sure var type is boolean
            else if(ast.getCurrent().getName().equals("Assign")) {
                // get the variable type
                String varType = getVariableType(ast.getCurrent().getChildren().get(0).getName());
                // make sure the type is boolean since we are setting it equal to true
                if (varType.equals("boolean")) {
                    // get variable scope to set to initialized
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
                    if(ast.getCurrent().getChildren().get(0).getName().matches("[0-9]")){
                        varType = "int";
                    }
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was assigned to type [ boolean ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
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
                    // get variable scope to set to initialized
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
                    if(ast.getCurrent().getChildren().get(0).getName().matches("[0-9]")){
                        varType = "int";
                    }
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was assigned to type [ boolean ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }

            // type check the boolean expression if we have true within isEqual or isNotEqual and first node is not empty
            else if((ast.getCurrent().getName().equals("isEqual") || ast.getCurrent().getName().equals("isNotEqual"))
                    && ast.getCurrent().getChildren().size() > 1) {

                String varType;
                // other node is bool val
                if(ast.getCurrent().getChildren().get(0).getName().equals("true") ||
                        ast.getCurrent().getChildren().get(0).getName().equals("false")){
                    varType = "boolean";
                }
                // get variable type from AST
                else {
                    varType = getVariableType(ast.getCurrent().getChildren().get(0).getName());
                }
                // check if other var type is boolean
                if (varType.equals("boolean")) {
                    int varScope = getVariableScope(ast.getCurrent().getChildren().get(0).getName());
                    if(varScope != -1) {
                        symbolTable.get(varScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setUsed();
                    }
                }
                // other var type is not boolean and since we are comparing it to true, throw error for type mismatch
                else {
                    if(ast.getCurrent().getChildren().get(0).getName().equals("Addition")){
                        varType = "int";
                    }
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
                    if(ast.getCurrent().getChildren().get(0).getName().matches("[0-9]")){
                        varType = "int";
                    }
                    System.out.println("SEMANTIC ANALYSIS: ERROR: Type Mismatch - Variable [ " + ast.getCurrent().getChildren().get(0).getName() +
                            " ] of type [ " + varType + " ] was assigned to type [ boolean ] at (" + tokens.get(tokIndex - 1).getLine() + ":" +
                            tokens.get(tokIndex - 1).getPosition() + ").");
                    errorCount++;
                }
            }

            // type check the boolean expression if we have true within isEqual or isNotEqual and first node is not empty
            else if((ast.getCurrent().getName().equals("isEqual") || ast.getCurrent().getName().equals("isNotEqual"))
                    && ast.getCurrent().getChildren().size() > 1) {

                String varType;
                // other node is true/false so type is boolean
                if(ast.getCurrent().getChildren().get(0).getName().equals("true") ||
                        ast.getCurrent().getChildren().get(0).getName().equals("false")){
                    varType = "boolean";
                }
                // get other var type from AST
                else {
                    varType = getVariableType(ast.getCurrent().getChildren().get(0).getName());
                }
                // check if other var type is boolean
                if (varType.equals("boolean")) {
                    int varScope = getVariableScope(ast.getCurrent().getChildren().get(0).getName());
                    if(varScope != -1) {
                        symbolTable.get(varScope).getScopeItems().get(ast.getCurrent().getChildren().get(0).getName()).setUsed();
                    }
                }
                // other var type is not boolean and since we are comparing it to true, throw error for type mismatch
                else {
                    if(ast.getCurrent().getChildren().get(0).getName().equals("Addition")){
                        varType = "int";
                    }
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

        // check if var is declared in current scope
        if (symbolTable.get(currentScope).getScopeItems().get(var) != null) {
            varType = symbolTable.get(currentScope).getScopeItems().get(var).getType();

        }
        // not in current scope -> check parent
        else if (symbolTable.get(currentScope).getParent() != null) {
            int tempScope = currentScope;
            while (symbolTable.get(tempScope).getParent() != null) {
                if (symbolTable.get(tempScope).getScopeItems().get(var) != null) {
                    varType = symbolTable.get(tempScope).getScopeItems().get(var).getType();
                    tempScope = 0;
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
     * Get variable's scope number
     * @param var name
     * @return variable scope num
     */
    public int getVariableScope(String var) {
       int varScope = -1;

       // check if var is declared in current scope
        if (symbolTable.get(currentScope).getScopeItems().get(var) != null) {
            varScope = currentScope;

        }
        // not in current scope -> check parent
        else if (symbolTable.get(currentScope).getParent() != null) {
            int tempScope = currentScope;
            while (symbolTable.get(tempScope).getParent() != null) {
                if (symbolTable.get(tempScope).getScopeItems().get(var) != null) {
                    varScope = tempScope;
                    tempScope = 0;
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
