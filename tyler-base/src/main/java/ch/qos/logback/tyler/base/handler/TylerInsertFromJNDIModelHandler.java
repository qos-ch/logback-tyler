/*
 *  Copyright (c) 2004-2024 QOS.ch
 *  All rights reserved.
 *
 *  Permission is hereby granted, free  of charge, to any person obtaining
 *  a  copy  of this  software  and  associated  documentation files  (the
 *  "Software"), to  deal in  the Software without  restriction, including
 *  without limitation  the rights to  use, copy, modify,  merge, publish,
 *  distribute,  sublicense, and/or sell  copies of  the Software,  and to
 *  permit persons to whom the Software  is furnished to do so, subject to
 *  the following conditions:
 *
 *  The  above  copyright  notice  and  this permission  notice  shall  be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 *  EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 *  MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ch.qos.logback.tyler.base.handler;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.InsertFromJNDIModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.InsertFromJNDIModelHandler;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

import javax.lang.model.element.Modifier;

import static ch.qos.logback.tyler.base.TylerConstants.LOGGER_CONTEXT_PARAMETER_NAME;

public class TylerInsertFromJNDIModelHandler extends ModelHandlerBase {

    public TylerInsertFromJNDIModelHandler(Context context) {
        super(context);
    }

    static public TylerInsertFromJNDIModelHandler makeInstance(Context context, ModelInterpretationContext mic) {
        return new TylerInsertFromJNDIModelHandler(context);
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        InsertFromJNDIModel insertFromJNDIModel = (InsertFromJNDIModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
        addJavaStatement(tmic, insertFromJNDIModel);
    }

    private void addJavaStatement(TylerModelInterpretationContext tmic, InsertFromJNDIModel originalModel) {

        // in configuration method add
        //   insertFromJNDIAs_$asStr(originalModel.asStr, originalModel.envEntryStr, originalModel.scopeStr);

        //  void insertFromJNDIAs(String asStr, String envEntryStr, String scopeStr) {
        //        InsertFromJNDIModel insertFromJNDIModel = new InsertFromJNDIModel();
        //        insertFromJNDIModel.setAs(subst(asStr));
        //        insertFromJNDIModel.setEnvEntryName(subst(envEntryStr));
        //        insertFromJNDIModel.setScopeStr(subst(sopeStr));
        //
        //        InsertFromJNDIModelHandler insertFromJNDIModelHandler = new InsertFromJNDIModelHandler(context);
        //        insertFromJNDIModelHandler.detachedHandle(this, insertFromJNDIModel);
        //  }
        String insertFromJNDIModelVarName = "insertFromJNDIModel";
        String ifjmVarName = "insertFromJNDIModelHandler";

        String asStr = originalModel.getAs();

        String jndiMethodName_As = "insertFromJNDIAs_" + asStr;
        String asStrVarName = "asStr";
        String envEntryStrVarName = "envEntryStr";
        String scopeStrVarName = "scopeStr";

        final ParameterSpec asStr_ParameterSpec = ParameterSpec.builder(String.class, asStrVarName).build();
        final ParameterSpec envEntryStr_ParameterSpec = ParameterSpec.builder(String.class, envEntryStrVarName).build();
        final ParameterSpec scopeStr_ParameterSpec = ParameterSpec.builder(String.class, scopeStrVarName).build();

        MethodSpec.Builder msBuilder = MethodSpec.methodBuilder(jndiMethodName_As).addModifiers(Modifier.PRIVATE)
                        .addParameter(asStr_ParameterSpec)
                        .addParameter(envEntryStr_ParameterSpec)
                        .addParameter(scopeStr_ParameterSpec)
                        .returns(void.class);

        msBuilder.addStatement("$1T $2N = new $1T()", InsertFromJNDIModel.class, insertFromJNDIModelVarName);
        msBuilder.addStatement("$N.setAs(subst($S))", insertFromJNDIModelVarName, asStrVarName);
        msBuilder.addStatement("$N.setEnvEntryName(subst($S))", insertFromJNDIModelVarName, envEntryStrVarName);
        msBuilder.addStatement("$N.setScopeStr(subst($S))", insertFromJNDIModelVarName, scopeStrVarName);

        msBuilder.addStatement("$1T $2N = new $1T($3N)", InsertFromJNDIModelHandler.class, ifjmVarName, tmic.getContextFieldSpec());
        msBuilder.addStatement("$N.detachedHandle(this, $N)", ifjmVarName, insertFromJNDIModelVarName);

        tmic.mapOfMethodSpecBuilders.put(jndiMethodName_As, msBuilder);

        tmic.configureMethodSpecBuilder.addStatement("$N($S, $S, $S)", jndiMethodName_As, originalModel.getAs(), originalModel.getEnvEntryName(), originalModel.getScopeStr());

    }
}
