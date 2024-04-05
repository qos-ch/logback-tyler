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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.StatusPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.spi.MDCAdapter;

import java.util.Deque;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class TylerConfiguratorTest {

    LoggerContext loggerContext = new LoggerContext();
    MDCAdapter mdcAdapter = new LogbackMDCAdapter();

    StatusPrinter statusPrinter = new StatusPrinter();

    @BeforeEach
    public void setUp() {
        loggerContext.setMDCAdapter(mdcAdapter);
    }

    @Test
    public void smoke() {

        long start = System.currentTimeMillis();
        TylerConfigurator tc = new TylerConfigurator();
        tc.configure(loggerContext);
        long end = System.currentTimeMillis();
        long diff = end - start;
        System.out.println("time " + diff);

        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

        RollingFileAppender rollingFileAppender = (RollingFileAppender) rootLogger.getAppender("RFILE");
        TimeBasedRollingPolicy timeBasedRollingPolicy = (TimeBasedRollingPolicy) rollingFileAppender.getRollingPolicy();

        TimeBasedFileNamingAndTriggeringPolicy tbfnatp = timeBasedRollingPolicy.getTimeBasedFileNamingAndTriggeringPolicy();

        Logger logger = loggerContext.getLogger(this.getClass());
        long now = System.currentTimeMillis();

        long MILLIS_IN_DAY=24*3_600_000;

        for (int i = 0; i < 10; i++) {
            tbfnatp.setCurrentTime(now+(i*MILLIS_IN_DAY));
            logger.info("hello world " + i);
            delay(1000);
        }


        assertNotNull(rollingFileAppender);
        assertTrue(rollingFileAppender.isStarted());
    }

    private void delay(int duration) {
        try {
            Thread.currentThread().sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void joran() throws JoranException {

        //        {
        //            JoranConfigurator jc = new JoranConfigurator();
        //            jc.setContext(loggerContext);
        //            jc.doConfigure("src/test/resources/logback-two.xml");
        //        }
        //        loggerContext.reset();

        {
            long start = System.currentTimeMillis();
            JoranConfigurator jc = new JoranConfigurator();
            jc.setContext(loggerContext);
            jc.doConfigure("src/test/resources/logback-two.xml");

            long end = System.currentTimeMillis();
            long diff = end - start;
            System.out.println("time " + diff);
        }
    }
}
