import java.util.regex.Pattern;

/**
 * This program stores the information for token creation
 * @author Emily Doran
 *
 */
public class Token {

    private String kind;
    private String value;
    private int lineNum;
    private int position;


    public Token(String kind, String value, int lineNum, int position){
        this.kind = tokenKind(value, kind);
        this.value = value;
        this.lineNum = lineNum;
        this.position = position;
    }

    /**
     * Gets the token kind
     * @return token kind
     */
    public String getKind(){
        return kind;
    }

    /**
     * Gets the token value
     * @return token value
     */
    public String getValue(){
        return value;
    }

    /**
     * Gets the token line number
     * @return token line
     */
    public int getLine(){
        return lineNum;
    }

    /**
     * Gets the token position number
     * @return token position
     */
    public int getPosition(){
        return position;
    }

    /**
     * Prints the token string with all of the values
     * @return token string
     */
    public String toString(){
        return "DEBUG Lexer - " + getKind() + " [ " + getValue() + " ] found at (" + getLine() +
                ":" + getPosition() + ")";
    }

    /**
     * Determine the token kind from the value
     * @param  value, kind
     * @return token kind
     */
    public String tokenKind(String value, String kind){
        // define the regex for each token kind
        String tokenKind = "";
        String regexDigit = "[0-9]";
        String regexSymbol = "[{}!=+()$]";
        String regexId = "[a-z]";
        String regexKeyword = "(while)|(print)|(string)|(if)|(int)|(boolean)|(true)|(false)";


        // char tokens have the kind parameter already passed if we are in string, we don't need to get kind
        if(kind.equals("T_CHAR")){
            tokenKind = "T_CHAR";
        }

        // match the Keyword token value to the corresponding token name
        else if (Pattern.matches(regexKeyword, value)) {
            if(value.equals("while"))
                tokenKind = "T_WHILE";
            else if(value.equals("print"))
                tokenKind = "T_PRINT";
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

        // token kind for Ids is always T_ID
        else if (Pattern.matches(regexId, value)) {
            tokenKind = "T_ID";
        }

        // match the Symbol token value to the corresponding token name
        else if (Pattern.matches(regexSymbol, value) || value.equals("==") || value.equals("!=")) {
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

        // token kind for Digits is always T_DIGIT
        else if (Pattern.matches(regexDigit, value)) {
            tokenKind = "T_DIGIT";
        }
        // token kind for quotes is always T_QUOTE
        else if (value.equals("\"")) {
            tokenKind = "T_QUOTE";
        }

        return tokenKind;
    }
}
