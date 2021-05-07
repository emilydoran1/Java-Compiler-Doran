/**
 * This class stores the necessary information about data in the table
 *
 * @author Emily Doran
 *
 */
public class StaticVariableTableItem {

    private String temp;
    private int address;
    private char var;
    private int scope;

    public StaticVariableTableItem(String temp, char var, int scope){
        this.temp = temp;
        this.var = var;
        this.scope = scope;
    }

    /**
     * Sets the address of var in memory
     * @param address
     */
    public void setAddress(int address){
        this.address = address;
    }

    /**
     * Get the temp address of item
     * @return temp address in memory of var
     */
    public String getTemp(){
        return temp;
    }

    /**
     * Get the var name (single char)
     * @return var name
     */
    public char getVar(){
        return var;
    }

    /**
     * Get the scope of var
     * @return scope of var
     */
    public int getScope(){
        return scope;
    }

    /**
     * Get the address of item
     * @return address in memory of var
     */
    public int getAddress(){
        return address;
    }
}
