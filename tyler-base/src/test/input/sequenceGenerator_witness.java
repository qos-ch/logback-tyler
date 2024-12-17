package com.example;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
import ch.qos.logback.core.spi.BasicSequenceNumberGenerator;
import ch.qos.logback.core.spi.LifeCycle;
import java.lang.Override;

/**
 *
 * <p>This file was generated by logback-tyler version 0.5-SNAPSHOT</p>
 *
 * <p>You can try it online at
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
 * <p>It requires logback-classic version 1.5.2 or later at runtime.</p>
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
     * <p>This method performs configuration per {@link Configurator} interface.</p>
     *
     * <p>If <code>TylerConfgiurator</code> is installed as a configurator service, this
     * method will be called by logback-classic during initialization.</p>
     */
    @Override
    public Configurator.ExecutionStatus configure(LoggerContext loggerContext) {
        setContext(loggerContext);
        setupBasicSequenceNumberGenerator();
        Logger logger_ROOT = setupLogger("ROOT", "DEBUG", null);
        logger_ROOT.addAppender(appenderRFILE);
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

    void setupBasicSequenceNumberGenerator() {
        BasicSequenceNumberGenerator basicSequenceNumberGenerator = new BasicSequenceNumberGenerator();
        basicSequenceNumberGenerator.setContext(context);

        if(basicSequenceNumberGenerator instanceof LifeCycle) {
            ((LifeCycle)basicSequenceNumberGenerator).start();
        }
    }
}
// 11:42:18,534 |-INFO in ch.qos.logback.tyler.base.handler.SequenceNumberGeneratorModelHandler - About to configure SequenceNumberGenerator of type [ch.qos.logback.core.spi.BasicSequenceNumberGenerator]
// 11:42:18,544 |-INFO in ch.qos.logback.core.model.processor.DefaultProcessor@7dc3712 - End of configuration.