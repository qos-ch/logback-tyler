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

import ch.qos.logback.core.joran.util.StringToObjectConverter;

import java.nio.charset.Charset;

import com.squareup.javapoet.CodeBlock;

public class StringToVariableStament {

    static final String DOLLAR_LEFT_ACCOLADE = "${";

    public static boolean containsVariable(String input) {
        if(input != null) {
            return input.contains(DOLLAR_LEFT_ACCOLADE);
        } else {
            return false;
        }
    }
    
    /**
     * @return a Java expression evaluating to the given string, substituted at runtime if it contains
     * variables, e.g. <code>subst("${name}")</code>, or else the string literal itself
     * @since 1.0.6
     */
    public static CodeBlock stringToSubstitutedExpression(String input) {
        return containsVariable(input) ? CodeBlock.of("subst($S)", input) : CodeBlock.of("$S", input);
    }

    public static String booleanObjectToString(Boolean bool) {
        if(bool == null)
            return "null";
        else
            return bool.toString();
    }

}
