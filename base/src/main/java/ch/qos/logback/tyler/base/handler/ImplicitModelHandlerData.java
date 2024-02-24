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

package ch.qos.logback.tyler.base.handler;

import com.squareup.javapoet.MethodSpec;

/**
 *
 */
public class ImplicitModelHandlerData {


    final String parentVariableName;
    final Class objClass;

    final String  variableName;
    final MethodSpec.Builder methodSpecBuilder;

    public ImplicitModelHandlerData(Class objClass, String variableName, MethodSpec.Builder methodSpecBuilder) {
        this(null, objClass, variableName, methodSpecBuilder);
    }
    public ImplicitModelHandlerData(String parentVariableName, Class objClass, String variableName, MethodSpec.Builder methodSpecBuilder) {
        this.parentVariableName = parentVariableName;
        this.objClass = objClass;
        this.variableName = variableName;
        this.methodSpecBuilder = methodSpecBuilder;
    }

    public String getParentVariableName() {
        return parentVariableName;
    }

    public Class getObjClass() {
        return objClass;
    }

    public String getVariableName() {
        return variableName;
    }

    public MethodSpec.Builder getMethodSpecBuilder() {
        return methodSpecBuilder;
    }
}
