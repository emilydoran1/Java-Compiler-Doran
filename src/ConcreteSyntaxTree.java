public class ConcreteSyntaxTree {

    private Node root = null;
    private Node current;

    public void addNode(String name, String kind){
        Node node = new Node(name);
        // check if this node needs to be the root node
        if(root == null){
            root = node;
        }
    }
}
