/*
 * Copyright (c) 2024 QOS.ch Sarl (Switzerland)
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 *
 */

package ch.qos.logback.tyler.base;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.util.StatusPrinter2;
import ch.qos.logback.tyler.base.antlr4.SyntaxVerifier;
import ch.qos.logback.tyler.base.antlr4.TylerAntlr4ErrorListener;
import ch.qos.logback.tyler.base.util.StringPrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ch.qos.logback.tyler.base.TylerTestContants.INPUT_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TylerRegressionTest {

    ContextBase context = new ContextBase();
    SyntaxVerifier syntaxVerifier = new SyntaxVerifier();
    ModelToJava m2j = new ModelToJava(context);
    OutputComparator outputComparator = new OutputComparator();
    StatusPrinter2 statusPrinter2 = new StatusPrinter2();
    StringPrintStream sps = new StringPrintStream(System.out, false);

    @BeforeEach
    public void setUp() {
        statusPrinter2.setPrintStream(sps);
    }
    @Test
    void smoke() throws JoranException, IOException {
        verify(INPUT_PREFIX+"smoke.xml", INPUT_PREFIX+"smoke_witness.java", false);
    }

    void verify(String path2XMLFile, String path2WitnessFile, boolean dumpResult) throws JoranException, IOException {
        List<String> lines = readFile(path2XMLFile);
        List<String> witnessLines = readFile(path2WitnessFile);
        StringBuffer buf = new StringBuffer();
        lines.forEach(l -> buf.append(l).append("\n"));

        Model model = m2j.extractModel(buf.toString());
        String result = m2j.toJava(model);
        String[] resultArray = result.split("\n");
        List<String> resultList = new ArrayList<>(List.of(resultArray));


        // capture status messages into sps.stringList
        statusPrinter2.print(context);

        appendStatusLinesToResultsList(sps.stringList, resultList);

        // uncomment to see filtered output
         resultList.forEach(System.out::println);

        assertTrue(outputComparator.checkForEquality(witnessLines, resultList));

        TylerAntlr4ErrorListener errorListener = syntaxVerifier.verify(result);
        assertEquals(0, errorListener.getSyntaxErrorCount(), errorListener.getErrorMessages().toString());
    }

    private void appendStatusLinesToResultsList(List<String> stringList, List<String> resultList) {

        for(String s: stringList) {
            StringBuilder sb = new StringBuilder();
            String[] split = s.split("\n");
            Arrays.stream(split).forEach(n -> sb.append("// ").append(n).append("\n"));
            resultList.add(sb.toString());
        }
    }

    private List<String> readFile(String pathStr) throws IOException {
        Path  path = Path.of(pathStr);
        List<String> lines = Files.readAllLines(path);
        return lines;
    }
}