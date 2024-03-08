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
import ch.qos.logback.core.util.StatusPrinter;
import ch.qos.logback.core.util.StatusPrinter2;
import ch.qos.logback.tyler.base.antlr4.SyntaxVerifier;
import ch.qos.logback.tyler.base.antlr4.TylerAntlr4ErrorListener;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TylerTest {

    ContextBase context = new ContextBase();
    SyntaxVerifier syntaxVerifier = new SyntaxVerifier();
    StatusPrinter2 statusPrinter2 = new StatusPrinter2();

    @Test
    void smoke() throws JoranException, IOException {
        ModelToJava m2j = new ModelToJava(context);
        String input = """
                <configuration xdebug="true">
                  <import class="ch.qos.logback.classic.encoder.PatternLayoutEncoder"/>
                  <import class="ch.qos.logback.core.ConsoleAppender"/>
                  <import class="ch.qos.logback.core.rolling.RollingFileAppender"/>
                  <import class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy"/>
                  <import class="ch.qos.logback.core.status.OnConsoleStatusListener"/>
                  
                  <shutdownHook/>
                  <statusListener class="OnConsoleStatusListener">
                     <prefix>moo</prefix>
                  </statusListener>
                    
                  <property name="USER_HOME" value="/home/alice"/>
                  
                  <contextName>${APPNAME}</contextName>
                       
                  <appender name="RFILE" class="RollingFileAppender">
                     <file>${USER_HOME}/logFile.log</file>
                     <rollingPolicy class="TimeBasedRollingPolicy">
                       <fileNamePattern>logFile.%d{yyyy-MM-dd}.log</fileNamePattern>
                       <maxHistory>30</maxHistory>
                       <totalSizeCap>3GB</totalSizeCap>
                     </rollingPolicy>
                     <encoder class="PatternLayoutEncoder">
                       <pattern>%-4relative [%thread] %-5level %logger{35} -%kvp- %msg%n</pattern>
                     </encoder>
                  </appender>
                                
                  <logger name="com.foo.Bar" level="DEBUG">
                     <appender-ref ref="RFILE"/>
                  </logger>  
                  
                   <root level="DEBUG">
                     <appender-ref ref="toto"/>
                     <appender-ref ref="RFILE"/>
                  </root>              
                </configuration>                
                """;

        Model model = m2j.extractModel(input);
        String result = m2j.toJava(model);

        TylerAntlr4ErrorListener errorListener = syntaxVerifier.verify(result);

        statusPrinter2.print(context);
        System.out.println("----------------");
        System.out.println(result);
        System.out.println("----------------");

        assertEquals(0, errorListener.getSyntaxErrorCount(), errorListener.getErrorMessages().toString());

    }

    @Test
    void example() throws IOException, JoranException {
        String xmlInput = """
                <configuration debug="true">
                  <import class="ch.qos.logback.classic.encoder.PatternLayoutEncoder"/>
                  <import class="ch.qos.logback.core.ConsoleAppender"/>
                  <import class="ch.qos.logback.core.rolling.RollingFileAppender"/>
                  <import class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy"/>
                  
                  <property name="APP_NAME" value="myApp"/>
                  
                  <contextName>${APP_NAME}</contextName>
                  
                  <appender class="ch.qos.logback.core.FileAppender" name="RFILE">
                     <file>toto.log</file>
                     <append>true</append>
                     <immediateFlush>true</immediateFlush>
                     <encoder>
                       <pattern>%-4relative [%thread] %-5level %logger{35} -%kvp- %msg%n</pattern>
                     </encoder>
                       
                  </appender>          
                       
                  
                   <root level="DEBUG">
                     <appender-ref ref="RFILE"/>
                  </root>              
                </configuration>                
                """;

        ModelToJava m2j = new ModelToJava(context);
        Model model = m2j.extractModel(xmlInput);
        String result = m2j.toJava(model);
        System.out.println("----------------");
        System.out.println(result);
        System.out.println("----------------");
        statusPrinter2.print(context);
    }
}
