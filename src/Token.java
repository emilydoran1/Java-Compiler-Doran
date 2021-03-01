import java.util.regex.Pattern;

public class Token {
    private String name;
    private String value;
    private int line;
    private int position;

    String regexInteger = "[0-9]+";
    String regexCharacter = "[a-z]";

    public Token(String name, String value, int line, int position){
        this.name = name;
        this.value = value;
        this.line = line;
        this.position = position;
    }

    public String getName(){
        return name;
    }

    public String getValue(){
        return value;
    }

    public int getLine(){
        return line;
    }

    public int getPosition(){
        return position;
    }
}
