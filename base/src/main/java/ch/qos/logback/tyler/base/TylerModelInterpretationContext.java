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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

import static ch.qos.logback.tyler.base.TylerConstants.CONFIGURE_METHOD_NAME;
import static ch.qos.logback.tyler.base.TylerConstants.LOGGER_CONTEXT_FIELD_NAME;

public class TylerModelInterpretationContext extends ModelInterpretationContext {

    final public TypeSpec.Builder tylerConfiguratorTSB;
    final public MethodSpec.Builder configureMethodSpecBuilder;


    final FieldSpec loggerContextFieldSpec = FieldSpec.builder(LoggerContext.class, LOGGER_CONTEXT_FIELD_NAME, Modifier.PRIVATE).build();
    final ParameterSpec contextParameterSpec = ParameterSpec.builder(LoggerContext.class, LOGGER_CONTEXT_FIELD_NAME).build();

    public TylerModelInterpretationContext(Context context) {
        super(context);
        this.configureMethodSpecBuilder = initializeConfiguerMethodSpecBuilder();
        this.tylerConfiguratorTSB = initializeTylerConfiguratoirTSB();
    }


    TypeSpec.Builder initializeTylerConfiguratoirTSB() {
        TypeSpec.Builder tsb  = TypeSpec.classBuilder(TylerConstants.TYLER_CONFIGURATOR)
                .addSuperinterface(Configurator.class)
                .addField(loggerContextFieldSpec);
        return tsb;
    }

    private MethodSpec.Builder initializeConfiguerMethodSpecBuilder() {
        MethodSpec.Builder msb = MethodSpec.methodBuilder(CONFIGURE_METHOD_NAME)
                .addParameter(contextParameterSpec)
                .returns(Configurator.ExecutionStatus.class)
                .addStatement("this.$N = $N", loggerContextFieldSpec, contextParameterSpec);
        return msb;
    }


    public FieldSpec getLoggerContextFieldSpec() {
        return loggerContextFieldSpec;
    }
}
