/**
 * This program creates a syntax tree
 *
 * @author Emily Doran, modified Alan G. Labouseur's implementation,
 * based on the 2009 work by Michael Ardizzone and Tim Smith.
 *
 */

public class SyntaxTree {

    private Node root = null;
    private Node current;

    private String traversalResult = "";

    /**
     * Add a node to our tree with name and kind passed in
     */
    public void addNode(String name, String kind){
        Node node = new Node(name);

        // check if this node needs to be the root node
        if(root == null){
            root = node;
            current = node;
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
     * Get root node
     * @return Node root node
     */
    public Node getRoot() {
        return root;
    }

    /**
     * Returns a String representation of our tree
     * @return String representation of tree
     */
    public String toString(){

        // call expand() to expand from root
        traversalResult += expand(root, 0);

        return traversalResult;
    }

    /**
     * Recursive function to handle the expansion of the nodes.
     * @param node, depth of tree
     */
    public String expand(Node node, int depth){
        for (int i = 0; i < depth; i++)
        {
            traversalResult += "-";
        }
        // if there are no children
        if (node.getChildren() == null || node.getChildren().size() == 0)
        {
            traversalResult += "[" + node.getName() + "]";
            traversalResult += "\n";
        }
        // there are children
        else{
            traversalResult += "<" + node.getName() + "> \n";
            // recursively expand the children
            for (int i = 0; i < node.getChildren().size(); i++)
            {
                expand(node.getChildren().get(i), depth + 1);
            }
        }
        return traversalResult;
    }

}