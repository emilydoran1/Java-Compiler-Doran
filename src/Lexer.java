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
            longestMatch += line.charAt(i);
//            System.out.println(longestMatch);

            // check if we are inside a comment
            if (insideComment == false) {

                // check if longest match is a keyword - update positions
                if (checkKeyword(longestMatch)) {
                    lastFound = longestMatch;
                    lastFoundEnd = i;

                    // check if we are at the end of the line
                    if (i == line.length()-1){
                        Token tok = new Token("Test", lastFound, currentLine, lastFoundStart);
                        System.out.println(tok.toString());
                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = lastFoundEnd+1;
                        i = lastFoundEnd;
                    }

                }
                // check if longest match is an id - update positions
                else if (checkId(longestMatch)) {
                    lastFound = longestMatch;
                    lastFoundEnd = i;

                    // check if we are at the end of the line
                    if (i == line.length()-1 && insideQuotes == false){
                        Token tok = new Token("Test", lastFound, currentLine, lastFoundStart);
                        System.out.println(tok.toString());
                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = lastFoundEnd+1;
                        i = lastFoundEnd;
                    }
                }
                // check if longest match is a symbol or if it is stop point
                else if (checkSymbol(line.charAt(i))) {
                    if(lastFound == ""){
                        lastFound = longestMatch;
                        lastFoundStart = i;
                        lastFoundEnd = i;

                        // check if we are at the end of the line
                        if(i == line.length()-1){
                            Token tok = new Token("Test", lastFound, currentLine, lastFoundStart);
                            System.out.println(tok.toString());
                            longestMatch = "";
                            lastFound = "";
                            lastFoundStart = lastFoundEnd+1;
                            i = lastFoundEnd;
                        }
                    }
                    else{
                        // check if we have a two character symbol
                        if (longestMatch.equals("!=") || longestMatch.equals("==")) {
                            lastFound = longestMatch;
                            lastFoundEnd = i;

                            // check if we are at the end of the line
                            if (i == line.length()-1 && insideQuotes == false){
                                Token tok = new Token("Test", lastFound, currentLine, lastFoundStart);
                                System.out.println(tok.toString());
                                longestMatch = "";
                                lastFound = "";
                                lastFoundStart = lastFoundEnd+1;
                                i = lastFoundEnd;
                            }
                        }

                        Token tok = new Token("Test", lastFound, currentLine, lastFoundStart);
                        System.out.println(tok.toString());

                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = lastFoundEnd+1;
                        i = lastFoundEnd;
                    }

                }
                // check if longest match is a digit - update positions
                else if (checkDigit(line.charAt(i))) {
                    lastFound = longestMatch;
                    lastFoundEnd = i;

                    if (i == line.length()-1){
                        Token tok = new Token("Test", lastFound, currentLine, lastFoundStart);
                        System.out.println(tok.toString());
                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = lastFoundEnd+1;
                        i = lastFoundEnd;
                    }
                }

                // check if we are in a string

                // check if we are in a comment
                else if(checkComment(longestMatch) == true){
                    Token tok = new Token("Test", lastFound, currentLine, lastFoundStart);
                    System.out.println(tok.toString());

                    longestMatch = "";
                    lastFound = "";
                    lastFoundStart = lastFoundEnd+1;
                    i = lastFoundEnd;
                }

                // check if current char is whitespace
                else if (checkWhitespace(line.charAt(i))) {
                    if(lastFound != ""){
                        Token tok = new Token("Test", lastFound, currentLine, lastFoundStart);
                        System.out.println(tok.toString());

                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = lastFoundEnd+1;
                        i = lastFoundEnd;
                    }
                    else{
                        // skip over white space, but update index of start
                        longestMatch = "";
                        lastFoundStart = i+1;
                    }

                }

                // check if we are at the end of line and not inside string
                else if (i == line.length()-1 && insideQuotes == false){
                    Token tok = new Token("Test", lastFound, currentLine, lastFoundStart);
                    System.out.println(tok.toString());
                    longestMatch = "";
                    lastFound = "";
                    lastFoundStart = lastFoundEnd+1;
                    i = lastFoundEnd;
                }

            }

            // we are inside of a comment
            else{
                checkComment(longestMatch);
                if(insideComment == false){
                    longestMatch = "";
                    lastFound = "";
                    lastFoundStart = i+1;
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
    private boolean checkId(String stringToCheck){
        // an Id is a single character a-z
        String regexId = "[a-z]";
        boolean isId = false;

        if (Pattern.matches(regexId, stringToCheck)) {
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
    private boolean checkDigit(char charToCheck){
        // a digit is a single number 0-9
        String regexInteger = "[0-9]";
        boolean isDigit = false;

        if (Pattern.matches(regexInteger, Character.toString(charToCheck))) {
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

    // check whitespace
    private boolean checkWhitespace(char charToCheck){
        boolean isWhitespace = false;
        String regexWhitespace = "\\s";
        if (Pattern.matches(regexWhitespace, Character.toString(charToCheck))) {
            isWhitespace = true;
        }
        return isWhitespace;
    }

    // check if we are inside a comment
    private boolean checkComment(String stringToCheck){
        boolean isComment = false;

        if (stringToCheck.contains("/*")) {
            isComment = true;
            insideComment = true;
        }

        if (stringToCheck.contains("*/")) {
            isComment = true;
            insideComment = false;
        }

        return isComment;
    }

}
