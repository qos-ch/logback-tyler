package ch.qos.logback.tyler.base.compiler;

import ch.qos.logback.core.util.EnvUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
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
        return compile(name, sourceCode, makeOutputDirectory());
    }

    /**
     * Compile the source code and load the resulting class with a class loader whose parent is the
     * class loader of this class.
     *
     * @throws IllegalStateException if compilation fails
     */
    public Class<?> compileAndLoad(String name, String sourceCode) throws ClassNotFoundException {
        Path outputDirectory = makeOutputDirectory();
        CompilerVerificationResult result = compile(name, sourceCode, outputDirectory);
        if (!result.successfullyCompiled()) {
            throw new IllegalStateException("Compilation of [" + name + "] failed:\n" + result.diagnosticsMessages());
        }
        try {
            URL[] urls = { outputDirectory.toUri().toURL() };
            // the class loader is not closed as the loaded class may be used after this method returns
            URLClassLoader classLoader = new URLClassLoader(urls, CompilationVerifier.class.getClassLoader());
            return classLoader.loadClass(name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private CompilerVerificationResult compile(String name, String sourceCode, Path outputDirectory) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final JavaFileObject source = new JavaSourceFromString(name, sourceCode);

        try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), Charset.defaultCharset());) {
            List<String> options = List.of("-classpath", classpath, "-d", outputDirectory.toString());
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, List.of(source));
            boolean compilationResult = task.call();

            return new CompilerVerificationResult(compilationResult, diagnostics);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // write class files under target/ instead of the current directory
    private Path makeOutputDirectory() {
        try {
            Path parent = Files.createDirectories(Path.of("target", "compilation-verifier"));
            return Files.createTempDirectory(parent, "out-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
