/*
 *  Copyright (c) 2004-2024 QOS.ch
 *  All rights reserved.
 *
 *  Permission is hereby granted, free  of charge, to any person obtaining
 *  a  copy  of this  software  and  associated  documentation files  (the
 *  "Software"), to  deal in  the Software without  restriction, including
 *  without limitation  the rights to  use, copy, modify,  merge, publish,
 *  distribute,  sublicense, and/or sell  copies of  the Software,  and to
 *  permit persons to whom the Software  is furnished to do so, subject to
 *  the following conditions:
 *
 *  The  above  copyright  notice  and  this permission  notice  shall  be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 *  EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 *  MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ch.qos.logback.tyler.base.generated;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.OnConsoleStatusListener;

import java.lang.Override;

public class SmokeTylerConfigurator extends TylerConfiguratorBase implements Configurator {
    /**
     * <p>This method performs configuration per {@link Configurator} interface.</p>
     *
     * <p>If <code>TylerConfgiurator</code> is installed as a configurator service, this
     * method will be called by logback-classic during initialization.</p>
     */
    @Override
    public Configurator.ExecutionStatus configure(LoggerContext loggerContext) {
        setContext(loggerContext);
        setupOnConsoleStatusListener();
        propertyModelHandlerHelper.handlePropertyModel(this, "USER_HOME", "/home/alice", "", "", "");
        setContextName(subst("${APPNAME}"));
        Appender appenderRFILE = setupAppenderRFILE();
        Logger logger_com_foo_Bar = setupLogger("com.foo.Bar", "DEBUG", null);
        Logger logger_ROOT = setupLogger("ROOT", "DEBUG", null);
        logger_ROOT.addAppender(appenderRFILE);
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

    void setupOnConsoleStatusListener() {
        OnConsoleStatusListener onConsoleStatusListener = new OnConsoleStatusListener();
        onConsoleStatusListener.setContext(context);
        boolean effectivelyAdded = context.getStatusManager().add(onConsoleStatusListener);
        onConsoleStatusListener.setPrefix("moo");

        if (effectivelyAdded && (onConsoleStatusListener instanceof LifeCycle)) {
            ((LifeCycle) onConsoleStatusListener).start();
        }
    }

    Appender setupAppenderRFILE() {
        RollingFileAppender appenderRFILE = new RollingFileAppender();
        appenderRFILE.setContext(context);
        appenderRFILE.setName("RFILE");
        appenderRFILE.setFile(subst("${USER_HOME}/logFile.log"));

        // Configure component of type TimeBasedRollingPolicy
        TimeBasedRollingPolicy timeBasedRollingPolicy = new TimeBasedRollingPolicy();
        timeBasedRollingPolicy.setContext(context);
        timeBasedRollingPolicy.setFileNamePattern("logFile.%d{yyyy-MM-dd}.log");
        timeBasedRollingPolicy.setMaxHistory(30);
        timeBasedRollingPolicy.setTotalSizeCap(ch.qos.logback.core.util.FileSize.valueOf("3GB"));
        timeBasedRollingPolicy.setParent(appenderRFILE);
        timeBasedRollingPolicy.start();

        // Inject component of type TimeBasedRollingPolicy into parent
        appenderRFILE.setRollingPolicy(timeBasedRollingPolicy);

        // Configure component of type PatternLayoutEncoder
        PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
        patternLayoutEncoder.setContext(context);
        patternLayoutEncoder.setPattern("%-4relative [%thread] %-5level %logger{35} -%kvp- %msg%n");
        patternLayoutEncoder.setParent(appenderRFILE);
        patternLayoutEncoder.start();

        // Inject component of type PatternLayoutEncoder into parent
        appenderRFILE.setEncoder(patternLayoutEncoder);

        appenderRFILE.start();
        return appenderRFILE;
    }
}

