import java.util.ArrayList;

/**
 * This class represents a jump table for the 6502a instruction set
 *
 * @author Emily Doran
 *
 */
public class JumpTable {

    ArrayList<JumpTableItem> jumpTable;

    /**
     * Initialize the static variable table to be the ArrayList<StaticVariableTableItem>
     */
    public JumpTable(){
        jumpTable = new ArrayList<JumpTableItem>();
    }

    /**
     * Get the number of variables in table
     * @return number of variables
     */
    public int getNumVariables(){
        return jumpTable.size();
    }

    /**
     * Add a variable to jump table
     * @param item to add
     */
    public void addItem(JumpTableItem item){
        jumpTable.add(item);
    }

    /**
     * Get the entire jump table
     * @return jump table
     */
    public ArrayList<JumpTableItem> getJumpTable(){
        return jumpTable;
    }

    /**
     * Get a specific table item
     * @return JumpTableItem with temp
     */
    public JumpTableItem getItem(String temp){
        JumpTableItem item = jumpTable.get(0);
        for(int i = 0; i < jumpTable.size(); i++){
            if(jumpTable.get(i).getTemp().equals(temp)){
                item = jumpTable.get(i);
            }
        }
        return item;
    }

    /**
     * Get the string representation of the jump table
     */
    public void printJumpTable(){
        for(int i = 0; i < jumpTable.size(); i++){
            System.out.printf("%-6s%-7s\n", jumpTable.get(i).getTemp(), Integer.toHexString(jumpTable.get(i).getDistance()).toUpperCase());
        }
    }
}
