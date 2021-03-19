/**
 *
 * @author Emily Doran
 *
 */

import java.util.List;

public class Parser {

    public Parser(List<Token> tokens, boolean verboseMode, boolean passLex, int programNum) {
        if(passLex){
            System.out.println("\nPARSER: Parsing program " + programNum + " ...");
        }
        else{
            System.out.println("\nPARSER: Skipped due to LEXER error(s)");
        }
    }
}
