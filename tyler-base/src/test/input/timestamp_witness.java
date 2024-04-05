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

package com.example;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.action.ActionUtil;
import ch.qos.logback.core.util.CachingDateFormatter;
import java.lang.Override;

/**
 *
 * <p>This file was generated by logback-tyler version 0.7-SNAPSHOT</p>
 *
 * <p>Eventual errors and warning are appended at the end.</p>
 *
 * <p>You can try, TylerConfigurator generation, that is the logback.xml
 * to Java translation at
 *
 *       https://logback.qos.ch/translator/services/xml2Java.html </p>
 *
 * <p>BEWARE: As of March 2024, TylerConfigurator generation from logback.xml
 * configuration files is still experimental and incomplete.
 * </p>
 *
 * <p>This class, i.e. TylerConfigurator, is intended to be copied and integrated
 * into the user's project as a custom configurator. It will configure logback
 * without XML.</p>
 *
 * <p>It requires logback-classic version 1.5.4 or later at runtime.</p>
 *
 * <p>Custom configurators are looked up via Java's service-provide facility. If a
 * custom provider is found, it takes precedence over logback's own configurators,
 * e.g. DefaultJoranConfigurator.</p>
 *
 * <p>See also item 1 of 'Configuration at initialization' section at
 *
 *     https://logback.qos.ch/manual/configuration.html#auto_configuration
 *
 * </p>
 */
class TylerConfigurator extends TylerConfiguratorBase implements Configurator {
    /**
     * <p>This method performs configuration per {@link Configurator} interface.</p>
     *
     * <p>If <code>TylerConfgiurator</code> is installed as a configurator service, this
     * method will be called by logback-classic during initialization.</p>
     */
    @Override
    public Configurator.ExecutionStatus configure(LoggerContext loggerCoontext) {
        setContext(loggerCoontext);
        timestamp_bySecond();
        Appender appenderFILE = setupAppenderFILE();
        Logger logger_ROOT = setupLogger("ROOT", "DEBUG", null);
        logger_ROOT.addAppender(appenderFILE);
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

    void timestamp_bySecond() {
        ActionUtil.Scope scope = ActionUtil.stringToScope("");
        CachingDateFormatter cdf = new CachingDateFormatter("yyyyMMdd'T'HHmmss");
        long timeReference = System.currentTimeMillis();
        addInfo("Using current interpretation time, i.e. now, as time reference.");
        String timeValue = cdf.format(timeReference);
        addInfo("Adding property to the context with key='"+"bySecond"+"' and value="+timeValue+" to the "+scope+" scope");
        ActionUtil.setProperty(this, "bySecond", timeValue, scope);
    }

    Appender setupAppenderFILE() {
        FileAppender appenderFILE = new FileAppender();
        appenderFILE.setContext(context);
        appenderFILE.setName("FILE");
        appenderFILE.setFile(subst("log-${bySecond}.txt"));

        // Configure component of type PatternLayoutEncoder
        PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
        patternLayoutEncoder.setContext(context);
        patternLayoutEncoder.setPattern("%logger{35} -%kvp- %msg%n");
        patternLayoutEncoder.setParent(appenderFILE);
        patternLayoutEncoder.start();

        // Inject component of type PatternLayoutEncoder into parent
        appenderFILE.setEncoder(patternLayoutEncoder);

        appenderFILE.start();
        return appenderFILE;
    }
}
// 18:10:06,175 |-INFO in ch.qos.logback.tyler.base.handler.ImplicitModelHandler - Assuming default type [ch.qos.logback.classic.encoder.PatternLayoutEncoder] for [encoder] property
// 18:10:06,180 |-INFO in ch.qos.logback.core.model.processor.DefaultProcessor@64ec96c6 - End of configuration.
