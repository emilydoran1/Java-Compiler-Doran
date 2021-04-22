import java.util.ArrayList;

public class SymbolTableItem {

    private String varType;
    private boolean isUsed;
    private boolean isInitialized;
    private int lineNum;

    private ArrayList<SymbolTableItem> children = new ArrayList<SymbolTableItem>();
    private SymbolTableItem parent;

    public SymbolTableItem(String varType, int lineNum) {
        this.varType = varType;
        this.lineNum = lineNum;
        isUsed = false;
        isInitialized = false;
    }

    public String getType(){
        return varType;
    }

    public int getLineNum(){
        return lineNum;
    }

    public void setUsed(){
        isUsed = true;
    }

    public boolean getIsUsed(){
        return isUsed;
    }

    public void setInitialized(){
        isInitialized = true;
    }

    public boolean getIsInitialized(){
        return isInitialized;
    }

}
