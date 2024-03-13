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

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.tyler.base.util.VariableNameUtil;
import com.squareup.javapoet.MethodSpec;

/**
 *
 */
public class ImplicitModelHandlerData {


    final String parentVariableName;
    final Class parentObjectClass;

    final String  variableName;
    final MethodSpec.Builder methodSpecBuilder;

    public ImplicitModelHandlerData(Class parentObjectClass, String variableName, MethodSpec.Builder methodSpecBuilder) {
        this(null, parentObjectClass, variableName, methodSpecBuilder);
    }

    /**
     *
     * @param parentVariableName used to inject the parent into the current object if it has the setParent method
     * @param parentObjectClass
     * @param variableName
     * @param methodSpecBuilder
     */
    public ImplicitModelHandlerData(String parentVariableName, Class parentObjectClass, String variableName, MethodSpec.Builder methodSpecBuilder) {
        this.parentVariableName = parentVariableName;
        this.parentObjectClass = parentObjectClass;
        this.variableName = variableName;
        this.methodSpecBuilder = methodSpecBuilder;
    }

    public String getParentVariableName() {
        return parentVariableName;
    }

    public Class getParentObjectClass() {
        return parentObjectClass;
    }

    public String getVariableName() {
        return variableName;
    }

    public MethodSpec.Builder getMethodSpecBuilder() {
        return methodSpecBuilder;
    }

    public static ImplicitModelHandlerData makeInstance(ContextAware contextAware, MethodSpec.Builder methodSpec, String fqcn) {
        String variableName = VariableNameUtil.fullyQualifiedClassNameToVariableName(fqcn);

        try {
            Class aClass = Class.forName(fqcn);
            ImplicitModelHandlerData implicitModelHandlerData = new ImplicitModelHandlerData(aClass, variableName,
                    methodSpec);
           return implicitModelHandlerData;
        } catch (ClassNotFoundException e) {
            contextAware.addError("Could not find class ["+fqcn+"]");
            return null;
        }
    }
}
