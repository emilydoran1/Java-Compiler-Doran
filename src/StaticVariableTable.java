import java.util.ArrayList;

public class StaticVariableTable {

    ArrayList<StaticVariableTableItem> variableTable;

    /**
     * Initialize the static variable table to be the ArrayList<StaticVariableTableItem>
     */
    public StaticVariableTable(){
        variableTable = new ArrayList<StaticVariableTableItem>();
    }

    /**
     * Add a variable to table
     * @param item to add
     */
    public void addItem(StaticVariableTableItem item){
        variableTable.add(item);
    }

    /**
     * Get the entire variable table
     * @return variable table
     */
    public ArrayList<StaticVariableTableItem> getVariableTable(){
        return variableTable;
    }

    /**
     * Get the number of variables in table
     * @return number of variables
     */
    public int getNumVariables(){
        return variableTable.size();
    }

    /**
     * Get a specific variable table item
     * @return StaticVariableTableItem with char name and int scopeNum
     */
    public StaticVariableTableItem getItem(char name, int scopeNum){
        StaticVariableTableItem item = variableTable.get(0);
        for(int i = 0; i < variableTable.size(); i++){
            if(variableTable.get(i).getScope() == scopeNum && variableTable.get(i).getVar() == name){
                item = variableTable.get(i);
            }
        }
        return item;
    }
}
