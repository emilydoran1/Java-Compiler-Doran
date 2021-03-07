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
        if(args.length > 0){

            // prompt user if they would like to run in verbose test mode
            System.out.println("Would you like to run in verbose test mode? (Y/N)");
            Scanner scan = new Scanner(System.in);

            String verboseTestMode = scan.nextLine();

            boolean verbose = false;

            if(verboseTestMode.equalsIgnoreCase("Y"))
                verbose = true;

            Lexer lex = new Lexer(args[0], verbose);
        }

    }
}
