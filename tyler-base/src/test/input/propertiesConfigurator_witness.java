package com.example;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.model.PropertiesConfiguratorModel;
import ch.qos.logback.classic.model.processor.PropertiesConfiguratorModelHandler;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.testUtil.StringListAppender;


/**
 *
 * <p>This file was generated by logback-tyler version 0.8-SNAPSHOT</p>
 *
 * <p>Eventual errors and warnings are appended at the end.</p>
 *
 * <p>You may experiment with logback.xml to Java translation, i.e.
 * TylerConfigurator generation, at the following URL:
 *
 *       https://logback.qos.ch/translator/services/xml2Java.html </p>
 *
 * <p>This generated TylerConfigurator class is intended to be copied and integrated
 * into the user's project as a custom configurator. It will configure logback
 * without XML.</p>
 *
 * <p>It requires logback-classic version 1.5.10 or later at runtime.</p>
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
public class TylerConfigurator extends TylerConfiguratorBase implements Configurator {
    /**
     * Appender variable referencing the appender named "LIST".
     */
    protected StringListAppender appenderLIST;

    /**
     * <p>This method performs configuration per {@link Configurator} interface.</p>
     *
     * <p>If <code>TylerConfigurator</code> is installed as a configurator service, this
     * method will be called by logback-classic during initialization.</p>
     */
    @Override
    public Configurator.ExecutionStatus configure(LoggerContext loggerContext) {
        setContext(loggerContext);
        this.appenderLIST = setupAppenderLIST();
        propertyModelHandlerHelper.handlePropertyModel(this, "JO_PREFIX", "src/test/input/joran", "", "", "");
        PropertiesConfiguratorModel propertyConfiguratorModel = new PropertiesConfiguratorModel();
        propertyConfiguratorModel.setFile(subst("${JO_PREFIX}/propertiesConfigurator/smoke.properties"));
        PropertiesConfiguratorModelHandler propertiesConfiguratorModelHandler = new PropertiesConfiguratorModelHandler(context);
        try {
            propertiesConfiguratorModelHandler.detachedHandle(this, propertyConfiguratorModel);
        } catch(ModelHandlerException e) {
            addError("Failed to process PropertyConfiguratorModel", e);
        }
        Logger logger_ROOT = setupLogger("ROOT", "debug", null);
        logger_ROOT.addAppender(appenderLIST);
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

    StringListAppender setupAppenderLIST() {
        StringListAppender appender = new StringListAppender();
        appender.setContext(context);
        appender.setName("LIST");

        // Configure component of type PatternLayout
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(context);
        patternLayout.setPattern("%msg");
        // ===========no parent setter
        patternLayout.start();
        // Inject component of type PatternLayout into parent
        appender.setLayout(patternLayout);

        appender.start();
        return appender;
    }
}
// 11:56:09,016 |-INFO in ch.qos.logback.core.model.processor.DefaultProcessor@44be0077 - End of configuration.
