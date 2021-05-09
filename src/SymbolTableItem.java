import java.util.ArrayList;
/**
 * This program creates a symbol table item which contains the necessary information about each variable stored in
 * the symbol table. It allows us to keep track of when (if) each variable is used/initialized and also it's type
 * and original token declaration line number
 *
 * @author Emily Doran
 *
 */
public class SymbolTableItem {

    private String varType;
    private boolean isUsed;
    private boolean isInitialized;
    private int lineNum;

    /**
     * Create a new SymbolTableItem and set booleans isUsed, isInitialized to false initially.
     * @param varType, lineNum of declaration
     */
    public SymbolTableItem(String varType, int lineNum) {
        this.varType = varType;
        this.lineNum = lineNum;
        isUsed = false;
        isInitialized = false;
    }

    /**
     * Get the type of symbol table item
     * @return variable type
     */
    public String getType(){
        return varType;
    }

    /**
     * Get the line number of symbol table item
     * @return variable lineNum
     */
    public int getLineNum(){
        return lineNum;
    }

    /**
     * Set variable isUsed boolean to true if it is used.
     */
    public void setUsed(){
        isUsed = true;
    }

    /**
     * Get isUsed boolean for symbol table item to see if we ever use the variable.
     */
    public boolean getIsUsed(){
        return isUsed;
    }

    /**
     * Set variable isInitialized boolean to true if it is initialzied.
     */
    public void setInitialized(){
        isInitialized = true;
    }

    /**
     * Get isInitialized boolean for symbol table item to see if we ever initialize the variable.
     */
    public boolean getIsInitialized(){
        return isInitialized;
    }

}
