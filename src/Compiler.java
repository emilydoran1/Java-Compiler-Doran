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

            // prompt user if they would like to run in verbose test mode
            System.out.println("Would You Like to Run the Lexer in Verbose Test Mode? (Y/N)");
            Scanner scan = new Scanner(System.in);

            String verboseTestMode = scan.nextLine();

            boolean verbose = false;

            if(verboseTestMode.equalsIgnoreCase("Y"))
                verbose = true;

            Lexer lex = new Lexer(args[0], verbose);
        }
        else{
            System.out.println("To Run the Lexer, Enter the File as a Command Line Parameter\n " +
                    "i.e. \"java Compiler testCases.txt\"");
        }

    }
}
