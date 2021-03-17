import java.util.ArrayList;

/**
 * This program stores the information for node creation
 * @author Emily Doran
 *
 */

public class Node {
    private String name;
    private ArrayList<Node> children;
    private Node parent;

    /**
     * Creates a new Node with all 3 parameters passed in
     * @param name of Node, ArrayList of Node children, parent of Node
     */
    public Node(String name, ArrayList<Node> children, Node parent){
        this.name = name;
        this.children = children;
        this.parent = parent;
    }

    /**
     * Creates a new Node with only a name passed in
     * @param name of Node
     */
    public Node(String name) {
        this.name = name;
    }

    /**
     * Returns the Node's name
     * @return String Node name
     */
    public String getName(){
        return name;
    }

    /**
     * Returns list of the Node's children
     * @return ArrayList<Node> of Node's children
     */
    public ArrayList<Node> getChildren(){
        return children;
    }

    /**
     * Returns the Node's parent
     * @return Node Node's parent
     */
    public Node getParent(){
        return parent;
    }

    /**
     * Sets the Node's parent if it wasn't set when Node was created
     * @param parent of Node
     */
    public void addParent(Node parent){
        this.parent = parent;
    }

    /**
     * Adds a child to the Node
     * @param child of Node
     */
    public void addChild(Node child){
        children.add(child);
    }
}
