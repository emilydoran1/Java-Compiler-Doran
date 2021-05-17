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
    private boolean insideWhile = false;

    private int jumpDist = 0;

    public CodeGen(SyntaxTree ast, SymbolTable symbolTable, int programNum, boolean verboseMode, boolean passedLex, boolean passedParse,
                   boolean passedSemanticAnalysis){
        this.ast = ast;
        this.symbolTable = symbolTable;
        this.programNum = programNum;
        this.verboseMode = verboseMode;

        // make sure Lex, Parse, and Semantic Analysis didn't throw any errors before we begin Code Generation
        if(passedLex && passedParse && passedSemanticAnalysis){
            System.out.println("CODE GENERATION: Beginning Code Generation on Program " + programNum + " ...");
            Node root = ast.getRoot();

            ArrayList<Node> children = root.getChildren();

            // add the true and false values to the heap
            storeHeap("false");

            storeHeap("true");

            // begin code gen on root node children
            beginCodeGen(children);
            opCodeOutput += "00";
            totalBytesUsed += 1;

            int difference = (256 - totalBytesUsed) - (256-heapEnd);

            String totalBytesUsedHex = Integer.toHexString(totalBytesUsed);

            if(difference < 0){
                errorCount++;
                System.out.println("CODE GENERATION: ERROR: Exceeded Stack Memory Limit. ");
            }
            else if (errorCount == 0){
                // add in zeros for empty bytes
                for (int i = 0; i < difference; i++) {
                    opCodeOutput += "00";
                }

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
                    String hexTemp = Integer.toHexString(varTable.getVariableTable().get(i).getAddress());
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
                    String tempJump = jumpTable.getJumpTable().get(i).getTemp();
                    String hexTemp = Integer.toHexString(jumpTable.getJumpTable().get(i).getDistance());
                    if (hexTemp.length() < 2) {
                        hexTemp = "0" + hexTemp;
                    }
                    opCodeOutput = opCodeOutput.replace(tempJump, hexTemp);
                    if(verboseMode){
                        System.out.println("CODE GENERATION: Backpatching Jump Variable Placeholder " + tempJump +
                                " Forward " + hexTemp + " Addresses");
                    }
                }
                System.out.println(outputToString());
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

    public void beginCodeGen(ArrayList<Node> children){

        for(Node child: children){
            // check if it is a branch node
            if(child.getChildren().size() > 0){
                // get child's children
                ArrayList<Node> childChildren = child.getChildren();
                if(child.getName().equals("If")){
                    insideIf = true;
                    beginCodeGen(childChildren);
                    int numJumpItems = jumpTable.getNumVariables();
                    jumpTable.getItem("J" + numJumpItems).setDistance(jumpDist);
                    jumpDist = 0;
                    insideIf = false;
                    System.out.println(insideIf);
                }
                else if(child.getName().equals("Print")){
                    if(child.getChildren().get(0).getName().equals("true") || child.getChildren().get(0).getName().equals("false")){
                        initializePrintBoolean(child.getChildren().get(0).getName());
                    }
                    else if(child.getChildren().get(0).getName().charAt(0) == '"'){
                        initializePrintString(child.getChildren().get(0).getName());
                    }
                    else if(child.getChildren().get(0).getName().equals("Addition")){
                        printAddInts(child.getChildren().get(0).getChildren().get(0), child.getChildren().get(0).getChildren().get(1), currentScope);
                        String opCode = "A201FF";
                        totalBytesUsed += opCode.length()/2;
                        opCodeOutput += opCode;

                    }
                    else if(child.getChildren().get(0).getName().equals("isNotEqual")){
                        compareValues(child.getChildren().get(0).getChildren().get(0), child.getChildren().get(0).getChildren().get(1),
                                true, false);
                    }
                    else if(child.getChildren().get(0).getName().equals("isEqual")){
                        compareValues(child.getChildren().get(0).getChildren().get(0), child.getChildren().get(0).getChildren().get(1),
                                true, true);
                    }
                    else {
                        initializePrint(child.getChildren().get(0).getName().charAt(0), currentScope);
                    }
                }
                else if(child.getName().equals("Addition")){
                    //if(!child.getChildren().get(0).getChildren().get(1).getName().equals("Addition")) {
                        storeAddInts(child.getParent().getChildren().get(0).getName().charAt(0), child.getChildren().get(0), child.getChildren().get(1), currentScope);
                   // }

                }
                else if(child.getName().equals("isEqual") && insideIf){
                    compareValues(child.getChildren().get(0), child.getChildren().get(1), false, true);
                }
                else if(child.getName().equals("isNotEqual") && insideIf){
                    compareValues(child.getChildren().get(0), child.getChildren().get(1), false, false);
                }

                else{
                    if(child.getName().equals("BLOCK")){
                        scopeCount++;
                        currentScope = scopeCount-1;
                        if(childChildren != null) {
                            beginCodeGen(childChildren);
                        }
                        if(currentScope != 0) {
                            currentScope = symbolTable.get(currentScope).getParent().getScopeNum();
                        }
                    }
                    else{
                        beginCodeGen(childChildren);
                    }

                }
            }
            // leaf node
            else{
                // variable declaration
                if(child.getName().equals("int") || child.getName().equals("boolean") || child.getName().equals("string")){
                    initializeVariable(child.getParent().getChildren().get(1).getName().charAt(0), currentScope);
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
            }
        }

    }

    public void initializeVariable(char variableName, int scope){
        int numVars = varTable.getNumVariables();

        StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", variableName, scope);
        varTable.addItem(newItem);

        String opCode = "A9008D" + newItem.getTemp();

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;


    }

    public void assignStmtInt(char variableName, String value, int scope){
        String opCode = "A90" + value + "8D" + varTable.getItem(variableName, scope).getTemp();

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

    }

    public void assignStmtString(char variableName, Node node, int scope){
        String value = node.getName();

        String opCode = "";

        // if you are assigning it to the value of a variable
        if(value.matches("[a-z]") && variableName != value.charAt(0)){
            opCode += "AD" + varTable.getItem(value.charAt(0), scope).getTemp() + "8D" +
                    varTable.getItem(variableName, scope).getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;
        }
        else if(variableName != value.charAt(0)){
            String end = "";
            if(value.equals("true") || value.equals("false")){
                if(value.equals("false")){
                    end = "FA";
                }
                else{
                    end = "F5";
                }
                opCode += "A9" + end + "8D" + varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp();
            }
            else if(value.equals("isEqual") || value.equals("isNotEqual")){
                if(!node.getChildren().get(0).equals("isEqual") && !node.getChildren().get(0).equals("isNotEqual") &&
                        !node.getChildren().get(1).equals("isEqual") && !node.getChildren().get(0).equals("isNotEqual")) {
                    if (value.equals("isEqual")) {
                        compareValues(node.getChildren().get(0), node.getChildren().get(1), false, true);
                        end = varTable.getItem(Character.forDigit(tempCount - 1, 10), scope).getTemp();
                    } else {
                        compareValues(node.getChildren().get(0), node.getChildren().get(1), false, false);
                        end = varTable.getItem(Character.forDigit(tempCount - 1, 10), scope).getTemp();
                    }
                    opCode += "AD" + end + "8D" + varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp();
                }
                else{
                    errorCount++;
                    System.out.println("CODE GENERATION: ERROR: Nested Boolean Expressions are not supported.");
                }
            }
            else {
                storeHeap(value);

                end = Integer.toHexString(heapEnd);
                if (end.length() < 2) {
                    end = "0" + end;
                }
                opCode += "A9" + end + "8D" + varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp();
            }

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;
        }

    }

    public void printAddInts(Node node1, Node node2, int scope){
        int numVars = varTable.getNumVariables();

        StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
        varTable.addItem(newItem);

        numVars = varTable.getNumVariables();

        String value1 = node1.getName();
        String value2 = node2.getName();

        String opCode = "";

        if(!value2.matches("[0-9]") && !value2.equals("Addition")){
            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            numVars = varTable.getNumVariables();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
            varTable.addItem(newItem2);

            opCode += "6D" + varTable.getItem(value2.charAt(0), scope).getTemp();

            opCode += "8D" + newItem2.getTemp() + "AD" + newItem2.getTemp();

            opCode += "A201AC" + newItem2.getTemp();


            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;
        }
        else if(value2.equals("Addition")){

            printAddInts(node2.getChildren().get(0), node2.getChildren().get(1), scope);

            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            opCode += "A9006D" + varTable.getItem(Character.forDigit(tempCount-1,10), scope).getTemp();

//            opCode += "A9006D" + varTable.getItem(var, scope).getTemp();

            opCode += "6D" + newItem.getTemp();

            opCode += "8D" + varTable.getItem(Character.forDigit(tempCount-1,10), scope).getTemp() + "AD" + varTable.getItem(Character.forDigit(tempCount-1,10), scope).getTemp();

            opCode += "8D" + newItem.getTemp();

            opCode += "A201AC" + newItem.getTemp();

//            opCode += "A201FF";

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

        }
        else{
            opCode += "A90" +value1 + "8D" + newItem.getTemp();
            opCode += "A90" + value2 + "6D" + newItem.getTemp();

            numVars = varTable.getNumVariables();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
            varTable.addItem(newItem2);

            opCode += "8D" + newItem2.getTemp();

            opCode += "A201AC" + newItem2.getTemp();

//            opCode += "A201FF";

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;
        }

    }

    public void storeAddInts(char var, Node node1, Node node2, int scope){
        int numVars = varTable.getNumVariables();

        StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
        varTable.addItem(newItem);

        numVars = varTable.getNumVariables();

        String value1 = node1.getName();
        String value2 = node2.getName();

        String opCode = "";

        if(!value2.matches("[0-9]") && !value2.equals("Addition")){
            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
            varTable.addItem(newItem2);

            opCode += "A9006D" + varTable.getItem(var, scope).getTemp();

            opCode += "6D" + newItem.getTemp();

            opCode += "8D" + newItem2.getTemp() + "AD" + newItem2.getTemp();

            opCode += "8D" + varTable.getItem(var, scope).getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;
        }
        else if(value2.equals("Addition")){
            storeAddInts(var, node2.getChildren().get(0), node2.getChildren().get(1), scope);
            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            opCode += "A9006D" + varTable.getItem(Character.forDigit(tempCount-1,10), scope).getTemp();

            opCode += "A9006D" + varTable.getItem(var, scope).getTemp();

            opCode += "6D" + newItem.getTemp();

            opCode += "8D" + varTable.getItem(Character.forDigit(tempCount-1,10), scope).getTemp() + "AD" +
                    varTable.getItem(Character.forDigit(tempCount-1,10), scope).getTemp();

            opCode += "8D" + varTable.getItem(var, scope).getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

        }
        else{
            opCode += "A90" +value1 + "8D" + newItem.getTemp();
            opCode += "A90" + value2 + "6D" + newItem.getTemp();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++,10), -1);
            varTable.addItem(newItem2);

            opCode += "8D" + newItem2.getTemp();

            opCode += "A201AC" + newItem2.getTemp();

            opCode += "AC" +  newItem2.getTemp() + "8D" +  varTable.getItem(var, scope).getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;
        }



    }

    public void storeHeap(String value){
        // if we have a string, ignore the quotes
        if(value.charAt(0) == '\"'){
            value = value.substring(1, value.length()-1);
        }

        String appendHeapOut = "";
        for(int i=0; i < value.length(); i++){
            String tempHeapOut = Integer.toHexString((int) value.charAt(i));
            if(tempHeapOut.length() < 2){
                tempHeapOut = "0" + tempHeapOut;
            }
            appendHeapOut += tempHeapOut;
            heapEnd--;
        }
        appendHeapOut += "00";
        heapEnd--;

        heapOutput = appendHeapOut + heapOutput;
    }


    public void initializePrint(char variableName, int scope){
        String opCode = "";

        if(Character.toString(variableName).matches("[a-z]")){
            if(getVariableType(Character.toString(variableName)).equals("int")) {
                opCode += "AC" + varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp() + "A201FF";
            }
            else if (getVariableType(Character.toString(variableName)).equals("string")
                || getVariableType(Character.toString(variableName)).equals("boolean")) {
                opCode += "AC" + varTable.getItem(variableName, getVariableScope(Character.toString(variableName))).getTemp() + "A202FF";
            }
        }
        else if(Character.toString(variableName).matches("[0-9]")){
            opCode += "A00" + Character.toString(variableName) + "A201FF";
            if(insideIf){
                jumpDist += 5;
            }
        }

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

    }

    public void initializePrintBoolean(String val){
        String opCode = "";

        // boolean values are pre-stored, so we don't need to re store them
        String end;
        if(val.equals("false")){
            end = "FA";
        }
        else{
            end = "F5";
        }

        opCode += "A0" + end + "A202FF";

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

    }

    public void initializePrintString(String val){
        String opCode = "";

        storeHeap(val);

        String end = Integer.toHexString(heapEnd);
        if(end.length() < 2){
            end = "0" + end;
        }

        opCode += "A0" + end + "A202FF";

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

    }

    public void compareValues(Node node1, Node node2, boolean inPrint, boolean isEqual){
        String opCode = "";

        String val1 = node1.getName();
        String val2 = node2.getName();

        // check if values are ints
        if(!val1.equals("isEqual") && !val1.equals("isNotEqual") && !val2.equals("isEqual") && !val2.equals("isNotEqual")) {
            if (val1.matches("[0-9]") && val2.matches("[0-9]")) {
                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                varTable.addItem(newItem);

                opCode += "A90" + val1 + "8D" + newItem.getTemp();

                numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                varTable.addItem(newItem2);

                opCode += "A90" + val2 + "8D" + newItem2.getTemp();

                opCode += "AE" + newItem.getTemp();

                opCode += "EC" + newItem2.getTemp();

                numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem3 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                varTable.addItem(newItem3);

                if (!isEqual) {
                    opCode += "A9F58D" + newItem3.getTemp();

                    opCode += "D005";

                    opCode += "A9FA8D" + newItem3.getTemp();

                    if(insideIf) {
                        opCode += "A2F5EC" + newItem3.getTemp();
                        // TODO replace with jump table
                        int numJumpItems = jumpTable.getNumVariables();
                        JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                        jumpTable.addItem(tempJumpItem);
                        opCode += "D0" + tempJumpItem.getTemp();
                    }
                    else{
                        opCode += "A2F5EC" + newItem3.getTemp();
                        opCode += "D005";
                    }

                    opCode += "A9F58D" + newItem3.getTemp();

                    if(insideIf){
                        jumpDist += 5;
                    }

                } else {
                    opCode += "A9FA8D" + newItem3.getTemp();

                    opCode += "D005";

                    opCode += "A9F58D" + newItem3.getTemp();

                    if(insideIf) {
                        opCode += "A2F5EC" + newItem3.getTemp();
                        // TODO replace with jump table
                        int numJumpItems = jumpTable.getNumVariables();
                        JumpTableItem tempJumpItem = new JumpTableItem("J" + numJumpItems);
                        jumpTable.addItem(tempJumpItem);
                        opCode += "D0" + tempJumpItem.getTemp();
                    }
                    else{
                        opCode += "A2FAEC" + newItem3.getTemp();
                        opCode += "D005";
                    }

                    opCode += "A9FA8D" + newItem3.getTemp();
                    if(insideIf){
                        jumpDist += 5;
                    }

                }

                if (inPrint) {
                    opCode += "A202AC" + newItem3.getTemp() + "FF";
                    if(insideIf){
                        jumpDist += 6;
                    }
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

                opCode += "EC" + endVal2 + "00";

                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                varTable.addItem(newItem1);

                // check if we are in an isNotEqual op
                if (!isEqual) {
                    opCode += "A9F58D" + newItem1.getTemp();

                    opCode += "D005";

                    opCode += "A9FA8D" + newItem1.getTemp();

                    opCode += "A2F5EC" + newItem1.getTemp();

                    opCode += "D005";

                    opCode += "A9F58D" + newItem1.getTemp();

                }
                // in an isEqual op
                else {
                    if(insideIf){
                        opCode += "A9018D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9008D" + newItem1.getTemp();

                        opCode += "A201EC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9018D" + newItem1.getTemp();
                    }
                    else {
                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "A2FAEC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();
                    }

                }

                if (inPrint) {
                    opCode += "A202AC" + newItem1.getTemp() + "FF";
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

                if (!isEqual) {
                    opCode += "A9F58D" + newItem1.getTemp();

                    opCode += "D005";

                    opCode += "A9FA8D" + newItem1.getTemp();

                    opCode += "A2F5EC" + newItem1.getTemp();

                    opCode += "D005";

                    opCode += "A9F58D" + newItem1.getTemp();

                } else {
                    opCode += "A9FA8D" + newItem1.getTemp();

                    opCode += "D005";

                    opCode += "A9F58D" + newItem1.getTemp();

                    opCode += "A2FAEC" + newItem1.getTemp();

                    opCode += "D005";

                    opCode += "A9FA8D" + newItem1.getTemp();

                }

                if (inPrint) {
                    opCode += "A202AC" + newItem1.getTemp() + "FF";
                }

                totalBytesUsed += opCode.length() / 2;

                opCodeOutput += opCode;
            } else if (val1.matches("[a-z]")) {
                String type = getVariableType(val1);

                if (type.equals("int")) {

                    opCode += "A20" + val2;

                    opCode += "EC" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    if (!isEqual) {
                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "A2F5EC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                    } else {
                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "A2FAEC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                    }

                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                } else if (type.equals("boolean")) {
                    String end;
                    if (val2.equals("false")) {
                        end = "FA";
                    } else {
                        end = "F5";
                    }
                    opCode += "A2" + end;

                    opCode += "EC" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    if (!isEqual) {
                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "A2F5EC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                    } else {
                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "A2FAEC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                    }

                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                } else {
                    storeHeap(val2);

                    String end = Integer.toHexString(heapEnd);
                    if (end.length() < 2) {
                        end = "0" + end;
                    }

                    opCode += "AE" + end;

                    opCode += "EC" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    if (!isEqual) {
                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "A2F5EC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                    } else {
                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "A2FAEC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                    }

                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                }

            } else if (val2.matches("[a-z]")) {
                String type = getVariableType(val2);

                // comparing int values
                if (type.equals("int")) {

                    opCode += "A20" + val1;

                    opCode += "EC" + varTable.getItem(val2.charAt(0), getVariableScope(val2)).getTemp();

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    if (!isEqual) {
                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "A2F5EC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                    } else {
                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "A2FAEC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                    }

                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                }
                // comparing boolean values
                else if (type.equals("boolean")) {
                    String end;
                    if (val1.equals("false")) {
                        end = "FA";
                    } else {
                        end = "F5";
                    }
                    opCode += "A2" + end;

                    opCode += "EC" + varTable.getItem(val2.charAt(0), getVariableScope(val2)).getTemp();

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    if (!isEqual) {
                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "A2F5EC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                    } else {
                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "A2FAEC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                    }

                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                }
                // comparing string values
                else {
                    storeHeap(val2);

                    String end = Integer.toHexString(heapEnd);
                    if (end.length() < 2) {
                        end = "0" + end;
                    }

                    opCode += "AE" + end;

                    opCode += "EC" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                    int numVars = varTable.getNumVariables();

                    StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", Character.forDigit(tempCount++, 10), -1);
                    varTable.addItem(newItem1);

                    if (!isEqual) {
                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "A2F5EC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                    } else {
                        opCode += "A9FA8D" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9F58D" + newItem1.getTemp();

                        opCode += "A2FAEC" + newItem1.getTemp();

                        opCode += "D005";

                        opCode += "A9FA8D" + newItem1.getTemp();

                    }

                    if (inPrint) {
                        opCode += "A202AC" + newItem1.getTemp() + "FF";
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                }

            }
            // no variables, comparing 2 string / boolean values
            else {
                storeHeap(val1);
                String end = Integer.toHexString(heapEnd);

                if (end.length() < 2) {
                    end = "0" + end;
                    if (inPrint) {
                        opCode += "A0" + end + "A202FF";
                    }

                    totalBytesUsed += opCode.length() / 2;

                    opCodeOutput += opCode;
                }
            }
        }
        else{
            errorCount++;
            System.out.println("CODE GENERATION: ERROR: Nested Boolean Expressions are not supported.");
        }
    }

    public String outputToString(){
        String val = "2";
        String result = opCodeOutput.replaceAll("(.{" + val + "})", "$1 ").trim();
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
