# logback-tyler

Logback-tyler translates logback-classic XML configuration files to Java.

The resulting java class named `TylerConfigurator` implements the [`Configurator`](https://logback.qos.ch/xref/ch/qos/logback/classic/spi/Configurator.html) 
interface. It can thus be declared as a custom configuration provider using Java's standard [service-provider](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html)
meachanism. Custom configurators are searched by looking up file resource located under the _META-INF/services/ch.qos.logback.classic.spi.Configurator_ file in your project.



replace logback.xml. 
