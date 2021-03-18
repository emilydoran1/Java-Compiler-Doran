/**
 * This program creates a CST (concrete syntax tree)
 *
 * @author Emily Doran
 *
 */

public class ConcreteSyntaxTree {

    private Node root = null;
    private Node current;

    public void addNode(String name, String kind){
        Node node = new Node(name);

        // check if this node needs to be the root node
        if(root == null){
            root = node;
        }
        else{
            // we are a child node
            // set parent to be the current node
            node.setParent(current);

            // add node to parent's child ArrayList
            current.addChild(node);
        }
        // we are an interior/branch node
        if(kind.equals("branch")){
            // update current node pointer to node
            current = node;
        }
    }

    /**
     * Move up to our parent node (if possible)
     */
    public void moveParent(){
        if(current.getParent() != null && current.getParent().getName() != null){
            current = current.getParent();
        }
        else{
            // TODO: error logging
        }
    }

    /**
     * Returns a String representation of our tree
     * @return String representation of tree
     */
    public String toString(){
        String traversalResult = "";



        return traversalResult;
    }
}