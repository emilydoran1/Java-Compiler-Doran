import java.util.regex.Pattern;

public class Lexer {
    private int lastPosition;
    private int currentPosition;

    // TODO: define Rule Order
    // keyword (1)
    private boolean checkKeyword(String stringToCheck){
        boolean isKeyword = false;
        return isKeyword;
    }

    // id (2)
    private boolean checkId(String stringToCheck){
        boolean isId = false;
        return isId;
    }

    // symbol (3)
    private boolean checkSymbol(char charToCheck){
        boolean isSymbol = false;
        return isSymbol;
    }

    // digit (4)
    private boolean checkDigit(String stringToCheck){
        String regexInteger = "[0-9]+";
        boolean isDigit = false;
        return isDigit;
    }

    // char (5)
    private boolean checkChar(char charToCheck){
        String regexCharacter = "[a-z]";
        boolean isChar = false;
        return isChar;
    }
}
