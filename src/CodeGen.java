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
                    else {
                        initializePrint(child.getChildren().get(0).getName().charAt(0), currentScope);
                    }
                }
                else if(child.getName().equals("Addition")){
                    //if(!child.getChildren().get(0).getChildren().get(1).getName().equals("Addition")) {
                        storeAddInts(child.getParent().getChildren().get(0).getName().charAt(0), child.getChildren().get(0), child.getChildren().get(1), currentScope);
                   // }

                }
                else{
                    if(child.getName().equals("BLOCK")){
                        currentScope++;
                    }
                    beginCodeGen(childChildren);
                }
            }
            // leaf node
            else{
                if(child.getName().equals("int") || child.getName().equals("boolean") || child.getName().equals("string")){
                    initializeVariable(child.getParent().getChildren().get(1).getName().charAt(0), currentScope);
                }
                else if(child.getParent().getChildren().size() > 1 && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().matches("[0-9]")){
                    assignStmtInt(child.getName().charAt(0), child.getParent().getChildren().get(1).getName(), currentScope);
                }
                else if(child.getParent().getChildren().size() > 1 && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().charAt(0) == '\"'){
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1).getName(), currentScope);
                }
                else if(child.getParent().getChildren().size() > 1 && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().equals("true")){
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1).getName(), currentScope);
                }
                else if(child.getParent().getChildren().size() > 1 && child.getName().matches("[a-z]") &&
                        child.getParent().getChildren().get(1).getName().equals("false")){
                    assignStmtString(child.getName().charAt(0), child.getParent().getChildren().get(1).getName(), currentScope);
                }
            }
            if(currentScope != 0) {
                currentScope--;
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
        storeHeap(value);

        String end = Integer.toHexString(heapEnd);
        if(end.length() < 2){
            end = "0" + end;
        }

        String opCode = "A9" + end + "8D" + varTable.getItem(variableName, scope).getTemp();

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

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
            /* printAddInts2(node2.getChildren().get(0), node2.getChildren().get(1), scope);

            opCode += "A90" +value1 + "6D" + varTable.getItem((char)(tempCount-1), scope).getTemp();

            opCode += "8D" + newItem.getTemp();

            opCode += "A201AC" + newItem.getTemp();

            */
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
                opCode += "AC" + varTable.getItem(variableName, scope).getTemp() + "A201FF";
            }
            else if (getVariableType(Character.toString(variableName)).equals("string")
                || getVariableType(Character.toString(variableName)).equals("boolean")) {
                opCode += "AC" + varTable.getItem(variableName, scope).getTemp() + "A202FF";
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

        storeHeap(val);

        String end = Integer.toHexString(heapEnd);
        if(end.length() < 2){
            end = "0" + end;
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
}
