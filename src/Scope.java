import java.util.ArrayList;
import java.util.Hashtable;

public class Scope {
    private int scopeNum;
    private ArrayList<Scope> children = new ArrayList<Scope>();
    private Scope parent;
    private Scope current;

    private Hashtable<String, SymbolTableItem> scopeItems = new Hashtable<String, SymbolTableItem>();

    public Scope(int scopeNum, Hashtable<String,SymbolTableItem> scopeItems){
        this.scopeItems = scopeItems;
        this.scopeNum = scopeNum;
    }

    public void addItem(String name, SymbolTableItem item){
        scopeItems.put(name, item);
    }

    public Hashtable<String, SymbolTableItem> getScopeItems(){
        return scopeItems;
    }

    public int getScopeNum(){
        return scopeNum;
    }

//    public ArrayList<Scope> getChildren(){
//        return children;
//    }

    public Scope getParent(){
        return parent;
    }

    public void setParent(Scope parent){
        this.parent = parent;
    }

    /*public void moveParent(){
        if(current.getParent() != null ){
            current = current.getParent();
        }
        else{
            // TODO: error logging
        }
    }*/

//    public void addChild(Scope child){
//        children.add(child);
//    }
}
