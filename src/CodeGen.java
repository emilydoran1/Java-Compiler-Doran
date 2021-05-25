import java.util.ArrayList;

/**
 * This class takes the AST and symbol table and generates 6502a machine code for our language grammar.
 *
 * @author Emily Doran
 *
 */
public class CodeGen {

    private boolean verboseMode;
    private int programNum;
    private SyntaxTree ast;
    private SymbolTable symbolTable;
    private StaticVariableTable varTable = new StaticVariableTable();
    private JumpTable jumpTable = new JumpTable();
    private int totalBytesUsed = 0;

    private String opCodeOutput = "";
    private String heapOutput = "";
    private int currentScope = 0;
    private int heapEnd = 256;
    private int tempCount = 0;
    private int scopeCount = 1;

    private int errorCount = 0;

    private boolean insideIf = false;
    private boolean insideIfFirstPass = false;
    private int jumpDist = 0;

    private boolean insideWhile = false;
    private boolean insideWhileFirstPass = false;

    private int startWhile = 0;

    public CodeGen(SyntaxTree ast, SymbolTable symbolTable, int programNum, boolean verboseMode, boolean passedLex, boolean passedParse,
                   boolean passedSemanticAnalysis){
        this.ast = ast;
        this.symbolTable = symbolTable;
        this.programNum = programNum;
        this.verboseMode = verboseMode;

        // make sure Lex, Parse, and Semantic Analysis didn't throw any errors before we begin Code Generation
        if(passedLex && passedParse && passedSemanticAnalysis){
            System.out.println("\n\nCODE GENERATION: Beginning Code Generation on Program " + programNum + " ...");
            Node root = ast.getRoot();

            ArrayList<Node> children = root.getChildren();

            // add the true and false values to the heap
            storeHeap("false");

            storeHeap("true");

            // begin code gen on root node children
            beginCodeGen(children);
            // add end break statement
            opCodeOutput += "00";
            if(verboseMode) {
                System.out.println("CODE GENERATION: Adding Break Statement");
            }
            totalBytesUsed += 1;

            // get difference between code and heap to fill with zeros
            int difference = (256 - totalBytesUsed) - (256-heapEnd);

            String totalBytesUsedHex = Integer.toHexString(totalBytesUsed);

            // make sure we didn't exceed 256 bytes
            if(difference < 0){
                errorCount++;
                System.out.println("CODE GENERATION: ERROR: Exceeded Stack Memory Limit. ");
            }
            // fill zeros
            else if (errorCount == 0){
                // add in zeros for empty bytes
                for (int i = 0; i < difference; i++) {
                    opCodeOutput += "00";
                }

                // add heap to output
                opCodeOutput += heapOutput;

                if (totalBytesUsedHex.length() < 2) {
                    totalBytesUsedHex = "0" + totalBytesUsedHex;
                }

                // set each static variable's address
                for (int i = 0; i < varTable.getNumVariables(); i++) {
                    varTable.getVariableTable().get(i).setAddress(totalBytesUsed + i);
                }

                // backpatch static variables
                for (int i = 0; i < varTable.getNumVariables(); i++) {
                    String temp = varTable.getVariableTable().get(i).getTemp();
                    String hexTemp = Integer.toHexString(varTable.getVariableTable().get(i).getAddress()).toUpperCase();
                    if (hexTemp.length() < 2) {
                        hexTemp = "0" + hexTemp;
                    }
                    opCodeOutput = opCodeOutput.replace(temp, hexTemp + "00");
                    if(verboseMode){
                        System.out.println("CODE GENERATION: Backpatching Static Variable Placeholder " + temp +
                                " With Memory Address " + hexTemp);
                    }
                }
                // backpatch jump table
                for (int i = 0; i < jumpTable.getNumVariables(); i++) {
                    String tempJump = jumpTable.getItem("J" + i).getTemp();
                    String hexTemp = Integer.toHexString(jumpTable.getItem("J" + i).getDistance()).toUpperCase();
                    if (hexTemp.length() < 2) {
                        hexTemp = "0" + hexTemp;
                    }
                    opCodeOutput = opCodeOutput.replace(tempJump, hexTemp);
                    if(verboseMode){
                        System.out.println("CODE GENERATION: Backpatching Jump Variable Placeholder " + tempJump +
                                " Forward " + hexTemp + " Addresses");
                    }
                }

                System.out.println("Program " + programNum + " Code Generation Passed With " + errorCount + " error(s)");

                // print static var table and jump table if no errors thrown in code gen
                if(errorCount == 0) {
                    System.out.println("\nProgram " + programNum + " Static Variable Table");
                    System.out.println("---------------------------------");
                    System.out.printf("%-6s%-7s%-9s%-4s\n", "Name", "Temp", "Address", "Scope");
                    System.out.println("---------------------------------");
                    varTable.printStaticVariableTable();

                    System.out.println("\nProgram " + programNum + " Jump Table");
                    System.out.println("----------------------");
                    System.out.printf("%-6s%-7s\n", "Temp", "Distance");
                    System.out.println("----------------------");
                    jumpTable.printJumpTable();

                    System.out.println("\nProgram "  + programNum + " Machine Code:\n" + outputToString() + "\n");
                }
            }

            // print error message if errors thrown
            if(errorCount > 0){
                System.out.println("Program " + programNum + " Code Generation Failed With " + errorCount + " error(s)");
            }

        }
        // Lex failed, so don't do code generation
        else if(!passedLex){
            System.out.println("\nCode Generation for program " + programNum + ": Skipped due to LEXER error(s)");
        }
        // Parse failed, so don't do code generation
        else if(!passedParse){
            System.out.println("\nCode Generation for program " + programNum + ": Skipped due to PARSER error(s)");
        }
        // Semantic analysis failed, so don't do code generation
        else{
            System.out.println("\nCode Generation for program " + programNum + ": Skipped due to SEMANTIC ANALYSIS error(s)");
        }
    }

    /**
     * Perform Depth First Traversal on the ArrayList of nodes
     * @param children to perform dft on
     */
    public void beginCodeGen(ArrayList<Node> children){

        // iterate through each child node
        for(Node child: children){
            // check if it is a branch node
            if(child.getChildren().size() > 0){
                // get child's children
                ArrayList<Node> childChildren = child.getChildren();
                // check if we are getting into an if statement
                if(child.getName().equals("If")){
                    insideIf = true;
                    insideIfFirstPass = true;
                    // call function on the children of the if
                    beginCodeGen(childChildren);
                    int numJumpItems = jumpTable.getNumVariables();
                    // set jump distance once end of if statement is reached
                    if(errorCount == 0) {
                        System.out.println(jumpDist);
                        jumpTable.getItem("J" + (numJumpItems-1)).setDistance(jumpDist);
                        jumpDist = 0;
                    }
                    insideIf = false;
                }
                // check if we are getting into a while statement
                else if(child.getName().equals("While")){
                    insideWhile = true;
                    insideWhileFirstPass = true;
                    insideIfFirstPass = true;
                    // call function on the children of the while
                    beginCodeGen(childChildren);

                    // add jump variable to loop back around
                    int numJumpItems = jumpTable.getNumVariables();
                    JumpTableItem temp = new JumpTableItem("J"+ numJumpItems);
                    jumpTable.addItem(temp);

                    String backOpCode = "";

                    backOpCode += "A9008D0000";
                    backOpCode += "A201EC0000D0" + jumpTable.getItem("J" + numJumpItems).getTemp();

                    totalBytesUsed += backOpCode.length() / 2;

                    opCodeOutput += backOpCode;

                    jumpDist += backOpCode.length()/2;

                    // get distance to loop back to
                    int backToLoop = 256 - opCodeOutput.length()/2 + startWhile;

                    insideWhile = false;
                    startWhile = 0;

                    // get jump distance and update jump table item
                    if(errorCount == 0) {
                        jumpTable.getItem("J" + numJumpItems).setDistance(backToLoop);
                        jumpTable.getItem("J" + (numJumpItems-1)).setDistance(jumpDist);
                        jumpDist = 0;
                    }
                }
                // check if node is a print statement
                else if(child.getName().equals("Print")){
                    // printing boolean value
                    if(child.getChildren().get(0).getName().equals("true") || child.getChildren().get(0).getName().equals("false")){
                        initializePrintBoolean(child.getChildren().get(0).getName());
                    }
                    // printing string
                    else if(child.getChildren().get(0).getName().charAt(0) == '"'){
                        initializePrintString(child.getChildren().get(0).getName());
                    }
                    // printing addition operation
                    else if(child.getChildren().get(0).getName().equals("Addition")){
                        printAddInts(child.getChildren().get(0).getChildren().get(0), child.getChildren().get(0).getChildren().get(1), currentScope);
                        String opCode = "A201FF";
                        totalBytesUsed += opCode.length()/2;
                        opCodeOutput += opCode;

                    }
                    // printing boolean isNotEqual expression
                    else if(child.getChildren().get(0).getName().equals("isNotEqual")){
                        compareValues(child.getChildren().get(0).getChildren().get(0), child.getChildren().get(0).getChildren().get(1),
                                true, false);
                    }
                    // printing boolean isEqual expression
                    else if(child.getChildren().get(0).getName().equals("isEqual")){
                        compareValues(child.getChildren().get(0).getChildren().get(0), child.getChildren().get(0).getChildren().get(1),
                                true, true);
                    }
                    // printing number
                    else {
                        initializePrint(child.getChildren().get(0).getName().charAt(0), currentScope);
                    }
                }
                // storing addition expression
                else if(child.getName().equals("Addition")){
                        storeAddInts(child.getParent().getChildren().get(0).getName().charAt(0), child.getChildren().get(0), child.getChildren().get(1), currentScope);
                }
                // check boolean values isEqual (within if or while)
                else if(child.getName().equals("isEqual") && ((insideIf || insideWhile) && (insideWhileFirstPass || insideIfFirstPass))){
                    compareValues(child.getChildren().get(0), child.getChildren().get(1), false, true);
                }
                // check boolean values isNotEqual (within if or while)
                else if(child.getName().equals("isNotEqual") && ((insideIf || insideWhile) && (insideWhileFirstPass || insideIfFirstPass))){
                    compareValues(child.getChildren().get(0), child.getChildren().get(1), false, false);
                }
                else{
                    // check if we have a nested block
                    if(child.getName().equals("BLOCK")){
                        scopeCount++;
                        currentScope = scopeCount-1;
                        // call function on the children to get depth first traversal
                        if(childChildren != null) {
                            beginCodeGen(childChildren);
                        }
                        // update scope
                        if(currentScope != 0) {
                            currentScope = symbolTable.get(currentScope).getParent().getScopeNum();
                        }
                    }
                    // call function on the children to get the depth first traversal
                    else{
                        beginCodeGen(childChildren);
                    }

                }
            }
            // leaf node
            else{
                // variable declaration
                if(child.getName().equals("int") || child.getName().equals("boolean") || child.getName().equals("string")){
                    declareVariable(child.getParent().getChildren().get(1).getName().charAt(0), currentScope);
                }
                // assigning var to int
                else if(child.getParent().getChildren().size() > 1 && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().matches("[0-9]")){
                    assignStmtInt(child.getName().charAt(0), child.getParent().getChildren().get(1).getName(), currentScope);
                }
                // assigning var to string
                else if(child.getParent().getChildren().size() > 1 && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().charAt(0) == '\"'){
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1), currentScope);
                }
                // assigning var to true
                else if(child.getParent().getChildren().size() > 1 && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().equals("true")){
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1), currentScope);
                }
                // assigning var to false
                else if(child.getParent().getChildren().size() > 1 && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().equals("false")){
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1), currentScope);
                }
                // assigning var to var
                else if(child.getParent().getName().equals("Assign") && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().matches("[a-z]")){
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1), currentScope);
                }
                // assigning var to boolean expression
                else if(child.getParent().getName().equals("Assign") && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().matches("(isEqual)|(isNotEqual)")){
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1), currentScope);
                }
                // check if we are in an if condition and the boolean expression is just true | false
                else if((child.getName().equals("true") || child.getName().equals("false")) &&
                        (child.getParent().getName().equals("If") || child.getParent().getName().equals("While"))){
                    BoolOpWithoutExpr(child);

                }
            }
        }

    }

    /**
     * Add op codes for variable declaration
     * @param variableName, scope of variable
     */
    public void declareVariable(char variableName, int scope){
        int numVars = varTable.getNumVariables();

        // create new static var item for the variable
        StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", variableName, scope);
        varTable.addItem(newItem);
        String opCode = "";

        // if boolean set default to false
        if(getVariableType(Character.toString(variableName)).equals("boolean")){
            opCode += "A9FA8D" + newItem.getTemp();
        }
        // if int/string set default to 0
        else{
            opCode += "A9008D" + newItem.getTemp();
        }

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

        // if inside if statement or while statement append to jump
        if(insideIf || insideWhile){
            jumpDist += opCode.length()/2;
        }

        if(verboseMode) {
            System.out.println("CODE GENERATION: Adding Variable Declaration of Variable: " + variableName);
        }
    }

    /**
     * Assign variable an integer value
     * @param variableName, value to assign, scope of variable
     */
    public void assignStmtInt(char variableName, String value, int scope){
        // load value, store in temp location
        String opCode = "A90" + value + "8D" + varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp();

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

        // if inside if statement or while statement append to jump
        if(insideIf || insideWhile){
            jumpDist += opCode.length()/2;
        }

        if(verboseMode) {
            System.out.println("CODE GENERATION: Assigning Variable " + variableName + " to value: " + value);
        }
    }

    /**
     * Assign variable a string value
     * @param variableName, Node of var, scope of variable
     */
    public void assignStmtString(char variableName, Node node, int scope){
        // get node value being assigned
        String value = node.getName();

        String opCode = "";

        // if you are assigning it to the value of another variable
        if(value.matches("[a-z]") && variableName != value.charAt(0)){
            // load that temp location and store in variableName temp
            int tempScope1 = getVariableScope(value);
            opCode += "AD" + varTable.getItem(value.charAt(0), tempScope1).getTemp() + "8D" +
                    varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp();


            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

            if(insideIf || insideWhile){
                jumpDist += opCode.length()/2;
            }

            if(verboseMode) {
                System.out.println("CODE GENERATION: Assigning Variable " + variableName + " to variable: " + value);
            }
        }
        // not assigning variable to another variable
        else if(variableName != value.charAt(0)){
            String end = "";
            // check if assigning bool value and call heap memory location
            if(value.equals("true") || value.equals("false")){
                if(value.equals("false")){
                    end = "FA";
                }
                else{
                    end = "F5";
                }
                opCode += "A9" + end + "8D" + varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp();

            }
            // assigning variable to boolean expression
            else if(value.equals("isEqual") || value.equals("isNotEqual")){
                // make sure we don't have nested boolean (it is not supported)
                if(!node.getChildren().get(0).equals("isEqual") && !node.getChildren().get(0).equals("isNotEqual") &&
                        !node.getChildren().get(1).equals("isEqual") && !node.getChildren().get(0).equals("isNotEqual")) {
                    // compare the values for isEqual
                    if (value.equals("isEqual")) {
                        compareValues(node.getChildren().get(0), node.getChildren().get(1), false, true);
                        end = varTable.getItem(Character.forDigit(tempCount - 1, 10), -1).getTemp();
                    }
                    // compare the values for isNotEqual
                    else {
                        compareValues(node.getChildren().get(0), node.getChildren().get(1), false, false);
                        end = varTable.getItem(Character.forDigit(tempCount - 1, 10), -1).getTemp();
                    }
                    // load end positon(will be true or false) and store in variable temp location
                    opCode += "AD" + end + "8D" + varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp();

                }
                // throw error for nested boolean
                else{
                    errorCount++;
                    System.out.println("CODE GENERATION: ERROR: Nested Boolean Expressions are not supported.");
                }
            }
            // assigning string value to variable
            else {
                // store the value in the heap
                storeHeap(value);

                // get location in heap of newly stored value
                end = Integer.toHexString(heapEnd).toUpperCase();
                if (end.length() < 2) {
                    end = "0" + end;
                }
                // load the heap end and store in variable temp location
                opCode += "A9" + end + "8D" + varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp();

            }

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

            // if inside if statement or while statement add to jump
            if(insideIf || insideWhile){
                jumpDist += opCode.length()/2;
            }

            if(verboseMode) {
                System.out.println("CODE GENERATION: Assigning Variable " + variableName + " to value: " + value);
            }
        }

    }

    /**
     * compare integer addition operation
     * @param node1, node2, scope
     */
    public void compareAddInts(Node node1, Node node2, int scope){
        int numVars = varTable.getNumVariables();

        // create new temporary item
        StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
        varTable.addItem(newItem);

        // get node values
        String value1 = node1.getName();
        String value2 = node2.getName();

        String opCode = "";

        // second value is a variable and we don't have any more nested integer expressions
        if(!value2.matches("[0-9]") && !value2.equals("Addition")){
            // load first value and save in temp item1
            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            numVars = varTable.getNumVariables();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
            varTable.addItem(newItem2);

            // add value of variable to accumulator
            opCode += "6D" + varTable.getItem(value2.charAt(0), getVariableScope(value2)).getTemp();

            // store in temp 2 variable
            opCode += "8D" + newItem2.getTemp() + "AD" + newItem2.getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

            // add to jump if inside if/while
            if((insideIf || insideWhile) && !insideIfFirstPass){
                jumpDist += opCode.length()/2;
            }
        }
        // nested addition op
        else if(value2.equals("Addition")){
            // call function on the nested op
            compareAddInts(node2.getChildren().get(0), node2.getChildren().get(1), scope);

            // load the first node value
            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            // after ending recursion, add the first digit to the accumulated result
            opCode += "A9006D" + varTable.getItem(Character.forDigit(tempCount-1,10), -1).getTemp();

            opCode += "6D" + newItem.getTemp();

            // store and load accumulator
            opCode += "8D" + varTable.getItem(Character.forDigit(tempCount-1,10), -1).getTemp() + "AD" + varTable.getItem(Character.forDigit(tempCount-1,10), -1).getTemp();

            // store accumulator in first temp
            opCode += "8D" + newItem.getTemp() + "AD" + newItem.getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

            // add to jump if inside if/while
            if((insideIf || insideWhile) && !insideIfFirstPass){
                jumpDist += opCode.length()/2;
            }

        }
        // just adding two ints
        else{
            // store first value
            opCode += "A90" +value1 + "8D" + newItem.getTemp();
            // add second value to the accumulator
            opCode += "A90" + value2 + "6D" + newItem.getTemp();

            numVars = varTable.getNumVariables();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
            varTable.addItem(newItem2);

            // store the accumulator in new temp
            opCode += "8D" + newItem2.getTemp() + "AD" + newItem2.getTemp();;

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

            // add to jump if inside if/while
            if((insideIf || insideWhile) && !insideIfFirstPass){
                jumpDist += opCode.length()/2;
            }
        }

    }

    /**
     * Print integer addition operation
     * @param node1, node2, scope
     */
    public void printAddInts(Node node1, Node node2, int scope){
        int numVars = varTable.getNumVariables();

        // create new temporary item
        StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
        varTable.addItem(newItem);

        // get node values
        String value1 = node1.getName();
        String value2 = node2.getName();

        String opCode = "";

        // second value is a variable and we don't have any more nested integer expressions
        if(!value2.matches("[0-9]") && !value2.equals("Addition")){
            // load first value and save in temp item1
            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            numVars = varTable.getNumVariables();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
            varTable.addItem(newItem2);

            // add value of variable to accumulator
            opCode += "6D" + varTable.getItem(value2.charAt(0), getVariableScope(value2)).getTemp();

            // store in temp 2 variable
            opCode += "8D" + newItem2.getTemp() + "AD" + newItem2.getTemp();

            // print temp 2 value
            opCode += "A201AC" + newItem2.getTemp();


            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

            // add to jump if in if/while
            if(insideIf || insideWhile){
                jumpDist += opCode.length()/2;
            }
        }
        // nested addition op
        else if(value2.equals("Addition")){
            // call function on the nested op
            printAddInts(node2.getChildren().get(0), node2.getChildren().get(1), scope);

            // load the first node value
            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            // after ending recursion, add the first digit to the accumulated result
            opCode += "A9006D" + varTable.getItem(Character.forDigit(tempCount-1,10), -1).getTemp();

            opCode += "6D" + newItem.getTemp();

            // store and load accumulator
            opCode += "8D" + varTable.getItem(Character.forDigit(tempCount-1,10), -1).getTemp() + "AD" + varTable.getItem(Character.forDigit(tempCount-1,10), -1).getTemp();

            // store accumulator in first temp
            opCode += "8D" + newItem.getTemp();

            // print the accumulator value
            opCode += "A201AC" + newItem.getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

            // add to jump if inside if/while
            if(insideIf || insideWhile){
                jumpDist += opCode.length()/2;
            }

        }
        // just adding two ints
        else{
            // store first value
            opCode += "A90" +value1 + "8D" + newItem.getTemp();
            // add second value to the accumulator
            opCode += "A90" + value2 + "6D" + newItem.getTemp();

            numVars = varTable.getNumVariables();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
            varTable.addItem(newItem2);

            // store the accumulator in new temp
            opCode += "8D" + newItem2.getTemp();

            // print value
            opCode += "A201AC" + newItem2.getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

            // add to jump if inside if/while
            if(insideIf || insideWhile){
                jumpDist += opCode.length()/2;
            }
        }

        if(verboseMode) {
            System.out.println("CODE GENERATION: Printing Addition Operation: " + value1 + " + " + value2);
        }

    }

    /**
     * Store integer addition operation
     * @param var to store in, node1, node2, scope
     */
    public void storeAddInts(char var, Node node1, Node node2, int scope){
        int numVars = varTable.getNumVariables();

        StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
        varTable.addItem(newItem);

        numVars = varTable.getNumVariables();

        // get node values
        String value1 = node1.getName();
        String value2 = node2.getName();

        String opCode = "";

        // second value is a variable and we don't have any more nested integer expressions
        if(!value2.matches("[0-9]") && !value2.equals("Addition")){
            // load first value and store in temp
            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
            varTable.addItem(newItem2);

            // add variable current value to the accumulator
            opCode += "A9006D" + varTable.getItem(var, getVariableScope(Character.toString(var))).getTemp();

            // add first value to accumulator
            opCode += "6D" + newItem.getTemp();

            // store in second temp
            opCode += "8D" + newItem2.getTemp() + "AD" + newItem2.getTemp();

            // store accumulator in variable
            opCode += "8D" + varTable.getItem(var, getVariableScope(Character.toString(var))).getTemp();


            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

            // add to jump if inside if/while
            if(insideIf || insideWhile){
                jumpDist += opCode.length()/2;
            }
        }
        // nested addition op
        else if(value2.equals("Addition")){
            // call function on the nested op
            storeAddInts(var, node2.getChildren().get(0), node2.getChildren().get(1), scope);

            // load initial first value and store in temp
            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            // after ending recursion, add the first digit to the accumulated result
            opCode += "A9006D" + varTable.getItem(Character.forDigit(tempCount-1,10), -1).getTemp();

            // store in variable
            opCode += "A9006D" + varTable.getItem(var, getVariableScope(Character.toString(var))).getTemp();

            // store accumulator in first temp item
            opCode += "6D" + newItem.getTemp();

            // load accumulator with variable value
            opCode += "8D" + varTable.getItem(Character.forDigit(tempCount-1,10), -1).getTemp() + "AD" +
                    varTable.getItem(Character.forDigit(tempCount-1,10), -1).getTemp();

            opCode += "8D" + varTable.getItem(var, getVariableScope(Character.toString(var))).getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

            // add to jump if inside if/while
            if(insideIf || insideWhile){
                jumpDist += opCode.length()/2;
            }

        }
        // just adding two ints
        else{
            // load and store first value
            opCode += "A90" +value1 + "8D" + newItem.getTemp();
            // add second value to accumulator
            opCode += "A90" + value2 + "6D" + newItem.getTemp();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
            varTable.addItem(newItem2);

            // store accumulator in second temp
            opCode += "8D" + newItem2.getTemp();

            // load value to y register
            opCode += "A201AC" + newItem2.getTemp();

            // store result in variable
            opCode += "AC" +  newItem2.getTemp() + "8D" +  varTable.getItem(var, getVariableScope(Character.toString(var))).getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

            // add to jump if inside if/while
            if(insideIf || insideWhile){
                jumpDist += opCode.length()/2;
            }
        }

        if(verboseMode) {
            System.out.println("CODE GENERATION: Storing Addition Operation: " + value1 + " + " + value2 + " in variable: " + var);
        }

    }

    /**
     * Store string in heap
     * @param value to store
     */
    public void storeHeap(String value){
        // if we have a string, ignore the quotes
        if(value.charAt(0) == '\"'){
            value = value.substring(1, value.length()-1);
        }

        String appendHeapOut = "";
        // iterate through string and add to heap
        for(int i=0; i < value.length(); i++){
            // get hex value of char
            String tempHeapOut = Integer.toHexString((int) value.charAt(i)).toUpperCase();
            // make sure it is a byte
            if(tempHeapOut.length() < 2){
                tempHeapOut = "0" + tempHeapOut;
            }
            appendHeapOut += tempHeapOut;
            heapEnd--;
        }
        appendHeapOut += "00";
        heapEnd--;

        // append to the heap
        heapOutput = appendHeapOut + heapOutput;

        if(verboseMode) {
            System.out.println("CODE GENERATION: Storing value: " + value + " in heap at location: " + heapEnd);
        }
    }

    /**
     * Print a variable value
     * @param variableName, scope
     */
    public void initializePrint(char variableName, int scope){
        String opCode = "";

        // check if we are printing a variable
        if(Character.toString(variableName).matches("[a-z]")){
            //  var is an int -> load integer value from memory
            if(getVariableType(Character.toString(variableName)).equals("int")) {
                opCode += "AC" + varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp() + "A201FF";
            }
            // var is a string or boolean -> load string value from heap
            else if (getVariableType(Character.toString(variableName)).equals("string")
                || getVariableType(Character.toString(variableName)).equals("boolean")) {
                opCode += "AC" + varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp() + "A202FF";

            }

            if(verboseMode) {
                System.out.println("CODE GENERATION: Printing variable: " + variableName);
            }
        }
        // check if printing an integer
        else if(Character.toString(variableName).matches("[0-9]")){
            opCode += "A00" + Character.toString(variableName) + "A201FF";

            if(verboseMode) {
                System.out.println("CODE GENERATION: Printing value: " + variableName);
            }
        }

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

        // add to jump if inside if/while
        if(insideIf || insideWhile){
            jumpDist += opCode.length()/2;
        }

    }

    /**
     * Print boolean value
     * @param val to print
     */
    public void initializePrintBoolean(String val){
        String opCode = "";

        // boolean values are pre-stored, so we don't need to re store them
        String end;
        // check if val == false -> set point in heap to get from to be false location
        if(val.equals("false")){
            end = "FA";
        }
        // val == true -> set point in heap to get from to be true location
        else{
            end = "F5";
        }

        opCode += "A0" + end + "A202FF";

        // add to jump if inside if/while
        if(insideIf || insideWhile){
            jumpDist += opCode.length()/2;
        }

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

        if(verboseMode) {
            System.out.println("CODE GENERATION: Printing value: " + val);
        }

    }

    /**
     * Print string value
     * @param val to print
     */
    public void initializePrintString(String val){
        String opCode = "";

        // store value in heap
        storeHeap(val);

        // get beginning location of string from heap
        String end = Integer.toHexString(heapEnd).toUpperCase();
        if(end.length() < 2){
            end = "0" + end;
        }

        // load y with value from heap and print value at location
        opCode += "A0" + end + "A202FF";

        // add to jump if inside if/while
        if(insideIf || insideWhile){
            jumpDist += 5;
        }

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

        if(verboseMode) {
            System.out.println("CODE GENERATION: Printing value: " + val);
        }

    }

    /**
     * Compare two nodes within an isEqual/isNotEqual Boolean Expression
     * @param node1, node2, boolean if inside print statement, boolean isEqual -> (true if ==) (false if !=)
     */
    public void compareValues(Node node1, Node node2, boolean inPrint, boolean isEqual){
        // declare start location if inside while for jump back
        if(insideWhileFirstPass) {
            startWhile = opCodeOutput.length() / 2;
        }

        String opCode = "";

        String val1 = node1.getName();
        String val2 = node2.getName();

        if(verboseMode) {
            if (isEqual) {
                System.out.println("CODE GENERATION: Comparing values: " + val1 + " and " + val2 + " in equality operation.");
            } else {
                System.out.println("CODE GENERATION: Comparing values: " + val1 + " and " + val2 + " in inequality operation.");
            }
        }

        // check if values are ints
        if(!val1.equals("isEqual") && !val1.equals("isNotEqual") && !val2.equals("isEqual") && !val2.equals("isNotEqual")) {
            if ((val1.matches("[0-9]") || val1.equals("Addition")) && (val2.matches("[0-9]") || val2.equals("Addition"))) {
                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                varTable.addItem(newItem);

                // val 1 is digit
                if(val1.matches("[0-9]")) {
                    // store first integer
                    opCode += "A90" + val1 + "8D" + newItem.getTemp();
                }
                // comparing addition op
                else{
                    // call function to add the numbers
                    compareAddInts(node1.getChildren().get(0), node1.getChildren().get(1), currentScope);
                    opCode += "8D" + newItem.getTemp();
                }

                numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                varTable.addItem(newItem2);

                // val 1 is digit
                if(val2.matches("[0-9]")) {
                    // store second integer
                    opCode += "A90" + val2 + "8D" + newItem2.getTemp();
                }
                // comparing addition op
                else{
                    // call function to add the numbers
                    compareAddInts(node2.getChildren().get(0), node2.getChildren().get(1), currentScope);
                    // store accumulator in temp
                    opCode += "8D" + newItem2.getTemp();
                }

                // compare both integers and set z flag
                opCode += "AE" + newItem.getTemp();
                opCode += "EC" + newItem2.getTemp();

                numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem3 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                varTable.addItem(newItem3);

                // within != expression
                if (!isEqual) {
                    // set temp variable to be true
                    opCode += "A9F58D" + newItem3.getTemp();

                    // if z flag from int comparison is true, stop over next set
                    opCode += "D005";

                    // z flag was false, set temp variable to be false
                    opCode += "A9FA8D" + newItem3.getTemp();

                    // check if inside if statement or while statement
                    if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                        // compare true to the value set in temp item
                        opCode += "A2F5EC" + newItem3.getTemp();

                        // create jump variable for if/while
                        int numJumpItems = jumpTable.getNumVariables();
                        JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                        jumpTable.addItem(tempJumpItem);
                        opCode += "D0" + tempJumpItem.getTemp();
                    }
                    // not inside if/while
                    else{
                        // compare true to the value set in temp item
                        opCode += "A2F5EC" + newItem3.getTemp();
                        // if true, skip over next set of instructions
                        opCode += "D005";
                    }

                    // z flag was false, set item to be true
                    opCode += "A9F58D" + newItem3.getTemp();

                    // add to jump distance
                    if((insideIf || insideWhile) && insideIfFirstPass){
                        jumpDist += 5;
                    }

                }
                // within == expression
                else {
                    // set temp variable to be false
                    opCode += "A9FA8D" + newItem3.getTemp();

                    // if z flag from int comparison is true, stop over next set
                    opCode += "D005";

                    // z flag was false, set temp variable to be false
                    opCode += "A9F58D" + newItem3.getTemp();

                    // check if inside if statement or while statement
                    if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                        // compare true to the value set in temp item
                        opCode += "A2F5EC" + newItem3.getTemp();

                        // create jump variable for if/while
                        int numJumpItems = jumpTable.getNumVariables();
                        JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                        jumpTable.addItem(tempJumpItem);
                        opCode += "D0" + tempJumpItem.getTemp();
                    }
                    // not inside if/while
                    else{
                        // compare false to the value set in temp item
                        opCode += "A2FAEC" + newItem3.getTemp();
                        // if true, skip over next set of instructions
                        opCode += "D005";
                    }

                    // z flag was false, set item to be false
                    opCode += "A9FA8D" + newItem3.getTemp();

                    // add to jump distance
                    if((insideIf || insideWhile) && insideIfFirstPass){
                        jumpDist += 5;
                    }

                }

                // if inside print print the true/false value stored
                if (inPrint) {
                    opCode += "A202AC" + newItem3.getTemp() + "FF";
                    if((insideIf || insideWhile) && insideIfFirstPass){
                        jumpDist += 6;
                    }
                }

                // add to jump if inside if/while and not in first pass
                if((insideIf || insideWhile) && !insideIfFirstPass && !insideWhileFirstPass){
                    jumpDist += opCode.length()/2;
                }

                totalBytesUsed += opCode.length() / 2;

                opCodeOutput += opCode;
            }
            // check if values are both booleans
            else if (val1.matches("(true|false)") && val2.matches("(true|false)")) {
                String endVal1;
                String endVal2;

                // check val1 true/false
                if (val1.equals("false")) {
                    endVal1 = "FA";
                } else {
                    endVal1 = "F5";
                }
                // check val2 true/false
                if (val2.equals("false")) {
                    endVal2 = "FA";
                } else {
                    endVal2 = "F5";
                }
                opCode += "AE" + endVal1 + "00";

                // set z flag based on boolean comparison
                opCode += "EC" + endVal2 + "00";

                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                varTable.addItem(newItem1);

                // check if we are in an isNotEqual op
                if (!isEqual) {
                    // set temp variable to be true
                    opCode += "A9F58D" + newItem1.getTemp();

                    // if z flag from int comparison is true, skip over next set
                    opCode += "D005";

                    // z flag was false, set temp variable to be false
                    opCode += "A9FA8D" + newItem1.getTemp();

                    // compare true to the temp value
                    opCode += "A2F5EC" + newItem1.getTemp();

                    // check if inside if statement/while statement
                    if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                        // create jump table item
                        int numJumpItems = jumpTable.getNumVariables();
                        JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                        jumpTable.addItem(tempJumpItem);

                        // jump to temp location which will later be backpatched
                        opCode += "D0" + tempJumpItem.getTemp();
                    }
                    // not inside if/while skip over storing true in temp
                    else {
                        opCode += "D005";
                    }

                    // z flag was false, set item to be true
                    opCode += "A9F58D" + newItem1.getTemp();

                    // add to jump if inside if/while
                    if((insideIf || insideWhile) && insideIfFirstPass){
                        jumpDist += 5;
                    }

                }
                // in an isEqual op
                else {
                    // check if inside if/while statement
                    if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                        // set temp to be false
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // if z flag from prev comparison was true, skip next instruction
                        opCode += "D005";

                        // if z flag was false, set temp to be true
                        opCode += "A9F58D" + newItem1.getTemp();

                        // compare true to temp value
                        opCode += "A2F5EC" + newItem1.getTemp();

                        int numJumpItems = jumpTable.getNumVariables();

                        // create jump table item
                        JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                        jumpTable.addItem(tempJumpItem);

                        // jump to temp location which will later be backpatched
                        opCode += "D0" + tempJumpItem.getTemp();

                        // store false in temp
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // if in the first pass of the if/while add to jump
                        if(insideIfFirstPass || insideWhileFirstPass) {
                            jumpDist += 5;
                        }
                    }
                    // not inside if/while statement
                    else {
                        // set temp to be false
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // if z flag from prev comparison was true, skip next instruction
                        opCode += "D005";

                        // if z flag was false, set temp to be true
                        opCode += "A9F58D" + newItem1.getTemp();

                        // compare false to temp value
                        opCode += "A2FAEC" + newItem1.getTemp();

                        // if z flag is true, skip next instruction
                        opCode += "D005";

                        // if z flag was false, set temp item to false
                        opCode += "A9FA8D" + newItem1.getTemp();
                    }

                }

                // if inside print statement, print the true/false value
                if (inPrint) {
                    opCode += "A202AC" + newItem1.getTemp() + "FF";
                    if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)){
                        jumpDist += 6;
                    }
                }

                // if inside if/while and not first pass, add to jump
                if((insideIf || insideWhile) && !insideIfFirstPass && !insideWhileFirstPass){
                    jumpDist += opCode.length()/2;
                }

                totalBytesUsed += opCode.length() / 2;

                opCodeOutput += opCode;
            }
            // check if the compared values are both variables
            else if (val1.matches("[a-z]") && val2.matches("[a-z]")) {
                opCode += "AE" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                opCode += "EC" + varTable.getItem(val2.charAt(0), getVariableScope(val2)).getTemp();

                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                varTable.addItem(newItem1);

                // we are in != op
                if (!isEqual) {
                    // store true in temp
                    opCode += "A9F58D" + newItem1.getTemp();

                    // if z flag from previous comparison was true, skip next instruction set
                    opCode += "D005";

                    // if z flag was false, set temp to false
                    opCode += "A9FA8D" + newItem1.getTemp();

                    // compare true to temp value
                    opCode += "A2F5EC" + newItem1.getTemp();

                    // check if inside if/while to update jump
                    if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                        // add jump item
                        int numJumpItems = jumpTable.getNumVariables();

                        JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                        jumpTable.addItem(tempJumpItem);

                        // jump to temp location which will later be backpatched
                        opCode += "D0" + tempJumpItem.getTemp();
                    }
                    // not inside if/while, if z flag true skip next instruction
                    else{
                        opCode += "D005";
                    }

                    // if z flag false set temp to true
                    opCode += "A9F58D" + newItem1.getTemp();

                    // if inside first pass, add 5 to jump dist
                    if((insideIf || insideWhile) && insideIfFirstPass){
                        jumpDist += 5;
                    }

                }
                // we are in == op
                else {
                    // set temp to false
                    opCode += "A9FA8D" + newItem1.getTemp();

                    // if z flag from prev comparison was true, skip setting temp to true
                    opCode += "D005";

                    // if z flag was false, set temp to true
                    opCode += "A9F58D" + newItem1.getTemp();

                    // check if inside first pass of if statement or while statement
                    if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                        // compare true to temp value
                        opCode += "A2F5EC" + newItem1.getTemp();
                        int numJumpItems = jumpTable.getNumVariables();

                        // create jump table item
                        JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                        jumpTable.addItem(tempJumpItem);

                        // jump to temp location which will later be backpatched
                        opCode += "D0" + tempJumpItem.getTemp();
                    }
                    // not inside if/while statement
                    else{
                        // compare false to temp
                        opCode += "A2FAEC" + newItem1.getTemp();
                        // if false == temp, skip next instruction
                        opCode += "D005";
                    }

                    // if false != temp, set temp to true
                    opCode += "A9FA8D" + newItem1.getTemp();

                    // add to jump if inside if/while
                    if((insideIf || insideWhile) && insideIfFirstPass){
                        jumpDist += 5;
                    }

                }

                // if inside print statement, print temp
                if (inPrint) {
                    opCode += "A202AC" + newItem1.getTemp() + "FF";
                    // if inside first pass, add to jump
                    if((insideIf || insideWhile) && insideIfFirstPass){
                        jumpDist += 6;
                    }
                }

                // add to jump if inside if/while
                if((insideIf || insideWhile) && !insideIfFirstPass){
                    jumpDist += opCode.length()/2;
                }

                totalBytesUsed += opCode.length() / 2;

                opCodeOutput += opCode;
            }
            // check if first Node is a variable and second node isn't
            else if (val1.matches("[a-z]")) {
                String type = getVariableType(val1);

                // comparing int values
                if (type.equals("int")) {

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem2);

                    // val2 is digit
                    if(val2.matches("[0-9]")) {
                        // store first integer
                        opCode += "A90" + val2 + "8D" + newItem2.getTemp();
                    }
                    // comparing addition op
                    else{
                        // call function to add the numbers
                        compareAddInts(node2.getChildren().get(0), node2.getChildren().get(1), currentScope);
                        opCode += "8D" + newItem2.getTemp();
                    }

                    opCode += "AE" + newItem2.getTemp();
                    // compare value to the variable value
                    opCode += "EC" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                    numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    // within != expression
                    if (!isEqual) {
                        // store true in temp
                        opCode += "A9F58D" + newItem1.getTemp();

                        // if z flag from previous comparison was true, skip setting temp to true
                        opCode += "D005";

                        // if z flag is false, set temp to false
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // compare true to temp value
                        opCode += "A2F5EC" + newItem1.getTemp();

                        // check if inside first pass of if/while statement
                        if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                            // add jump item to jump table
                            int numJumpItems = jumpTable.getNumVariables();
                            JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                            jumpTable.addItem(tempJumpItem);
                            // jump to temp location which will later be backpatched
                            opCode += "D0" + tempJumpItem.getTemp();
                        }
                        // not inside first pass of if/while
                        else{
                            // if true == temp val skip next instruction
                            opCode += "D005";
                        }
                        // true != temp set temp to true
                        opCode += "A9F58D" + newItem1.getTemp();

                        // add to jump if inside if/while
                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist += 5;
                        }

                    }
                    // within == expression
                    else {
                        // store false in temp
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // if z flag from previous comparison is true, skip over next instruction set
                        opCode += "D005";

                        // if z flag is false, store true in temp
                        opCode += "A9F58D" + newItem1.getTemp();

                        // check if inside first pass of if/while statement
                        if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                            // compare true to temp
                            opCode += "A2F5EC" + newItem1.getTemp();
                            // add jump table item to jump table
                            int numJumpItems = jumpTable.getNumVariables();
                            JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                            jumpTable.addItem(tempJumpItem);
                            // jump to temp location which will later be backpatched
                            opCode += "D0" + tempJumpItem.getTemp();
                        }
                        else{
                            // compare false to temp
                            opCode += "A2FAEC" + newItem1.getTemp();
                            opCode += "D005";
                        }

                        // store false in temp if false != temp
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // add to jump if inside if/while statement
                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist += 5;
                        }

                    }

                    // print temp if inside print statement
                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";

                        // add to jump
                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist +=6;
                        }
                    }

                    // add to jump
                    if((insideIf || insideWhile) && !insideIfFirstPass){
                        jumpDist += opCode.length()/2;
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                }
                // variable type is boolean
                else if (type.equals("boolean")) {
                    String end;

                    // get true/false value and location in heap
                    if (val2.equals("false")) {
                        end = "FA";
                    } else {
                        end = "F5";
                    }
                    opCode += "A2" + end;

                    // compare value to variable value
                    opCode += "EC" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    // inside != op
                    if (!isEqual) {
                        // store true in temp
                        opCode += "A9F58D" + newItem1.getTemp();

                        // if z flag from previous comparison was true, skip next instruction set
                        opCode += "D005";

                        // if z flag was false, set temp to false
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // compare true to temp value
                        opCode += "A2F5EC" + newItem1.getTemp();

                        // if inside first pass of if/while statement add jump table item
                        if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                            int numJumpItems = jumpTable.getNumVariables();
                            JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                            jumpTable.addItem(tempJumpItem);
                            // jump to temp location which will later be backpatched
                            opCode += "D0" + tempJumpItem.getTemp();
                        }
                        // not inside first pass of if/while so if true == temp skip next instruction set
                        else{
                            opCode += "D005";
                        }
                        // true != temp, set temp to false
                        opCode += "A9F58D" + newItem1.getTemp();

                        // add to jump if inside if/while
                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist +=5;
                        }

                    }
                    // inside == op
                    else {
                        // store false in temp
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // if z flag from prev variable comparison is true, skip next set of instructions
                        opCode += "D005";

                        // if z flag is false, set temp to true
                        opCode += "A9F58D" + newItem1.getTemp();

                        // if inside first pass of if/while statement, add to jump table
                        if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                            // compare true to temp value
                            opCode += "A2F5EC" + newItem1.getTemp();
                            int numJumpItems = jumpTable.getNumVariables();
                            JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                            jumpTable.addItem(tempJumpItem);
                            // jump to temp location which will later be backpatched
                            opCode += "D0" + tempJumpItem.getTemp();
                        }
                        // not inside if/while first pass, compare true to temp value
                        else{
                            opCode += "A2FAEC" + newItem1.getTemp();
                            // if true == temp, skip next instruction set
                            opCode += "D005";
                        }

                        // if z flag is false, set temp to false
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // if inside first pass, add to jump
                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist +=5;
                        }

                    }

                    // if inside print, print temp
                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";

                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist +=6;
                        }
                    }

                    // add to jump
                    if((insideIf || insideWhile) && !insideIfFirstPass){
                        jumpDist += opCode.length()/2;
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                }
                // comparing string values
                else {
                    // store first value in heap
                    storeHeap(val2);
                    String endVal2 = Integer.toHexString(heapEnd);

                    if (endVal2.length() < 2) {
                        endVal2 = "0" + endVal2;
                    }

                    // compare 2 string values
                    opCode += "AE" + endVal2.toUpperCase() + "00";

                    // set z flag based on boolean comparison
                    // compare value to the variable value
                    opCode += "EC" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    // set temp to be true
                    opCode += "A9F58D" + newItem1.getTemp();

                    // if z flag from int comparison is true, skip over next set
                    opCode += "D005";

                    // z flag was false, set temp variable to be false
                    opCode += "A9FA8D" + newItem1.getTemp();

                    // compare true to the temp value
                    if(isEqual) {
                        opCode += "A2F5EC" + newItem1.getTemp();
                    }
                    // compare false to temp value
                    else{
                        opCode += "A2FAEC" + newItem1.getTemp();
                    }

                    // check if inside if statement/while statement
                    if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                        if(isEqual) {
                            opCode += "A2FAEC" + newItem1.getTemp();
                        }
                        // compare false to temp value
                        else{
                            opCode += "A2F5EC" + newItem1.getTemp();
                        }

                        // create jump table item
                        int numJumpItems = jumpTable.getNumVariables();
                        JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                        jumpTable.addItem(tempJumpItem);

                        // jump to temp location which will later be backpatched
                        opCode += "D0" + tempJumpItem.getTemp();
                    }
                    // not inside if/while skip over storing true in temp
                    else {
                        opCode += "D005";
                    }

                    // z flag was false, set item to be false
                    opCode += "A9FA8D" + newItem1.getTemp();

                    // add to jump if inside if/while
                    if((insideIf || insideWhile) && insideIfFirstPass){
                        jumpDist += 5;
                    }

                    // if inside print statement, print the true/false value
                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";
                        if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)){
                            jumpDist += 6;
                        }
                    }

                    // if inside if/while and not first pass, add to jump
                    if((insideIf || insideWhile) && !insideIfFirstPass && !insideWhileFirstPass){
                        System.out.println(!insideWhileFirstPass);
                        jumpDist += opCode.length()/2;
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                }

            }
            // second Node is a variable and first isn't
            else if (val2.matches("[a-z]")) {
                String type = getVariableType(val2);

                // comparing int values
                if (type.equals("int")) {

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem2);

                    // val2 is digit
                    if(val1.matches("[0-9]")) {
                        // store first integer
                        opCode += "A90" + val1 + "8D" + newItem2.getTemp();
                    }
                    // comparing addition op
                    else{
                        // call function to add the numbers
                        compareAddInts(node1.getChildren().get(0), node1.getChildren().get(1), currentScope);
                        opCode += "8D" + newItem2.getTemp();
                    }

                    opCode += "AE" + newItem2.getTemp();

                    // compare value to the variable value
                    opCode += "EC" + varTable.getItem(val2.charAt(0), getVariableScope(val2)).getTemp();

                    numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    // within != expression
                    if (!isEqual) {
                        // store true in temp
                        opCode += "A9F58D" + newItem1.getTemp();

                        // if z flag from previous comparison was true, skip setting temp to true
                        opCode += "D005";

                        // if z flag is false, set temp to false
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // compare true to temp value
                        opCode += "A2F5EC" + newItem1.getTemp();

                        // check if inside first pass of if/while statement
                        if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                            // add jump item to jump table
                            int numJumpItems = jumpTable.getNumVariables();
                            JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                            jumpTable.addItem(tempJumpItem);
                            // jump to temp location which will later be backpatched
                            opCode += "D0" + tempJumpItem.getTemp();
                        }
                        // not inside first pass of if/while
                        else{
                            // if true == temp val skip next instruction
                            opCode += "D005";
                        }
                        // true != temp set temp to true
                        opCode += "A9F58D" + newItem1.getTemp();

                        // add to jump if inside if/while
                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist += 5;
                        }

                    }
                    // within == expression
                    else {
                        // store false in temp
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // if z flag from previous comparison is true, skip over next instruction set
                        opCode += "D005";

                        // if z flag is false, store true in temp
                        opCode += "A9F58D" + newItem1.getTemp();

                        // check if inside first pass of if/while statement
                        if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                            // compare true to temp
                            opCode += "A2F5EC" + newItem1.getTemp();
                            // add jump table item to jump table
                            int numJumpItems = jumpTable.getNumVariables();
                            JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                            jumpTable.addItem(tempJumpItem);
                            // jump to temp location which will later be backpatched
                            opCode += "D0" + tempJumpItem.getTemp();
                        }
                        else{
                            // compare false to temp
                            opCode += "A2FAEC" + newItem1.getTemp();
                            opCode += "D005";
                        }

                        // store false in temp if false != temp
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // add to jump if inside if/while statement
                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist += 5;
                        }

                    }

                    // print temp if inside print statement
                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";

                        // add to jump
                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist +=6;
                        }
                    }

                    // add to jump
                    if((insideIf || insideWhile) && !insideIfFirstPass){
                        jumpDist += opCode.length()/2;
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                }
                // comparing boolean values
                else if (type.equals("boolean")) {
                    String end;
                    // get value of val1 and get heap location
                    if (val1.equals("false")) {
                        end = "FA";
                    } else {
                        end = "F5";
                    }
                    opCode += "A2" + end;

                    // compare value to variable value
                    opCode += "EC" + varTable.getItem(val2.charAt(0), getVariableScope(val2)).getTemp();

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    // within != expression
                    if (!isEqual) {
                        // store true in temp
                        opCode += "A9F58D" + newItem1.getTemp();

                        // if z flag from previous comparison was true, skip next set of instructions
                        opCode += "D005";

                        // if z flag was false, set temp to false
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // compare true to temp
                        opCode += "A2F5EC" + newItem1.getTemp();

                        // check if inside first pass of if/while statement
                        if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                            // add jump table item
                            int numJumpItems = jumpTable.getNumVariables();
                            JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                            jumpTable.addItem(tempJumpItem);
                            // jump to temp location which will later be backpatched
                            opCode += "D0" + tempJumpItem.getTemp();
                        }
                        // if z flag was true, skip over next set of instructions
                        else{
                            opCode += "D005";
                        }

                        // if z flag was false, set temp to true
                        opCode += "A9F58D" + newItem1.getTemp();

                        // add to jump
                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist += 5;
                        }

                    }
                    // within == expression
                    else {
                        // store false in temp
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // if z flag from previous comparison was true, skip next set of instructions
                        opCode += "D005";

                        // if z flag was false, set temp to true
                        opCode += "A9F58D" + newItem1.getTemp();

                        // check if inside first pass of if/while statement
                        if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                            // compare true to temp
                            opCode += "A2F5EC" + newItem1.getTemp();
                            // add jump table itme
                            int numJumpItems = jumpTable.getNumVariables();
                            JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                            jumpTable.addItem(tempJumpItem);
                            // jump to temp location which will later be backpatched
                            opCode += "D0" + tempJumpItem.getTemp();
                        }
                        else{
                            // compare false to temp
                            opCode += "A2FAEC" + newItem1.getTemp();
                            // if false == temp, skip next set of instruction
                            opCode += "D005";
                        }

                        // z flag false, set temp to false
                        opCode += "A9FA8D" + newItem1.getTemp();

                        // add to jump
                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist += 5;
                        }
                    }

                    // if inside print, print temp value
                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";

                        // add to jump
                        if((insideIf || insideWhile) && insideIfFirstPass){
                            jumpDist +=6;
                        }
                    }
                    // add to jump
                    if((insideIf || insideWhile) && !insideIfFirstPass){
                        jumpDist += opCode.length()/2;
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                }
                // comparing string value to variable
                else {
                    // store first value in heap
                    storeHeap(val1);
                    String endVal1 = Integer.toHexString(heapEnd);

                    if (endVal1.length() < 2) {
                        endVal1 = "0" + endVal1;
                    }

                    // compare 2 string values
                    opCode += "AE" + endVal1.toUpperCase() + "00";

                    // set z flag based on boolean comparison
                    // compare value to the variable value
                    opCode += "EC" + varTable.getItem(val2.charAt(0), getVariableScope(val2)).getTemp();

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    // set temp to be true
                    opCode += "A9F58D" + newItem1.getTemp();

                    // if z flag from int comparison is true, skip over next set
                    opCode += "D005";

                    // z flag was false, set temp variable to be false
                    opCode += "A9FA8D" + newItem1.getTemp();

                    // compare true to the temp value
                    if(isEqual) {
                        opCode += "A2F5EC" + newItem1.getTemp();
                    }
                    // compare false to temp value
                    else{
                        opCode += "A2FAEC" + newItem1.getTemp();
                    }

                    // check if inside if statement/while statement
                    if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                        if(isEqual) {
                            opCode += "A2FAEC" + newItem1.getTemp();
                        }
                        // compare false to temp value
                        else{
                            opCode += "A2F5EC" + newItem1.getTemp();
                        }
                        // create jump table item
                        int numJumpItems = jumpTable.getNumVariables();
                        JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                        jumpTable.addItem(tempJumpItem);

                        // jump to temp location which will later be backpatched
                        opCode += "D0" + tempJumpItem.getTemp();
                    }
                    // not inside if/while skip over storing true in temp
                    else {
                        opCode += "D005";
                    }

                    // z flag was false, set item to be false
                    opCode += "A9FA8D" + newItem1.getTemp();

                    // add to jump if inside if/while
                    if((insideIf || insideWhile) && insideIfFirstPass){
                        jumpDist += 5;
                    }

                    // if inside print statement, print the true/false value
                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";
                        if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)){
                            jumpDist += 6;
                        }
                    }

                    // if inside if/while and not first pass, add to jump
                    if((insideIf || insideWhile) && !insideIfFirstPass && !insideWhileFirstPass){
                        System.out.println(!insideWhileFirstPass);
                        jumpDist += opCode.length()/2;
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                }

            }
            // no variables, comparing 2 string values
            else {
                // store first value in heap
                storeHeap(val1);
                String endVal1 = Integer.toHexString(heapEnd);

                if (endVal1.length() < 2) {
                    endVal1 = "0" + endVal1;
                }

                // store second value in heap
                storeHeap(val2);
                String endVal2 = Integer.toHexString(heapEnd);

                if (endVal2.length() < 2) {
                    endVal2 = "0" + endVal2;
                }

                // compare 2 string values
                opCode += "AE" + endVal1.toUpperCase() + "00";

                // set z flag based on string comparison
                opCode += "EC" + endVal2.toUpperCase() + "00";

                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                varTable.addItem(newItem1);

                // set temp to be true
                opCode += "A9F58D" + newItem1.getTemp();

                // if z flag from int comparison is true, skip over next set
                opCode += "D005";

                // z flag was false, set temp variable to be false
                opCode += "A9FA8D" + newItem1.getTemp();

                // check if inside if statement/while statement
                if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)) {
                    if(isEqual) {
                        opCode += "A2F5EC" + newItem1.getTemp();
                    }
                    // compare false to temp value
                    else{
                        opCode += "A2FAEC" + newItem1.getTemp();
                    }
                    // create jump table item
                    int numJumpItems = jumpTable.getNumVariables();
                    JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                    jumpTable.addItem(tempJumpItem);

                    // jump to temp location which will later be backpatched
                    opCode += "D0" + tempJumpItem.getTemp();
                }
                else{
                    // compare true to the temp value
                    if(isEqual) {
                        opCode += "A2FAEC" + newItem1.getTemp();
                    }
                    // compare false to temp value
                    else{
                        opCode += "A2F5EC" + newItem1.getTemp();

                        opCode += "A9F58D" + newItem1.getTemp();
                    }
                    // not inside if/while skip over storing true in temp
                    opCode += "D005";
                }


                // z flag was false, set item to be false
                opCode += "A9FA8D" + newItem1.getTemp();

                // add to jump if inside if/while
                if((insideIf || insideWhile) && insideIfFirstPass){
                    jumpDist += 5;
                }

                // if inside print statement, print the true/false value
                if (inPrint) {
                    opCode += "A202AC" + newItem1.getTemp() + "FF";
                    if((insideIf || insideWhile) && (insideIfFirstPass || insideWhileFirstPass)){
                        jumpDist += 6;
                    }
                }

                // if inside if/while and not first pass, add to jump
                if((insideIf || insideWhile) && !insideIfFirstPass && !insideWhileFirstPass){
                    jumpDist += opCode.length()/2;
                }

                totalBytesUsed += opCode.length() / 2;

                opCodeOutput += opCode;
            }
        }
        // throw error for nested boolean expression
        else{
            errorCount++;
            System.out.println("CODE GENERATION: ERROR: Nested Boolean Expressions are not supported.");
        }

        // set boolean to false since no longer in first pass
        if(insideIfFirstPass){
            insideIfFirstPass = false;
        }

        // set boolean to false since no longer in first pass
        if(insideWhileFirstPass){
            insideWhileFirstPass = false;
        }
    }

    /**
     * If statement or while statement branch on true/false
     * @param node1
     */
    public void BoolOpWithoutExpr(Node node1){
        // check if inside first pass while and set start
        if(insideWhileFirstPass) {
            startWhile = opCodeOutput.length() / 2;
        }

        // get value (true/false)
        String val1 = node1.getName();
        String opCode = "";

        String endVal1;
        // check val1 true/false and set heap location
        if (val1.equals("false")) {
            endVal1 = "FA";
        } else {
            endVal1 = "F5";
        }

        // load x with heap val
        opCode += "AE" + endVal1 + "00";

        // compare val1 to true
        opCode += "EC" + "F5" + "00";

        int numVars = varTable.getNumVariables();

        StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
        varTable.addItem(newItem1);

        // set temp to be true
        opCode += "A9FA8D" + newItem1.getTemp();

        // if z flag from comparison is true skip next set of instructions
        opCode += "D005";

        // if z flag was false, set temp to false
        opCode += "A9F58D" + newItem1.getTemp();

        // compare true to temp value
        opCode += "A2F5EC" + newItem1.getTemp();

        int numJumpItems = jumpTable.getNumVariables();

        // create jump item
        JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
        jumpTable.addItem(tempJumpItem);

        // jump to temp location which will later be backpatched
        opCode += "D0" + tempJumpItem.getTemp();

        // if z flag is false, set temp to be true
        opCode += "A9FA8D" + newItem1.getTemp();

        // add to jump
        if(insideIfFirstPass) {
            jumpDist += 5;
        }

        totalBytesUsed += opCode.length() / 2;

        opCodeOutput += opCode;
        insideIfFirstPass = false;
        insideWhileFirstPass = false;

        System.out.println("CODE GENERATION: Checking value: " + val1 + " in if/while statement.");
    }

    /**
     * format op code to be two digit bytes
     * @return formatted op code
     */
    public String outputToString(){
        // set blocks to be of two and add one space between sets
        String val = "2";
        String result = opCodeOutput.replaceAll("(.{" + val + "})", "$1 ").trim();

        result = result.replaceAll("(.{48})", "$1\n");
        return result;
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
