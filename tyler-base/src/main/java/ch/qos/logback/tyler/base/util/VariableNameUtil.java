/*
 * Logback-tyler translates logback-classic XML configuration files into
 * Java.
 *
 * Copyright (C) 2024-2024-2026, QOS.ch. All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ch.qos.logback.tyler.base.util;
//import static ch.qos.logback.core.CoreConstants.DOT;
import ch.qos.logback.core.model.conditional.ByPropertiesConditionModel;
import ch.qos.logback.core.util.StringUtil;

import static ch.qos.logback.core.CoreConstants.DOT;
import static ch.qos.logback.tyler.base.TylerConstants.UNDERSCORE;

public class VariableNameUtil {


    static public String replaceDotsWithUnderscores(String loggerName) {
        return loggerName.replace(DOT, UNDERSCORE);
    }

    /**
     * Return "logger_" followed by the logger name in which every character not allowed in a Java
     * identifier, including dots, is replaced with an underscore.
     */
    static public String loggerNameToVariableName(String loggerName) {
        return "logger_"+toJavaIdentifierPart(loggerName);
    }

    /**
     * Replace every character not allowed in a Java identifier with an underscore. Identifier-ignorable
     * characters are replaced as well.
     *
     * @since 1.0.5
     */
    static public String toJavaIdentifierPart(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        str.codePoints().forEach(cp -> {
            if (Character.isJavaIdentifierPart(cp) && !Character.isIdentifierIgnorable(cp)) {
                sb.appendCodePoint(cp);
            } else {
                sb.append(UNDERSCORE);
            }
        });
        return sb.toString();
    }


    /**
     * Return "appender" followed by the appender name, with its first letter capitalized and every
     * character not allowed in a Java identifier replaced with an underscore. Distinct appender names
     * may map to the same variable name.
     */
    static public String appenderNameToVariableName(String appenderName) {
        return "appender"+ StringUtil.capitalizeFirstLetter(toJavaIdentifierPart(appenderName));
    }

    static public String fullyQualifiedClassNameToVariableName(String fqcn) {
        String simpleClassName = ClassUtil.extractSimpleClassName(fqcn);
        return StringUtil.lowercaseFirstLetter(simpleClassName);
    }

    public static String conditionModelToVariableName(String className, int instanceNum) {
        String simpleClassName = ClassUtil.extractSimpleClassName(className);
        return StringUtil.lowercaseFirstLetter(simpleClassName) + "_" + instanceNum;
    }
}
