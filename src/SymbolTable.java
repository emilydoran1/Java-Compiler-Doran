import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Set;

public class SymbolTable {
    private Scope root = null;
    private Scope current;

    private ArrayList<Scope> symbolTable;

    public SymbolTable(){
        symbolTable = new ArrayList<Scope>();
    }

    public void addScope(Scope scope){
        symbolTable.add(scope);
    }

    public ArrayList<Scope> getSymbolTable(){
        return symbolTable;
    }

    public int size(){
        return symbolTable.size();
    }

    public Scope get(int i){
        return symbolTable.get(i);
    }

    public String toString(){
        String output = "";
        for(int i = 0; i < symbolTable.size(); i++){
            output += "Scope: " + (i+1) + "\n";
            Set<String> keys = symbolTable.get(i).getScopeItems().keySet();
            for(String key: keys){
                output += "value: " + key +
                        "  type: " + symbolTable.get(i).getScopeItems().get(key).getType() +
                        "  line num: " + symbolTable.get(i).getScopeItems().get(key).getLineNum() +
                        "  isUsed: " + symbolTable.get(i).getScopeItems().get(key).getIsUsed() +
                        "  isInitialized: " + symbolTable.get(i).getScopeItems().get(key).getIsInitialized() + "\n";
            }
            output += "\n";
        }
        return output;
    }

    /*public static void main(String[] args){
        SymbolTable sym = new SymbolTable();
        Hashtable<String, SymbolTableItem> newHash = new Hashtable<String, SymbolTableItem>();
        Scope test = new Scope(0, newHash);
        sym.addScope(test);

        SymbolTableItem newItem = new SymbolTableItem("boolean");
        test.addItem("a", newItem);
        System.out.println(sym.get(0).getScopeItems().get("a").getType());
    }*/

}
