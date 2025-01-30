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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.AppenderRefModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.VariableNameUtil;
import com.squareup.javapoet.MethodSpec;


public class AppenderRefModelHandler extends ModelHandlerBase {

    boolean inError = false;

    public AppenderRefModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext ic) {
        return new AppenderRefModelHandler(context);
    }

    @Override
    protected Class<? extends AppenderRefModel> getSupportedModelClass() {
        return AppenderRefModel.class;
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        AppenderRefModel appenderRefModel = (AppenderRefModel) model;
        Object o = mic.peekObject();

        if(o instanceof String) {
            String loggerName  = (String) o;
            String variableName = VariableNameUtil.loggerNameToVariableName(loggerName);
            addJavaStatement(tmic.configureMethodSpecBuilder, variableName, appenderRefModel.getRef());
        } else if(o instanceof ImplicitModelHandlerData) {
            ImplicitModelHandlerData implicitModelHandlerData = (ImplicitModelHandlerData) o;
             String variableName = implicitModelHandlerData.variableName;
             addJavaStatementForNestedAppender(implicitModelHandlerData.methodSpecBuilder, variableName, appenderRefModel.getRef());
        } else {
            inError = true;
            addError("Was expecting an object of type AppenderAttachableData");
            return;
        }

    }

    private void addJavaStatement(MethodSpec.Builder methodSpecBuilder, String variableName, String ref) {
        String appenderVariableName = VariableNameUtil.appenderNameToVariableName(ref);
        methodSpecBuilder.addStatement("$N.addAppender($N)", variableName, appenderVariableName);
    }

    private void addJavaStatementForNestedAppender(MethodSpec.Builder methodSpecBuilder, String variableName, String ref) {
        String nestedAppenderVariableName = VariableNameUtil.appenderNameToVariableName(ref);

        methodSpecBuilder.beginControlFlow("if($N == null)", nestedAppenderVariableName);
        methodSpecBuilder.addStatement("addInfo(\"Could not find appender named '$N'\")", ref);
        methodSpecBuilder.nextControlFlow("else");
        methodSpecBuilder.addStatement("$N.addAppender($N)", variableName, nestedAppenderVariableName);
        methodSpecBuilder.endControlFlow();
    }


}