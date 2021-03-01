import java.util.regex.Pattern;

public class Lexer {
    private int lastPosition;
    private int currentPosition;

    // define Rule Order
    // keyword (1)
    private boolean checkKeyword(String stringToCheck){
        // keywords are: while, print, if, int, string, boolean, true, false
        String regexKeyword = "(while)|(print)|(i((f)|(nt)))(string)|(boolean)|(true)|(false)";
        boolean isKeyword = false;

        if (Pattern.matches(regexKeyword, stringToCheck)) {
            isKeyword = true;
        }
        return isKeyword;
    }

    // id (2)
    private boolean checkId(char charToCheck){
        // an Id is a single character a-z
        String regexCharacter = "[a-z]";
        boolean isId = false;

        if (Pattern.matches(regexCharacter, Character.toString(charToCheck))) {
            isId = true;
        }

        return isId;
    }

    // symbol (3)
    private boolean checkSymbol(char charToCheck){
        // symbols allowed are {, }, !, =, +, (, ), $
        String regexSymbol = "[{}!=+()$]";
        boolean isSymbol = false;

        if (Pattern.matches(regexSymbol, Character.toString(charToCheck))) {
            isSymbol = true;
        }

        return isSymbol;
    }

    // digit (4)
    private boolean checkDigit(String stringToCheck){
        // a digit is 1 or more numbers 0-9
        String regexInteger = "[0-9]+";
        boolean isDigit = false;

        if (Pattern.matches(regexInteger, stringToCheck)) {
            isDigit = true;
        }

        return isDigit;
    }

}
