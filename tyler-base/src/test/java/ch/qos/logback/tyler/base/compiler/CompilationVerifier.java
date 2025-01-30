package ch.qos.logback.tyler.base.compiler;

import ch.qos.logback.core.util.EnvUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class CompilationVerifier {

    private static final String classpath;

    static {
        String systemClasspath = System.getProperty("java.class.path");
        String modulePath = System.getProperty("jdk.module.path");

        char separatorChar = EnvUtil.isWindows() ? ';' : ':';

        if (modulePath != null && !modulePath.isBlank()) {
            systemClasspath += separatorChar + modulePath;
        }
        classpath = systemClasspath;
    }

    public CompilerVerificationResult verify(String name, String sourceCode) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final JavaFileObject source = new JavaSourceFromString(name, sourceCode);

        try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), Charset.defaultCharset());) {
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, List.of("-classpath", classpath), null, List.of(source));
            boolean compilationResult = task.call();

            return new CompilerVerificationResult(compilationResult, diagnostics);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
