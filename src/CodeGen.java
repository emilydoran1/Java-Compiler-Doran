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
    private int totalBytesUsed = 0;

    private String opCodeOutput = "";
    private String heapOutput = "";
    private int currentScope = 0;
    private int heapEnd = 256;
    private int tempCount = 0;
    private int scopeCount = 1;

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

            beginCodeGen(children);
            opCodeOutput += "00";
            totalBytesUsed += 1;

            int difference = 256 - totalBytesUsed - (256-heapEnd);

            String totalBytesUsedHex = Integer.toHexString(totalBytesUsed);

            for(int i = 0; i < difference; i++){
                opCodeOutput += "00";
            }

            opCodeOutput += heapOutput;

            if(totalBytesUsedHex.length() < 2){
                totalBytesUsedHex = "0" + totalBytesUsedHex;
            }

            for(int i = 0; i < varTable.getNumVariables(); i++){
                varTable.getVariableTable().get(i).setAddress(totalBytesUsed + i);
            }

            for(int i = 0; i < varTable.getNumVariables(); i++){
                String temp = varTable.getVariableTable().get(i).getTemp();
                String hexTemp = Integer.toHexString(varTable.getVariableTable().get(i).getAddress());
                if(hexTemp.length() < 2){
                    hexTemp = "0" + hexTemp;
                }
                opCodeOutput = opCodeOutput.replace(temp, hexTemp + "00");
            }
            System.out.println(outputToString());

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
                if(child.getName().equals("Print")){
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
                        compareValuesNotEqual(child.getChildren().get(0).getChildren().get(0), child.getChildren().get(0).getChildren().get(1), true);
                    }
                    else if(child.getChildren().get(0).getName().equals("isEqual")){
//                        compareValuesEqual(child.getChildren().get(0).getChildren().get(0), child.getChildren().get(0).getChildren().get(1));
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
                else if(child.getName().equals("isNotEqual")){
                    compareValuesNotEqual(child.getChildren().get(0), child.getChildren().get(0), false);

                }
                else if(child.getName().equals("isEqual")){
//                    compareValuesEqual(child.getChildren().get(0), child.getChildren().get(0));

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
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1).getName(), currentScope);
                }
                // assigning var to true
                else if(child.getParent().getChildren().size() > 1 && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().equals("true")){
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1).getName(), currentScope);
                }
                // assigning var to false
                else if(child.getParent().getChildren().size() > 1 && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().equals("false")){
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1).getName(), currentScope);
                }
                // assigning var to var
                else if(child.getParent().getName().equals("Assign") && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().matches("[a-z]")){
//                    System.out.println(child.getName());
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1).getName(), currentScope);
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

    public void assignStmtString(char variableName, String value, int scope){
        // if you are assigning it to the value of a variable
        if(value.matches("[a-z]") && variableName != value.charAt(0)){
            String opCode = "AD" + varTable.getItem(value.charAt(0), scope).getTemp() + "8D" +
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
            }
            else {
                storeHeap(value);

                end = Integer.toHexString(heapEnd);
                if (end.length() < 2) {
                    end = "0" + end;
                }
            }

            String opCode = "A9" + end + "8D" + varTable.getItem(variableName, scope).getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;
        }

    }

    public void printAddInts(Node node1, Node node2, int scope){
        int numVars = varTable.getNumVariables();

        StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, scope);
        varTable.addItem(newItem);

        numVars = varTable.getNumVariables();

        String value1 = node1.getName();
        String value2 = node2.getName();

        String opCode = "";

        if(!value2.matches("[0-9]") && !value2.equals("Addition")){
            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            numVars = varTable.getNumVariables();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, scope);
            varTable.addItem(newItem2);

            opCode += "6D" + varTable.getItem(value2.charAt(0), scope).getTemp();

            opCode += "8D" + newItem2.getTemp() + "AD" + newItem2.getTemp();

            opCode += "A201AC" + newItem2.getTemp();

//            opCode += "A201FF";

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;
        }
        else if(value2.equals("Addition")){

            printAddInts(node2.getChildren().get(0), node2.getChildren().get(1), scope);

            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            opCode += "A9006D" + varTable.getItem((char)(tempCount-1), scope).getTemp();

//            opCode += "A9006D" + varTable.getItem(var, scope).getTemp();

            opCode += "6D" + newItem.getTemp();

            opCode += "8D" + varTable.getItem((char)(tempCount-1), scope).getTemp() + "AD" + varTable.getItem((char)(tempCount-1), scope).getTemp();

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

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, scope);
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

        StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, scope);
        varTable.addItem(newItem);

        numVars = varTable.getNumVariables();

        String value1 = node1.getName();
        String value2 = node2.getName();

        String opCode = "";

        if(!value2.matches("[0-9]") && !value2.equals("Addition")){
            opCode += "A90" +value1 + "8D" + newItem.getTemp();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, scope);
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

            opCode += "A9006D" + varTable.getItem((char)(tempCount-1), scope).getTemp();

            opCode += "A9006D" + varTable.getItem(var, scope).getTemp();

            opCode += "6D" + newItem.getTemp();

            opCode += "8D" + varTable.getItem((char)(tempCount-1), scope).getTemp() + "AD" + varTable.getItem((char)(tempCount-1), scope).getTemp();

            opCode += "8D" + varTable.getItem(var, scope).getTemp();

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;

        }
        else{
            opCode += "A90" +value1 + "8D" + newItem.getTemp();
            opCode += "A90" + value2 + "6D" + newItem.getTemp();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, scope);
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

    public void compareValuesNotEqual(Node node1, Node node2, boolean inPrint){
        String opCode = "";

        String val1 = node1.getName();
        String val2 = node2.getName();
        // check if values are ints
        if(val1.matches("[0-9]") && val2.matches("[0-9]")) {
            int numVars = varTable.getNumVariables();

            StaticVariableTableItem newItem = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, currentScope);
            varTable.addItem(newItem);

            opCode += "A90" + val1 + "8D" + newItem.getTemp();

            numVars = varTable.getNumVariables();

            StaticVariableTableItem newItem2 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, currentScope);
            varTable.addItem(newItem2);

            opCode += "A90" + val2 + "8D" + newItem2.getTemp();

            opCode += "AE" + newItem.getTemp();

            opCode += "EC" + newItem2.getTemp();

            numVars = varTable.getNumVariables();

            StaticVariableTableItem newItem3 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, currentScope);
            varTable.addItem(newItem3);

            opCode += "A9018D" + newItem3.getTemp();

            opCode += "D005";

            opCode += "A9008D" + newItem3.getTemp();

            opCode += "A201EC" + newItem3.getTemp();

            opCode += "D005";

            opCode += "A9018D" + newItem3.getTemp();

            if (inPrint){
                opCode += "A201AC" + newItem3.getTemp() + "FF";
            }

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;
        }
        else if(val1.matches("[a-z]") && val2.matches("[a-z]")){
            opCode += "AE" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

            opCode += "EC" + varTable.getItem(val2.charAt(0), getVariableScope(val2)).getTemp();

            int numVars = varTable.getNumVariables();

            StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, currentScope);
            varTable.addItem(newItem1);

            opCode += "A9018D" + newItem1.getTemp();

            opCode += "D005";

            opCode += "A9008D" + newItem1.getTemp();

            opCode += "A201EC" + newItem1.getTemp();

            opCode += "D005";

            opCode += "A9018D" + newItem1.getTemp();

            if(inPrint) {
                opCode += "A201AC" + newItem1.getTemp() + "FF";
            }

            totalBytesUsed += opCode.length()/2;

            opCodeOutput += opCode;
        }
        else if(val1.matches("[a-z]")){
            String type = getVariableType(val1);

            if(type.equals("int")){

                opCode += "A20" + val2;

                opCode += "EC" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, currentScope);
                varTable.addItem(newItem1);

                opCode += "A9018D" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9008D" + newItem1.getTemp();

                opCode += "A201EC" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9018D" + newItem1.getTemp();

                if(inPrint) {
                    opCode += "A201AC" + newItem1.getTemp() + "FF";
                }

                totalBytesUsed += opCode.length()/2;

                opCodeOutput += opCode;
            }
            else if(type.equals("boolean")){
                String end;
                if(val2.equals("false")){
                    end = "FA";
                }
                else{
                    end = "F5";
                }
                opCode += "A2" + end;

                opCode += "EC" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, currentScope);
                varTable.addItem(newItem1);

                opCode += "A9018D" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9008D" + newItem1.getTemp();

                opCode += "A201EC" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9018D" + newItem1.getTemp();

                if(inPrint) {
                    opCode += "A201AC" + newItem1.getTemp() + "FF";
                }

                totalBytesUsed += opCode.length()/2;

                opCodeOutput += opCode;
            }
            else{
                storeHeap(val2);

                String end = Integer.toHexString(heapEnd);
                if(end.length() < 2){
                    end = "0" + end;
                }

                opCode += "AE" + end;

                opCode += "EC" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, currentScope);
                varTable.addItem(newItem1);

                opCode += "A9018D" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9008D" + newItem1.getTemp();

                opCode += "A201EC" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9018D" + newItem1.getTemp();

                if(inPrint) {
                    opCode += "A201AC" + newItem1.getTemp() + "FF";
                }

                totalBytesUsed += opCode.length()/2;

                opCodeOutput += opCode;
            }

        }
        else if(val2.matches("[a-z]")){
            String type = getVariableType(val2);

            if(type.equals("int")){

                opCode += "A20" + val1;

                opCode += "EC" + varTable.getItem(val2.charAt(0), getVariableScope(val2)).getTemp();

                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, currentScope);
                varTable.addItem(newItem1);

                opCode += "A9018D" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9008D" + newItem1.getTemp();

                opCode += "A201EC" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9018D" + newItem1.getTemp();

                if(inPrint) {
                    opCode += "A201AC" + newItem1.getTemp() + "FF";
                }

                totalBytesUsed += opCode.length()/2;

                opCodeOutput += opCode;
            }
            else if(type.equals("boolean")){
                String end;
                if(val1.equals("false")){
                    end = "FA";
                }
                else{
                    end = "F5";
                }
                opCode += "A2" + end;

                opCode += "EC" + varTable.getItem(val2.charAt(0), getVariableScope(val2)).getTemp();

                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, currentScope);
                varTable.addItem(newItem1);

                opCode += "A9018D" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9008D" + newItem1.getTemp();

                opCode += "A201EC" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9018D" + newItem1.getTemp();

                if(inPrint) {
                    opCode += "A201AC" + newItem1.getTemp() + "FF";
                }

                totalBytesUsed += opCode.length()/2;
//
                opCodeOutput += opCode;
            }
            else{
                storeHeap(val2);

                String end = Integer.toHexString(heapEnd);
                if(end.length() < 2){
                    end = "0" + end;
                }

                opCode += "AE" + end;

                opCode += "EC" + varTable.getItem(val1.charAt(0), getVariableScope(val1)).getTemp();

                int numVars = varTable.getNumVariables();

                StaticVariableTableItem newItem1 = new StaticVariableTableItem("T" + numVars + "XX", (char) tempCount++, currentScope);
                varTable.addItem(newItem1);

                opCode += "A9018D" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9008D" + newItem1.getTemp();

                opCode += "A201EC" + newItem1.getTemp();

                opCode += "D005";

                opCode += "A9018D" + newItem1.getTemp();

                if(inPrint) {
                    opCode += "A201AC" + newItem1.getTemp() + "FF";
                }

                totalBytesUsed += opCode.length()/2;

                opCodeOutput += opCode;
            }

        }
        else{
            storeHeap(val1);
            String end = Integer.toHexString(heapEnd);

            if(end.length() < 2){
                end = "0" + end;
                if(inPrint) {
                    opCode += "A0" + end + "A202FF";
                }

                totalBytesUsed += opCode.length()/2;

                opCodeOutput += opCode;
            }
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
