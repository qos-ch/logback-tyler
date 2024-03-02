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

import java.io.IOException;

public class TylerExample {

    static String xmlInput =
    """
            <configuration debug="true">
              <import class="ch.qos.logback.classic.encoder.PatternLayoutEncoder"/>
              
              <property name="APP_NAME" value="myApp"/>
             
              <contextName>${APP_NAME}</contextName>
             
              <appender class="ch.qos.logback.core.FileAppender" name="TOTO">
                <file>toto.log</file>
                <append>true</append>
                <immediateFlush>true</immediateFlush>
                <encoder>
                  <pattern>%-4relative [%thread] %-5level %logger{35} -%kvp- %msg%n</pattern>
                </encoder>   
              </appender>         
             
             
              <root level="DEBUG">
                <appender-ref ref="TOTO"/>
              </root>             
            </configuration>                 
    """;

    public static void main(String[] args)  throws JoranException, IOException {
        ContextBase context = new ContextBase();
        ModelToJava m2j = new ModelToJava(context);
        Model model = m2j.extractModel(xmlInput);
        String result = m2j.toJava(model);
        System.out.println(result);
    }
}
