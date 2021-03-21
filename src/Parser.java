/**
 *
 * @author Emily Doran
 *
 */

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private ArrayList<Token> tokens;
    private boolean verboseMode;
    private Token currentToken;
    private int tokIndex = 0;

    private ConcreteSyntaxTree cst;

    int errorCount = 0;

    public Parser(ArrayList<Token> tokens, boolean verboseMode, boolean passLex, int programNum) {
        this.tokens = tokens;
        this.verboseMode = verboseMode;

        if(passLex){
            System.out.println("\nPARSER: Parsing program " + programNum + " ...");
            currentToken = tokens.get(tokIndex);
            parse();
            System.out.println("PARSER: Parse completed successfully");
        }
        else{
            System.out.println("\nPARSER: Skipped due to LEXER error(s)");
        }

    }

    public void parse(){
        Token tok = currentToken;
        System.out.println("PARSER: parse()");

        parseProgram();

        // output error count
        System.out.println("Parsing found " + errorCount + " error(s).");
    }

    public void parseProgram(){
        System.out.println("PARSER: parseProgram()");
        parseBlock();
    }

    public void parseBlock(){
        System.out.println("PARSER: parseBlock()");
        checkToken("T_L_BRACE");
        parseStatementList();
        checkToken("T_R_BRACE");
    }

    public void parseStatementList(){
        System.out.println("PARSER: parseStatementList()");
        parseStatement();
        parseStatementList();

    }

    public void parseStatement(){
        System.out.println("PARSER: parseStatement()");

        if(checkToken("T_PRINT"))
            parsePrintStatement();
        else if(checkToken("T_ID"))
            parseAssignStatement();
        else if(checkToken("T_VARIABLE_TYPE"))
            parseVarDecl();
        else if(checkToken("T_WHILE"))
            parseWhileStatement();
        else if(checkToken("T_IF"))
            parseIfStatement();
        else
            parseBlock();
    }

    public void parsePrintStatement(){
        System.out.println("PARSER: parsePrintStatement()");
        checkToken("T_L_PAREN");
        parseExpr();
        checkToken("T_R_PAREN");
    }

    public void parseAssignStatement(){
        System.out.println("PARSER: parseAssignStatement()");
        checkToken("T_ASSIGN_OP");
        parseExpr();
    }

    public void parseVarDecl(){
        System.out.println("PARSER: parseVarDecl()");
        checkToken("T_ID");
    }

    public void parseWhileStatement(){
        System.out.println("PARSER: parseWhileStatement()");
        parseBooleanExpr();
        parseBlock();
    }

    public void parseIfStatement(){
        System.out.println("PARSER: parseIfStatement()");
        parseBooleanExpr();
        parseBlock();
    }

    public void parseExpr(){
        System.out.println("PARSER: parseExpr()");
        if(checkToken("T_DIGIT"))
            parseIntExpr();
        else if(checkToken("T_QUOTE"))
            parseStringExpr();
        else if(checkToken("T_L_PAREN"))
            parseBooleanExpr();

        // TODO: what do I need to add for these two?
        else if(checkToken("T_BOOL_TRUE") || checkToken("T_BOOL_FALSE")){}

        else if(checkToken("T_ID")){}
    }

    public void parseIntExpr(){
        System.out.println("PARSER: parseIntExpr()");
        checkToken("T_ADDITION_OP");
        parseExpr();
    }

    public void parseStringExpr(){
        System.out.println("PARSER: parseStringExpr()");
        parseCharList();
        checkToken("T_QUOTE");
    }

    public void parseBooleanExpr(){
        System.out.println("PARSER: parseBooleanExpr()");
        checkToken("T_L_PAREN");
        parseExpr();
        checkToken("T_BOOL_TRUE");
        checkToken("T_BOOL_FALSE");
        parseExpr();
    }

    public void parseCharList(){
        System.out.println("PARSER: parseCharList()");
        checkToken("T_CHAR");
        parseCharList();
    }

    public boolean checkToken(String expectedKind){
        boolean tokenMatch = false;

        if (tokens.size() <= tokIndex) {
            System.out.println("PARSER: ERROR: Expected [" + expectedKind + "] got end of stream.");
        }

        else{
            if(tokens.get(tokIndex).getKind().equals(expectedKind)) {
                tokenMatch = true;
                tokIndex++;
            }
            else{
                // TODO: throw error
            }
        }

        return tokenMatch;
    }

}
