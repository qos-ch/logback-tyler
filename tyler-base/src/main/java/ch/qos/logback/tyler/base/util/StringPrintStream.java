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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class StringPrintStream extends PrintStream {

    PrintStream other;
    boolean duplicate = false;

    public List<String> stringList = new ArrayList<String>();

    public StringPrintStream(PrintStream ps) {
        this(ps, false);
    }

    public StringPrintStream(PrintStream ps, boolean duplicate) {
        super(ps);
        other = ps;
        this.duplicate = duplicate;
    }

    public void print(String s) {
        if (duplicate)
            other.print(s);
        stringList.add(s);
    }

    public void println(String s) {
        if (duplicate)
            other.println(s);
        stringList.add(s);
    }

    public void println(Object o) {
        if (duplicate)
            other.println(o.toString());
        stringList.add(o.toString());
    }
}
