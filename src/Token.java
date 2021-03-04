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
            if(value.equals("while"))
                tokenKind = "T_WHILE";
            else if(value.equals("print"))
                tokenKind = "T_PRINT]";
            else if(value.equals("string") || value.equals("int") || value.equals("boolean"))
                tokenKind = "T_VARIABLE_TYPE";
            else if(value.equals("if"))
                tokenKind = "T_IF]";
            else if(value.equals("while"))
                tokenKind = "T_WHILE]";
            else if(value.equals("true"))
                tokenKind = "T_BOOL_TRUE]";
            else if(value.equals("false"))
                tokenKind = "T_BOOL_FALSE]";
        }
        else if (Pattern.matches(regexId, value)) {
            tokenKind = "T_ID";
        }
        else if (Pattern.matches(regexSymbol, value)) {
            tokenKind = "";
            
            if(value.equals("{"))
                tokenKind = "T_L_BRACE";
            else if(value.equals("}"))
                tokenKind = "T_R_BRACE";
            else if(value.equals("="))
                tokenKind = "T_ASSIGN_OP";
            else if(value.equals("=="))
                tokenKind = "T_EQUALITY_OP";
            else if(value.equals("!="))
                tokenKind = "T_INEQUALITY_OP";
            else if(value.equals("+"))
                tokenKind = "T_ADDITION_OP";
            else if(value.equals("("))
                tokenKind = "T_L_PAREN";
            else if(value.equals(")"))
                tokenKind = "T_R_PAREN";
            else if(value.equals("$"))
                tokenKind = "T_EOP";
        }
        else if (Pattern.matches(regexDigit, value)) {
            tokenKind = "T_DIGIT";
        }

        return tokenKind;
    }
}
