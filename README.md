# logback-tyler

Logback-tyler translates logback-classic XML configuration files to Java.

The resulting java class named `TylerConfigurator` implements the
[`Configurator`](https://logback.qos.ch/xref/ch/qos/logback/classic/spi/Configurator.html)
interface. It can thus be declared as a custom configuration provider
using Java's standard
[service-provider](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html)
meachanism. Custom configurators are searched by looking up file
resource located under the
_META-INF/services/ch.qos.logback.classic.spi.Configurator_ file in
your project.

Running `TylerConfigurator` does not require XML parsers and usually
executes much faster than `JoranConfigurator`, logback's XML
configurator. Moreover, `TylerConfigurator` does not use
reflection. In addition, given that it ships with your project's
binaries, it is harder to modify and offers yet a smaller attack
surface.

At runtime, `TylerConfigurator` does not have any additional
dependencies other than logback-classic version 1.5.2. 

Logback-tyler is located at the following Maven coordinates:
```xml
 <groupId>ch.qos.logback.tyler</groupId>
 <artifactId>tyler-base</artifactId>
 <version>0.4</version>
```

Here is a sample program to translate a logback.xml as string into Java.
  
```java
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;

import java.io.IOException;

public class TylerExample {

    String xmlInput =
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
```

running the above program will output

```java

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.NoAutoStartUtil;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.OptionHelper;
import java.lang.Exception;
import java.lang.Override;

/**
 *  <p>BEWARE: As of March 2024, TylerConfigurator generation from logback.xml configuration files is
 * still experimental and incomplete.
 * <p>
 *
 * <p>This class, i.e. TylerConfigurator, is intended to be copied and integrated into the user's
 * project as custom configurator. It will configure logback without XML.</p>
 *
 * <p>It requires logback-classic version 1.5.2 or later at runtime.</p>
 *
 * <p>Custom configurators are looked up via Java's service-provide facility. If a custom provider is
 * found, it takes precedence over logback's own configurators, e.g. DefaultJoranConfigurator.</p>
 *
 * <p>See also item 1 of 'Configuration at initialization' section at
 * "https://logback.qos.ch/manual/configuration.html#auto_configuration </p>
 */
class TylerConfigurator extends TylerConfiguratorBase implements Configurator {
    /**
     * <p>This method performs configuration per {@link Configurator} interface.</p>
     *
     * <p>If <code>TylerConfgiurator</code> is installed as a configurator service, this method will be
     * called by logback-classic during initialization.</p>
     */
    @Override
    public Configurator.ExecutionStatus configure(LoggerContext loggerCoontext) {
        setContext(loggerCoontext);
        addOnConsoleStatusListener();
        propertyModelHandlerHelper.handlePropertyModel(this, "APP_NAME", "myApp", "", "", "");
        setContextName("${APP_NAME}");
        Appender appenderTOTO = setupAppenderTOTO();
        Logger logger_ROOT = setupLogger("ROOT", "DEBUG", null);
        logger_ROOT.addAppender(appenderTOTO);
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

    Appender setupAppenderTOTO() {
        FileAppender appenderTOTO;
        try {
            appenderTOTO = (FileAppender) OptionHelper.instantiateByClassName("ch.qos.logback.core.FileAppender", Appender.class, context);
        } catch (Exception oops) {
            addError("Could not create an Appender of type [" + "ch.qos.logback.core.FileAppender" + "].", oops);
            return null;
        }
        appenderTOTO.setContext(context);
        appenderTOTO.setName("TOTO");
        appenderTOTO.setFile("toto.log");
        appenderTOTO.setAppend(true);
        appenderTOTO.setImmediateFlush(true);

        // Configure component of type PatternLayoutEncoder
        PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
        if (patternLayoutEncoder instanceof ContextAware) {
            patternLayoutEncoder.setContext(context);
        }
        patternLayoutEncoder.setPattern("%-4relative [%thread] %-5level %logger{35} -%kvp- %msg%n");
        // ===========no parent setter
        // start the complex property if it implements LifeCycle and is not
        // marked with a @NoAutoStart annotation
        if(NoAutoStartUtil.shouldBeStarted(patternLayoutEncoder)) {
            ((LifeCycle) patternLayoutEncoder).start();
        }
        // Inject component of type PatternLayoutEncoder into parent
        appenderTOTO.setEncoder(patternLayoutEncoder);

        appenderTOTO.start();
        return appenderTOTO;
    }
}
```


