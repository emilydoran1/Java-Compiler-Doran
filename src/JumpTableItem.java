/**
 * This class stores the necessary information about data in the jump table
 *
 * @author Emily Doran
 *
 */
public class JumpTableItem {

    private String temp;
    private int distance;

    public JumpTableItem(String temp){
        this.temp = temp;
    }

    /**
     * Sets the distance
     * @param distance of jump
     */
    public void setDistance(int distance){
        this.distance = distance;
    }

    /**
     * Gets the distance
     * @return distance of jump
     */
    public int getDistance(){
        return distance;
    }

    /**
     * Gets the temp name of jump table item
     * @return temp name
     */
    public String getTemp(){
        return temp;
    }
}
