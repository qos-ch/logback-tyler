/*
 * Copyright (c) 2024 QOS.ch Sarl (Switzerland)
 * All rights reserved.
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
 *
 *
 *
 */

package ch.qos.logback.tyler.base.util;

import ch.qos.logback.core.joran.util.StringToObjectConverter;

import java.nio.charset.Charset;

public class StringToVariableStament {

    static final String DOLLAR_LEFT_ACCOLADE = "${";

    public static boolean containsVariable(String input) {
        if(input != null) {
            return input.contains(DOLLAR_LEFT_ACCOLADE);
        } else {
            return false;
        }
    }

    public static String convertArg (Class<?> type, String value) {
        boolean isVar = containsVariable(value);

        if (String.class.isAssignableFrom(type)) {
            return isVar ? "subst($S)" : "$S";
        } else if (Integer.TYPE.isAssignableFrom(type)) {
            return "$N";
        } else if (Long.TYPE.isAssignableFrom(type)) {
            return "$N";
        } else if (Float.TYPE.isAssignableFrom(type)) {
            return "$N";
        } else if (Double.TYPE.isAssignableFrom(type)) {
            return "$N";
        } else if (Boolean.TYPE.isAssignableFrom(type)) {
            return "$N";
        } else if (type.isEnum()) {
            return "Enum.valueOf("+type.getName()+", $S";
        } else if (StringToObjectConverter.followsTheValueOfConvention(type)) {
            return type.getName()+".valueOf($S)";
        } else if (isOfTypeCharset(type)) {
            return  "Charset.forName($S)";
        }

        return null;
    }

    static private boolean isOfTypeCharset(Class<?> type) {
        return Charset.class.isAssignableFrom(type);
    }


    public static String booleanObjectToString(Boolean bool) {
        if(bool == null)
            return "null";
        else
            return bool.toString();
    }

}
