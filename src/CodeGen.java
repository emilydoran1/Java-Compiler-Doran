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
                    initializePrint(child.getChildren().get(0).getName().charAt(0), currentScope);
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

        System.out.println(value);

        String end = Integer.toHexString(heapEnd);
        if(end.length() < 2){
            end = "0" + end;
        }

        String opCode = "A9" + end + "8D" + varTable.getItem(variableName, scope).getTemp();

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

    }

    public void storeHeap(String value){
        // if we have a string, ignore the quotes
        if(value.charAt(0) == '\"'){
            value = value.substring(1, value.length()-1);
        }

        for(int i=0; i < value.length(); i++){
            String tempHeapOut = Integer.toHexString((int) value.charAt(i));
            if(tempHeapOut.length() < 2){
                tempHeapOut = "0" + tempHeapOut;
            }
            heapOutput += tempHeapOut;
            heapEnd--;
        }
        heapOutput += "00";
        heapEnd--;
    }

    public void assignStmtBoolTrue(char variableName, int scope){

        String opCode = "A9008D" + varTable.getItem(variableName, scope).getTemp();

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

    }

    public void assignStmtBoolFalse(char variableName, int scope){

        String opCode = "A9008D" + varTable.getItem(variableName, scope).getTemp();

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

    }

    public void initializePrint(char variableName, int scope){
        String opCode = "";

        if(Character.toString(variableName).matches("[a-z]")){
            if(symbolTable.get(scope).getScopeItems().get(Character.toString(variableName)).getType().equals("int")) {
                opCode += "AC" + varTable.getItem(variableName, scope).getTemp() + "A201FF";
            }
            else if (symbolTable.get(scope).getScopeItems().get(Character.toString(variableName)).getType().equals("string")
                || symbolTable.get(scope).getScopeItems().get(Character.toString(variableName)).getType().equals("boolean")) {
                opCode += "AC" + varTable.getItem(variableName, scope).getTemp() + "A202FF";
            }
        }
        else if(Character.toString(variableName).matches("[0-9]")){
            opCode += "A00" + Character.toString(variableName) + "A201FF";
        }

        totalBytesUsed += opCode.length()/2;

        opCodeOutput += opCode;

    }

    public String outputToString(){
        String val = "2";
        String result = opCodeOutput.replaceAll("(.{" + val + "})", "$1 ").trim();
        return result;
    }
}
