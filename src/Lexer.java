import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Lexer {
    private int lastPosition = -1;
    private int currentPosition = 0;
    private boolean insideQuotes = false;
    private boolean insideComment = false;

    private int currentLine = 1;

    private int programNum = 1;

    public Lexer(String passedFile) {
        try {
            File file = new File(passedFile);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                getToken(line);
//                System.out.println(line);
                currentLine++;
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //getToken(passedFile, programNum);
    }

    private void getToken(String line){
//        System.out.println("INFO Lexer - Lexing program " + programNum + "...");
        String longestMatch = "";
        String lastFound = "";
        int lastFoundStart = 0;
        int lastFoundEnd = 0;

        for(int i = 0; i < line.length(); i++) {
            // check if we are inside a comment
            if (insideComment == false) {
                longestMatch += line.charAt(i);
                if (checkKeyword(longestMatch)) {
                    lastFound = longestMatch;
                    lastFoundEnd = i;

                } else if (checkId(longestMatch)) {
                    lastFound = longestMatch;
                    lastFoundEnd = i;
                } else if (checkSymbol(line.charAt(i))) {
                    if(lastFound == ""){
                        lastFound = longestMatch;
                        lastFoundEnd = i;
                    }
                    else if(lastFound == "" && i == line.length()){
                        System.out.println(lastFound + " found at " + lastFoundStart);
                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = lastFoundEnd+1;
                        i = lastFoundEnd;
                    }
                    else{
                       System.out.println(lastFound + " found at " + lastFoundStart);
                       longestMatch = "";
                       lastFound = "";
                       lastFoundStart = lastFoundEnd+1;
                       i = lastFoundEnd;
                    }

                }
            }
        }

    }

    // define Rule Order
    // keyword (1)
    private boolean checkKeyword(String stringToCheck){
        // keywords are: while, print, if, int, string, boolean, true, false
        String regexKeyword = "(while)|(print)|(string)|(if)|(int)|(boolean)|(true)|(false)";
        boolean isKeyword = false;

        if (Pattern.matches(regexKeyword, stringToCheck)) {
            isKeyword = true;
        }
        return isKeyword;
    }

    // id (2)
    private boolean checkId(String charToCheck){
        // an Id is a single character a-z
        String regexCharacter = "[a-z]";
        boolean isId = false;

        if (Pattern.matches(regexCharacter, charToCheck)) {
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
        // a digit is a single number 0-9
        String regexInteger = "[0-9]";
        boolean isDigit = false;

        if (Pattern.matches(regexInteger, stringToCheck)) {
            isDigit = true;
        }

        return isDigit;
    }

    // char (5)
    private boolean checkChar(char charToCheck){
        // an char is a single character a-z that is inside a quote
        String regexCharacter = "[a-z]";
        boolean isChar = false;

        if ((insideQuotes == true) && (Pattern.matches(regexCharacter, Character.toString(charToCheck)))) {
            isChar = true;
        }

        return isChar;
    }

}
