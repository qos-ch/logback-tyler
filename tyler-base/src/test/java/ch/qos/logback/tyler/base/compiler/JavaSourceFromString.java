package ch.qos.logback.tyler.base.compiler;

import java.net.URI;
import javax.tools.SimpleJavaFileObject;

class JavaSourceFromString extends SimpleJavaFileObject {
    private final String sourceCode;

    JavaSourceFromString(String name, String sourceCode) {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.sourceCode = sourceCode;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return sourceCode;
    }
}
