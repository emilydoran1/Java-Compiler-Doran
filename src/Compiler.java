/**
 * This program will be the entry point for the compiler
 * @author Emily Doran
 *
 */

public class Compiler {

    public static void main (String[] args) throws Exception {
//        System.out.println("Welcome to my compilers repository!");

        // check if a paramater is entered to read the file contents
        if(args.length > 0){

            Lexer lex = new Lexer(args[0]);
        }


    }
}
