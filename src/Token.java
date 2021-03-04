import java.util.regex.Pattern;

public class Token {
    private String kind;
    private String value;
    private int lineNum;
    private int position;

    String regexInteger = "[0-9]";
    String regexCharacter = "[a-z]";

    public Token(String kind, String value, int lineNum, int position){
        this.kind = tokenKind(value);
        this.value = value;
        this.lineNum = lineNum;
        this.position = position;
    }

    public String getKind(){
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

    public String toString(){
        return "DEBUG Lexer - " + getKind() + " [ " + getValue() + " ] found at (" + getLine() +
                ":" + getPosition() + ")";
    }

    // get the token kind
    public String tokenKind(String value){
        String tokenKind = "";
        String regexDigit = "[0-9]";
        String regexSymbol = "[{}!=+()$]";
        String regexId = "[a-z]";
        String regexKeyword = "(while)|(print)|(string)|(if)|(int)|(boolean)|(true)|(false)";

        if (Pattern.matches(regexKeyword, value)) {
            tokenKind = "T_KEYWORD";
        }
        else if (Pattern.matches(regexId, value)) {
            tokenKind = "T_ID";
        }
        else if (Pattern.matches(regexSymbol, value)) {
            tokenKind = "T_SYMBOL";
        }
        else if (Pattern.matches(regexDigit, value)) {
            tokenKind = "T_DIGIT";
        }

        return tokenKind;
    }
}
