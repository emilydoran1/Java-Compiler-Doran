import java.util.ArrayList;
import java.util.Set;

/**
 * This program creates a Symbol Table to be used for semantic analysis scope checking and further compilation.
 *
 * @author Emily Doran
 *
 */
public class SymbolTable {
    private ArrayList<Scope> symbolTable;

    /**
     * Initialize the symbol table to be the ArrayList<Scope>
     */
    public SymbolTable(){
        symbolTable = new ArrayList<Scope>();
    }

    /**
     * Add a scope to the symbol table
     * @param scope to add
     */
    public void addScope(Scope scope){
        symbolTable.add(scope);
    }

    /**
     * Get the entire symbol table
     * @return symbol table
     */
    public ArrayList<Scope> getSymbolTable(){
        return symbolTable;
    }

    /**
     * Get the symbol table size
     * @return size of symbol table
     */
    public int size(){
        return symbolTable.size();
    }

    /**
     * Get a specific Scope from symbol table
     * @return Scope at index i of symbol table
     */
    public Scope get(int i){
        return symbolTable.get(i);
    }

    /**
     * Print warnings for uninitialized and unused variables
     */
    public int printWarnings(){
        int numWarnings = 0;
        // iterate through symbol table to check if variables are initialized/used
        for(int i = 0; i < symbolTable.size(); i++){
            Set<String> keys = symbolTable.get(i).getScopeItems().keySet();
            for(String key: keys){
                if(symbolTable.get(i).getScopeItems().get(key).getIsUsed() == false &&
                        symbolTable.get(i).getScopeItems().get(key).getIsInitialized() == true){
                    System.out.println("SEMANTIC ANALYSIS: WARNING: Variable [ " + key +
                            " ] is declared and initialized but never used.");

                    numWarnings++;
                }
                else {
                    if(symbolTable.get(i).getScopeItems().get(key).getIsInitialized() == false &&
                            symbolTable.get(i).getScopeItems().get(key).getIsUsed() == false){
                        System.out.println("SEMANTIC ANALYSIS: WARNING: Variable [ " + key +
                                " ] is declared but never initialized or used.");
                        numWarnings++;
                    }
                    else if(symbolTable.get(i).getScopeItems().get(key).getIsUsed() == false){
                        System.out.println("SEMANTIC ANALYSIS: WARNING: Variable [ " + key +
                                " ] is declared but never used.");
                        numWarnings++;
                    }
                    else if(symbolTable.get(i).getScopeItems().get(key).getIsInitialized() == false &&
                            symbolTable.get(i).getScopeItems().get(key).getIsUsed() == true){
                        System.out.println("SEMANTIC ANALYSIS: WARNING: Variable [ " + key +
                                " ] is declared and used but never initialized.");
                        numWarnings++;
                    }
                }
            }
        }
        return numWarnings;
    }

    /**
     * Get the string representation of the symbol table
     */
    public void printSymbolTable(){
        for(int i = 0; i < symbolTable.size(); i++){
            Set<String> keys = symbolTable.get(i).getScopeItems().keySet();
            for(String key: keys){
                System.out.printf("%-6s%-9s%-7s%-4s\n", key, symbolTable.get(i).getScopeItems().get(key).getType(),
                        (i), symbolTable.get(i).getScopeItems().get(key).getLineNum());
            }
        }
    }

}
