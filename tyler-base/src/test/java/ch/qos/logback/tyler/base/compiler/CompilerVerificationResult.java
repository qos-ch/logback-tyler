package ch.qos.logback.tyler.base.compiler;

import java.util.Locale;
import java.util.stream.Collectors;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

public class CompilerVerificationResult {
    private final boolean success;
    private final DiagnosticCollector<JavaFileObject> diagnostics;

    CompilerVerificationResult(boolean success, DiagnosticCollector<JavaFileObject> diagnostics) {
        this.success = success;
        this.diagnostics = diagnostics;
    }

    public boolean successfullyCompiled() {
        return success;
    }

    public String diagnosticsMessages() {
        return diagnostics.getDiagnostics().stream()
                .map(diagnostic -> diagnostic.getMessage(Locale.getDefault()))
                .collect(Collectors.joining("\n"));
    }
}
