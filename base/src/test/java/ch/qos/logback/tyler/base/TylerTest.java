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
import ch.qos.logback.core.util.ContextUtil;
import ch.qos.logback.core.util.StatusPrinter;
import org.junit.jupiter.api.Test;

public class TylerTest {

    ContextBase context = new ContextBase();
    @Test
    void smoke() throws JoranException {
        ModelToJava m2j = new ModelToJava(context);
        String input = """
                <configuration>
                  <import class="ch.qos.logback.classic.encoder.PatternLayoutEncoder"/>
                  <import class="ch.qos.logback.core.ConsoleAppender"/>
                                
                </configuration>                
                """;

        Model model = m2j.extractModel(input);
        String result = m2j.toJava(model);

        StatusPrinter statusPrinter = new StatusPrinter();
        statusPrinter.print(context);
        System.out.println(result);
    }
}
