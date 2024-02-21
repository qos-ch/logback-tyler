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

import ch.qos.logback.classic.model.ContextNameModel;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import com.squareup.javapoet.MethodSpec;

import static ch.qos.logback.tyler.base.TylerConstants.SET_CONTEXT_NAME;

public class ContextNameModelHandler  extends ModelHandlerBase  {

    public ContextNameModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext ic) {
        return new ContextNameModelHandler(context);
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        ContextNameModel contextNameModel = (ContextNameModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        String finalBody = mic.subst(contextNameModel.getBodyText());
        addInfo("Setting logger context name as [" + finalBody + "]");
        System.out.println("==="+finalBody);
        addJavaStatement(tmic, finalBody);

    }


    void addJavaStatement(TylerModelInterpretationContext tmic, String finalBody) {
        //
        // context.setName(finalBody);

        final String parameterName = "name";

        MethodSpec setContextNameMethodSpec = MethodSpec.methodBuilder(SET_CONTEXT_NAME)
                .addParameter(String.class, parameterName)
                .returns(void.class)
                .beginControlFlow("try")
                .addStatement("$N.setName($N)", tmic.getContextFieldSpec(), parameterName)
                .nextControlFlow("catch ($T e)", IllegalStateException.class)
                .addStatement("addError(\"Failed to rename context as [\"+$N+\"]\")", parameterName)
                .endControlFlow()
                .build();

        tmic.configureMethodSpecBuilder.addStatement("$N($S)", setContextNameMethodSpec, finalBody);
        tmic.tylerConfiguratorTSB.addMethod(setContextNameMethodSpec);
    }

}
