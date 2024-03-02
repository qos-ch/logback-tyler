
package ch.qos.logback.tyler.base.antlr4;


//import org.antlr.runtime.*;
import org.antlr.v4.runtime.*;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        ANTLRInputStream input = new ANTLRFileStream(args[0]);
        JavaLexer lexer = new JavaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);

        final StringBuilder errorMessages = new StringBuilder();
        parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                        int line, int charPositionInLine, String msg,
                                        RecognitionException e) {
            String err = String.format("Failed to parse at line %d:%d due to %s", line,
                                       charPositionInLine + 1, msg);
            errorMessages.append(err);
            errorMessages.append(System.lineSeparator());
        }
    });

        parser.compilationUnit();
        int syntaxErrors = parser.getNumberOfSyntaxErrors();

        if (syntaxErrors == 0) {
            System.out.println(args[0] + ": PASS");
        } else {
            System.out.println(args[0] + ": FAILED (" + syntaxErrors + " syntax errors");
        }
    }

}
