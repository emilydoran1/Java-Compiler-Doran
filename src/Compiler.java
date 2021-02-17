import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * This program will be the entry point for the compiler
 * @author Emily Doran
 *
 */

public class Compiler {

    public static void main (String[] args) throws Exception {
        System.out.println("Welcome to my compilers repository!");

        // check if a paramater is entered to read the file contents
        if(args.length > 0){
            File file = new File(args[0]);
            BufferedReader reader = new BufferedReader(new FileReader(file));

            try{
                while (reader.ready()) {
                    System.out.println(reader.readLine());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }
}
