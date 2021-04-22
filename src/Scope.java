import java.util.ArrayList;
import java.util.Hashtable;

/**
 * This program creates a scope for the symbol table.
 *
 * @author Emily Doran
 *
 */
public class Scope {
    private int scopeNum;
    private ArrayList<Scope> children = new ArrayList<Scope>();
    private Scope parent;

    private Hashtable<String, SymbolTableItem> scopeItems = new Hashtable<String, SymbolTableItem>();

    /**
     * Creates a new instance of Scope
     * @param scopeNum, HashTable of variables in scope
     */
    public Scope(int scopeNum, Hashtable<String,SymbolTableItem> scopeItems){
        this.scopeItems = scopeItems;
        this.scopeNum = scopeNum;
    }

    /**
     * Add a variable to current scope
     * @param name of variable, SymbolTableItem containing variable type and line num
     */
    public void addItem(String name, SymbolTableItem item){
        scopeItems.put(name, item);
    }

    /**
     * Get the hashtable of scope items
     * @return scope items
     */
    public Hashtable<String, SymbolTableItem> getScopeItems(){
        return scopeItems;
    }

    /**
     * Get the scope number
     * @return scope number
     */
    public int getScopeNum(){
        return scopeNum;
    }

    /**
     * Get the scope's parent
     * @return Scope parent
     */
    public Scope getParent(){
        return parent;
    }

    /**
     * set the scope's parent
     * @parent parent Scope
     */
    public void setParent(Scope parent){
        this.parent = parent;
    }

}
