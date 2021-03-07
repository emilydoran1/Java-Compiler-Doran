import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * This program provides lexical analysis on the passed file and
 * prints out tokens, errors, and warnings according to our grammar.
 *
 * @author Emily Doran
 *
 */
public class Lexer {

    // store true/false value to know if we are inside a string
    private boolean insideQuotes = false;

    // store true/false value to know if we are inside a comment
    private boolean insideComment = false;

    // store current line number for printing token positions
    private int currentLine = 1;

    // store current program info to print message after we finish lexing each program
    private int programNum = 1;
    private int numErrors = 0;
    public boolean newProgram = true;

    // store if we are in verbose test mode or not
    boolean verboseTestMode;

    public Lexer(String passedFile, boolean verboseMode) {
        verboseTestMode = verboseMode;

        try {
            File file = new File(passedFile);
            Scanner scanner = new Scanner(file);

            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                getToken(line);

                currentLine++;
            }

            // check if EOP char is forgotten at end
            if(newProgram == false){
                System.out.println("WARNING Lexer - Missing EOP Character '$'");

                // append $ to file so that compilation continues without error
                FileWriter fileWriter = new FileWriter(file, true);
                PrintWriter printWriter = new PrintWriter(fileWriter);
                printWriter.println("$");
                printWriter.close();
            }
            // check if comment is left open at end of program
            if(insideComment == true){
                System.out.println("WARNING Lexer - Unclosed Comment at End of Program");
            }
            // check if quote is left open at end of program
            if(insideQuotes == true){
                System.out.println("WARNING Lexer - Unclosed String at End of Program");
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * gets the tokens for each line of the file
     * @param line of file
     */
    private void getToken(String line){
        // store the characters to find longest match until we hit a break
        String longestMatch = "";

        // store the last found token as we continue for longest match
        String lastFound = "";

        // store indices of last found so we can go back
        int lastFoundStart = 0;
        int lastFoundEnd = 0;

        // loop through each character in the line to find the longest match
        for(int i = 0; i < line.length(); i++) {

            // check if we have started a new program
            if(newProgram == true){
                System.out.println();
                System.out.println("INFO  Lexer - Lexing program " + programNum + "...");
                numErrors = 0;
                newProgram = false;
            }

            longestMatch += line.charAt(i);

            // check if we are inside a comment
            if (insideComment == false) {

                // check if longest match is a keyword - update positions
                if (checkKeyword(longestMatch)) {
                    lastFound = longestMatch;
                    lastFoundEnd = i;

                    // check if we are at the end of the line
                    if (i == line.length()-1){
                        Token tok = new Token("", lastFound, currentLine, lastFoundStart+1);

                        // if we are in verbose test mode, print token
                        if(verboseTestMode)
                            System.out.println(tok.toString());

                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = lastFoundEnd+1;
                        i = lastFoundEnd;
                    }

                }
                // check if longest match is an id - update positions
                else if (checkId(longestMatch) && insideQuotes == false) {
                    lastFound = longestMatch;
                    lastFoundEnd = i;

                    // check if we are at the end of the line
                    if (i == line.length()-1){
                        Token tok = new Token("", lastFound, currentLine, lastFoundStart+1);

                        // if we are in verbose test mode, print token
                        if(verboseTestMode)
                            System.out.println(tok.toString());

                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = lastFoundEnd+1;
                        i = lastFoundEnd;
                    }
                }
                // check if longest match is a symbol or if it is stop point
                else if (checkSymbol(line.charAt(i))) {
                     if(lastFound == "" && longestMatch.length() < 2){
                        lastFound = longestMatch;
                        lastFoundStart = i;
                        lastFoundEnd = i;

                        // check if we are at the end of the line or end of program
                        if((i == line.length()-1 || lastFound.equals("$")) && insideQuotes == false){
                            Token tok = new Token("", lastFound, currentLine, lastFoundStart+1);

                            // if we are in verbose test mode, print token
                            if(verboseTestMode)
                                System.out.println(tok.toString());

                            if(lastFound.equals("$")){
                                programNum++;
                                newProgram = true;
                                System.out.println(finishedProgram());
                            }

                            longestMatch = "";
                            lastFound = "";
                            lastFoundStart = lastFoundEnd+1;
                            i = lastFoundEnd;

                        }

                        // a symbol inside of quotes should throw an error
                        else if(insideQuotes == true){
                            System.out.println("ERROR Lexer - Error: " + currentLine + ":" + (i+1) +
                                    " Unrecognized Token inside string: " + line.charAt(i));

                            longestMatch = "";
                            lastFound = "";
                            lastFoundStart = i+1;
                            lastFoundEnd = i+1;
                            numErrors++;
                        }
                    }
                    else{
                        // check if we have a two character symbol
                        if (longestMatch.equals("!=") || longestMatch.equals("==")) {
                            lastFound = longestMatch;
                            lastFoundEnd = i;

                            // check if we are at the end of the line
                            if (i == line.length()-1 && insideQuotes == false){
                                Token tok = new Token("", lastFound, currentLine, lastFoundStart+1);

                                // if we are in verbose test mode, print token
                                if(verboseTestMode)
                                    System.out.println(tok.toString());

                                longestMatch = "";
                                lastFound = "";
                                lastFoundStart = lastFoundEnd+1;
                                i = lastFoundEnd;
                            }
                        }

                         // create token as long as not in quotes and last found wasn't part of a
                         // two char symbol, which we didn't find
                        if(insideQuotes == false && !lastFound.equals("!") && !lastFound.equals("/")){
                            Token tok = new Token("", lastFound, currentLine, lastFoundStart+1);

                            // if we are in verbose test mode, print token
                            if(verboseTestMode)
                                System.out.println(tok.toString());

                            longestMatch = "";
                            lastFound = "";
                            lastFoundStart = lastFoundEnd+1;
                            i = lastFoundEnd;
                        }
                        // symbol inside quotes should throw an error
                        else if (insideQuotes == true){
                            System.out.println("ERROR Lexer - Error: " + currentLine + ":" + (i+1) +
                                    " Unrecognized Token inside string: " + line.charAt(i));

                            longestMatch = "";
                            lastFound = "";
                            lastFoundStart = i+1;
                            numErrors++;
                        }
                        // we stored ! as last found incase !=, but that wasn't found - throw error
                        else if (lastFound.equals("!")){
                            System.out.println("ERROR Lexer - Error: " + currentLine + ":" + i +
                                    " Unrecognized Token: " + lastFound);

                            longestMatch = "";
                            lastFound = "";
                            lastFoundStart = i;
                            i = lastFoundEnd;
                            numErrors++;
                        }
                        // we stored / as last found incase /*, but that wasn't found - throw error
                        else if (lastFound.equals("/")){
                            System.out.println("ERROR Lexer - Error: " + currentLine + ":" + i +
                                    " Unrecognized Token: " + lastFound);

                            longestMatch = "";
                            lastFound = "";
                            lastFoundStart = i;
                            i = lastFoundEnd;
                            numErrors++;
                        }

                    }

                }
                // check if longest match is a digit - update positions
                else if (checkDigit(line.charAt(i))) {
                    // make sure we aren't in a string because digits aren't allowed in strings
                     if(insideQuotes == false){
                         // store last found as longestMatch if we have a digit and nothing stored in lastFound
                         if(lastFound == ""){
                             lastFound = longestMatch;
                             lastFoundEnd = i;
                             if(i == line.length() - 1){
                                 Token tok = new Token("", lastFound, currentLine, lastFoundStart+1);

                                 // if we are in verbose test mode, print token
                                 if(verboseTestMode)
                                    System.out.println(tok.toString());

                                 longestMatch = "";
                                 lastFound = "";
                                 lastFoundEnd = i;
                                 lastFoundStart = lastFoundEnd+1;
                                 i = lastFoundEnd;
                             }
                         }
                         // create a token for the last found if it already exists
                         else{
                             Token tok = new Token("", lastFound, currentLine, lastFoundStart+1);

                             // if we are in verbose test mode, print token
                             if(verboseTestMode)
                                System.out.println(tok.toString());

                             longestMatch = "";
                             lastFound = "";
                             lastFoundStart = lastFoundEnd+1;
                             i = lastFoundEnd;
                         }

                     }
                     // digits aren't allowed in strings - throw error
                     else{
                         System.out.println("ERROR Lexer - Error: " + currentLine + ":" + (i+1) +
                                 " Unrecognized Token inside string: " + line.charAt(i));

                         longestMatch = "";
                         lastFound = "";
                         lastFoundStart = i+1;
                         numErrors++;
                     }

                }
                // check if we are in a string and we have a char - print token
                else if (checkChar(line.charAt(i))) {
                    Token tok = new Token("T_CHAR", longestMatch, currentLine, i+1);

                    // if we are in verbose test mode, print token
                    if(verboseTestMode)
                        System.out.println(tok.toString());

                    longestMatch = "";
                    lastFound = "";
                    lastFoundEnd = i;
                    lastFoundStart = lastFoundEnd+1;
                }

                // check if we are in a string
                else if(checkString(longestMatch) == true){
                    if(lastFound != "") {
                        // create token for last found before entering string
                        Token tok = new Token("", lastFound, currentLine, lastFoundStart+1);

                        // if we are in verbose test mode, print token
                        if(verboseTestMode)
                            System.out.println(tok.toString());

                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = lastFoundEnd+1;
                        i = lastFoundEnd;
                    }
                    else{
                        // create token for opening and closing quotes
                        if(line.charAt(i) == '\"'){
                            lastFound = "\"";
                            Token tok = new Token("T_QUOTE", lastFound, currentLine, lastFoundStart+1);

                            // if we are in verbose test mode, print token
                            if(verboseTestMode)
                                System.out.println(tok.toString());

                            longestMatch = "";
                            lastFound = "";
                            lastFoundStart = i+1;
                            lastFoundEnd = i;
                            i = lastFoundEnd;
                            if(insideQuotes == false)
                                insideQuotes = true;
                            else
                                insideQuotes = false;
                        }
                    }
                }

                // check if we are in a comment
                else if(checkComment(longestMatch) == true){
                    if(lastFound != "" && !lastFound.equals("/")) {
                        Token tok = new Token("", lastFound, currentLine, lastFoundStart+1);

                        // if we are in verbose test mode, print token
                        if(verboseTestMode)
                            System.out.println(tok.toString());

                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = lastFoundEnd+1;
                        i = lastFoundEnd;
                    }
                }

                // check if current char is whitespace
                else if (checkWhitespace(line.charAt(i))) {
                    // whitespace is a stop point - create token for last found
                    if(lastFound != ""){
                        Token tok = new Token("", lastFound, currentLine, lastFoundStart+1);

                        // if we are in verbose test mode, print token
                        if(verboseTestMode)
                            System.out.println(tok.toString());

                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = lastFoundEnd+1;
                        i = lastFoundEnd;
                    }
                    else{
                        // if we are in string, print white space token
                        if(insideQuotes == true){
                            lastFound = " ";
                            Token tok = new Token("T_CHAR", lastFound, currentLine, i+1);

                            // if we are in verbose test mode, print token
                            if(verboseTestMode)
                                System.out.println(tok.toString());

                            longestMatch = "";
                            lastFound = "";
                            lastFoundEnd = i;
                            lastFoundStart = lastFoundEnd+1;
                        }
                        // skip over white space, but update index of start if not in string
                        else{
                            longestMatch = "";
                            lastFoundStart = i+1;
                        }

                    }

                }

                // check if we are at the end of line and not inside string to create lastFound Token
                else if (i == line.length()-1 && insideQuotes == false){
                    Token tok = new Token("", lastFound, currentLine, lastFoundStart+1);

                    // if we are in verbose test mode, print token
                    if(verboseTestMode)
                        System.out.println(tok.toString());

                    longestMatch = "";
                    lastFound = "";
                    lastFoundStart = lastFoundEnd+1;
                    i = lastFoundEnd;
                }

                // check if an illegal symbol was entered in the quotes
                else if(insideQuotes == true && longestMatch != ""){
                    // check if invalid char is in string
                    if(line.charAt(i) != '\"'){
                        System.out.println("ERROR Lexer - Error: " + currentLine + ":" + (i+1) +
                                " Unrecognized Token inside string: " + line.charAt(i));
                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = i+1;
                        lastFoundEnd = i+1;
                        numErrors++;
                    }
                    else{
                        // create closing quote token
                        lastFound = "\"";
                        Token tok = new Token("T_QUOTE", lastFound, currentLine, lastFoundStart+1);

                        // if we are in verbose test mode, print token
                        if(verboseTestMode)
                            System.out.println(tok.toString());

                        longestMatch = "";
                        lastFound = "";
                        lastFoundStart = i+1;
                        lastFoundEnd = i;
                        i = lastFoundEnd;
                        if(insideQuotes == false)
                            insideQuotes = true;
                        else
                            insideQuotes = false;
                    }

                }

                // invalid character is entered in program - throw error
                else if(longestMatch != "" && lastFound == "" && !longestMatch.equals("/")){
                    System.out.println("ERROR Lexer - Error: " + currentLine + ":" + (i+1) +
                            " Unrecognized Token: " + line.charAt(i));

                    longestMatch = "";
                    lastFound = "";
                    lastFoundStart = i+1;
                    lastFoundEnd = i+1;
                    numErrors++;
                }

            }

            // inside of a comment - ignore
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
    /**
     * check if longest match is a keyword
     * @param stringToCheck longest match search string
     * @return True if longestMatch is a keyword, otherwise false
     */
    private boolean checkKeyword(String stringToCheck){
        // keywords are: while, print, if, int, string, boolean, true, false
        String regexKeyword = "(while)|(print)|(string)|(if)|(int)|(boolean)|(true)|(false)";
        boolean isKeyword = false;

        if (Pattern.matches(regexKeyword, stringToCheck)) {
            isKeyword = true;
        }
        return isKeyword;
    }

    /**
     * check if longest match is an Identifier
     * @param stringToCheck longest match search string
     * @return True if longestMatch is an Id, otherwise false
     */
    private boolean checkId(String stringToCheck){
        // an Id is a single character a-z
        String regexId = "[a-z]";
        boolean isId = false;

        if (Pattern.matches(regexId, stringToCheck)) {
            isId = true;
        }

        return isId;
    }

    /**
     * check if current character is a symbol
     * @param charToCheck current index char
     * @return True if current char is a symbol, otherwise false
     */
    private boolean checkSymbol(char charToCheck){
        // symbols allowed are {, }, !, =, +, (, ), $, /
        String regexSymbol = "[{}!=+()$/]";
        boolean isSymbol = false;

        if (Pattern.matches(regexSymbol, Character.toString(charToCheck))) {
            isSymbol = true;
        }

        return isSymbol;
    }

    /**
     * check if current character is a digit
     * @param charToCheck current index char
     * @return True if current char is a digit, otherwise false
     */
    private boolean checkDigit(char charToCheck){
        // a digit is a single number 0-9
        String regexInteger = "[0-9]";
        boolean isDigit = false;

        if (Pattern.matches(regexInteger, Character.toString(charToCheck))) {
            isDigit = true;
        }

        return isDigit;
    }

    /**
     * check if current character is a char
     * @param charToCheck current index char
     * @return True if current character is a char, otherwise false
     */
    private boolean checkChar(char charToCheck){
        // an char is a single character a-z that is inside a quote
        String regexCharacter = "[a-z]";
        boolean isChar = false;

        if (Pattern.matches(regexCharacter, Character.toString(charToCheck)) && insideQuotes == true) {
            isChar = true;
        }

        return isChar;
    }

    /**
     * check if current character is whitespace
     * @param charToCheck current index char
     * @return True if current char is a space, otherwise false
     */
    private boolean checkWhitespace(char charToCheck){
        boolean isWhitespace = false;
        String regexWhitespace = "\\s";
        if (Pattern.matches(regexWhitespace, Character.toString(charToCheck))) {
            isWhitespace = true;
        }
        return isWhitespace;
    }

    /**
     * check if longest match is a comment two character symbol
     * @param stringToCheck longest match string
     * @return True if longestMatch begins or ends a comment, otherwise false
     */
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

    /**
     * check if longest match is beginning a string
     * @param stringToCheck longest match string
     * @return True if longestMatch contains quotes to start/end string, otherwise false
     */
    private boolean checkString(String stringToCheck){
        boolean isString = false;

        if (stringToCheck.contains("\"")) {
            isString = true;
        }

        return isString;
    }

    /**
     * print out the program complete/fail messages
     * @return String with Lex complete or fail message with numErrors
     */
    private String finishedProgram(){
        if(numErrors == 0)
            return "INFO  Lexer - Lex completed with " + numErrors + " errors";
        else
            return "ERROR Lexer - Lex failed with " + numErrors + " error(s)";
    }

}
