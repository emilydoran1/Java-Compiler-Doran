import java.util.ArrayList;

public class SymbolTableItem {

    private String varType;
    private boolean isUsed;
    private boolean isInitialized;

    private ArrayList<SymbolTableItem> children = new ArrayList<SymbolTableItem>();
    private SymbolTableItem parent;

    public SymbolTableItem(String varType) {
        this.varType = varType;
        isUsed = false;
        isInitialized = false;
    }

    public String getType(){
        return varType;
    }

    public void setUsed(){
        isUsed = true;
    }

    public boolean getIsUsed(){
        return isUsed;
    }

    public void setInitialized(){
        isInitialized = true;
    }

    public boolean getIsInitialized(){
        return isInitialized;
    }

    /**
     * Returns list of the item's children
     * @return ArrayList<SymbolTableItem> of table item's children
     */
    public ArrayList<SymbolTableItem> getChildren(){
        return children;
    }

    /**
     * Returns the item's parent
     * @return SymbolTableItem table item's parent
     */
    public SymbolTableItem getParent(){
        return parent;
    }

    /**
     * Sets the item's parent if it wasn't set when SymbolTableItem was created
     * @param parent of table item
     */
    public void setParent(SymbolTableItem parent){
        this.parent = parent;
    }

    /**
     * Adds a child to the symbol table item
     * @param child of table item
     */
    public void addChild(SymbolTableItem child){
        children.add(child);
    }

}
