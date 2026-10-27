/*
 * Logback-tyler translates logback-classic XML configuration files into
 * Java.
 *
 * Copyright (C) 2024-2026, QOS.ch. All rights reserved.
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

package ch.qos.logback.tyler.base;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.Duration;
import ch.qos.logback.core.util.FileSize;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Appender with basic properties of every type supported by logback's StringToObjectConverter, used
 * to compare the effect of Tyler-generated code with that of JoranConfigurator. Located in an exported
 * package so that generated configurators loaded outside the module can access it.
 */
public class PropertyHolderAppender extends AppenderBase<ILoggingEvent> {

    String stringValue;
    int intValue = -1;
    long longValue = -1;
    float floatValue = -1;
    double doubleValue = -1;
    boolean booleanValue;
    Integer boxedInteger;
    Long boxedLong;
    Boolean boxedBoolean;
    Double boxedDouble;
    FilterReply enumValue;
    FileSize fileSize;
    Duration duration;
    Charset charset;
    Character characterValue;
    final List<Integer> ports = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent eventObject) {
    }

    public void setStringValue(String stringValue) { this.stringValue = stringValue; }
    public void setIntValue(int intValue) { this.intValue = intValue; }
    public void setLongValue(long longValue) { this.longValue = longValue; }
    public void setFloatValue(float floatValue) { this.floatValue = floatValue; }
    public void setDoubleValue(double doubleValue) { this.doubleValue = doubleValue; }
    public void setBooleanValue(boolean booleanValue) { this.booleanValue = booleanValue; }
    public void setBoxedInteger(Integer boxedInteger) { this.boxedInteger = boxedInteger; }
    public void setBoxedLong(Long boxedLong) { this.boxedLong = boxedLong; }
    public void setBoxedBoolean(Boolean boxedBoolean) { this.boxedBoolean = boxedBoolean; }
    public void setBoxedDouble(Double boxedDouble) { this.boxedDouble = boxedDouble; }
    public void setEnumValue(FilterReply enumValue) { this.enumValue = enumValue; }
    public void setFileSize(FileSize fileSize) { this.fileSize = fileSize; }
    public void setDuration(Duration duration) { this.duration = duration; }
    public void setCharset(Charset charset) { this.charset = charset; }
    // Character has no valueOf(String) method and is not supported by StringToObjectConverter
    public void setCharacterValue(Character characterValue) { this.characterValue = characterValue; }
    public void addPort(int port) { ports.add(port); }

    /**
     * Property values in comparable form.
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("stringValue", stringValue);
        map.put("intValue", intValue);
        map.put("longValue", longValue);
        map.put("floatValue", floatValue);
        map.put("doubleValue", doubleValue);
        map.put("booleanValue", booleanValue);
        map.put("boxedInteger", boxedInteger);
        map.put("boxedLong", boxedLong);
        map.put("boxedBoolean", boxedBoolean);
        map.put("boxedDouble", boxedDouble);
        map.put("enumValue", enumValue);
        map.put("fileSize", fileSize == null ? null : fileSize.getSize());
        map.put("duration", duration == null ? null : duration.getMilliseconds());
        map.put("charset", charset);
        map.put("characterValue", characterValue);
        map.put("ports", List.copyOf(ports));
        return map;
    }
}
