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

package ch.qos.logback.tyler.base;

import ch.qos.logback.tyler.base.util.VersionUtil;

public class TylerConstants {

    public static final char SEMICOLON = ';';

    public static char UNDERSCORE = '_';

    static public final int NOT_FOUND = -1;

    public static final String TYLER_CONFIGURATOR = "TylerConfigurator";
    public static final String CONTEXT_FIELD_NAME = "context";
    public static final String LOGGER_CONTEXT_PARAMETER_NAME = "loggerContext";
    public static final String LEVEL_FIELD_NAME = "level";
    public static final String LEVEL_STRING_PARAMETER_NAME = "levelString";

    public static final String REQUIRED_LOGBACK_VERSION = "1.5.20";

    public static final String TYLER_VERSION = VersionUtil.tylerVersion();

    public static final String LOGGER_FIELD_NAME = "logger";

    public static final String LOGGER_NAME_FIELD_NAME = "loggerName";
    public static final String ADDITIVITY_FIELD_NAME = "additivity";
    public static final String CONFIGURE_METHOD_NAME = "configure";

    public static final String SETUP_APPENDER = "setupAppender";
    public static final String SETUP = "setup";
    public static final String ADD_ON_CONSOLE_STATUS_LISTENER = "addOnConsoleStatusListener";

    public static final String LOCAL_APPENDER_FIELD_NAME = "appender";


    public static final String LOCAL_PROPERTY_CONDITION_FIELD_NAME = "propertyCondition";


}
