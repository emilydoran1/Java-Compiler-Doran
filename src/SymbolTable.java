import java.util.ArrayList;
import java.util.Hashtable;

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
