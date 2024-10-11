package com.example;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.model.processor.ConfigurationModelHandlerFull;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
import java.lang.Override;

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
 * <p>BEWARE: As of April 2024, TylerConfigurator generation from logback.xml
 * configuration files remains experimental and incomplete.
 * </p>
 *
 * <p>This generated TylerConfigurator class is intended to be copied and integrated
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
   * <p>If <code>TylerConfigurator</code> is installed as a configurator service, this
   * method will be called by logback-classic during initialization.</p>
   */
  @Override
  public Configurator.ExecutionStatus configure(LoggerContext loggerContext) {
    setContext(loggerContext);
    ConfigurationModelHandlerFull configurationMHF = new ConfigurationModelHandlerFull(context);
    configurationMHF.detachedPostProcess(subst("true"), subst(null));
    return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
  }
}
// 18:48:44,444 |-INFO in ch.qos.logback.core.model.processor.DefaultProcessor@1d8062d2 - End of configuration.