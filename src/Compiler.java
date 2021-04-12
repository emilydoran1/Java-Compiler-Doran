import java.util.Scanner;

/**
 * This program will be the entry point for the compiler.
 * If a file is passed in, we call the Lexer to analyze the grammar
 *
 * @author Emily Doran
 *
 */

public class Compiler {

    public static void main (String[] args) throws Exception {

        // check if a paramater is entered to read the file contents
        if(args.length == 1){
            Lexer lex = new Lexer(args[0], true);
        } else{
            System.out.println("To Run the compiler, Enter the File as a Command Line Parameter\n " +
                    "i.e. \"java Compiler testCases.txt\"");
        }
    }
}
