package com.example;

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
class TylerConfigurator extends TylerConfiguratorBase implements Configurator {
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

    if(effectivelyAdded && (onConsoleStatusListener instanceof LifeCycle)) {
      ((LifeCycle)onConsoleStatusListener).start();
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
// 21:32:27,561 |-INFO in ch.qos.logback.core.model.processor.DefaultProcessor@5f20155b - End of configuration.
