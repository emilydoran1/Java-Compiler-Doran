import java.util.regex.Pattern;

public class Token {
    private String kind;
    private String value;
    private int lineNum;
    private int position;

    String regexInteger = "[0-9]";
    String regexCharacter = "[a-z]";

    public Token(String kind, String value, int lineNum, int position){
        this.kind = kind;
        this.value = value;
        this.lineNum = lineNum;
        this.position = position;
    }

    public String getName(){
        return kind;
    }

    public String getValue(){
        return value;
    }

    public int getLine(){
        return lineNum;
    }

    public int getPosition(){
        return position;
    }
}
