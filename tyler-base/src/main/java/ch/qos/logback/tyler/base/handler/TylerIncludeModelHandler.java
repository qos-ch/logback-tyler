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

import ch.qos.logback.classic.model.PropertiesConfiguratorModel;
import ch.qos.logback.classic.model.processor.PropertiesConfiguratorModelHandler;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.IncludeModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.IncludeModelHandler;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;

public class TylerIncludeModelHandler extends ModelHandlerBase {

    public TylerIncludeModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext ic) {
        return new TylerIncludeModelHandler(context);
    }

    @Override
    protected Class<IncludeModel> getSupportedModelClass() {
        return IncludeModel.class;
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        IncludeModel includeModel = (IncludeModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
        addJavaStatement(tmic, includeModel);
    }

    protected void addJavaStatement(TylerModelInterpretationContext tmic, IncludeModel originalModel) {
        // code to produce
        //  IncludeModel includeModel = new IncludeModel();
        //
        //  includeModel.setFile(originalModel.getFile());  // use actual string value of pcModel.getFile()
        //
        //  includeModel.setUrl(originalModel.getUrl()); // use actual string value of pcModel.getUrl()
        //  includeModel.setResource(originalModel.getResource());
        //  includeModel.setOptional(originalModel.getOptional());
        //  IncludeModelHandler includeModelHandler = new IncludeModelHandler(context);
        //  try {
        //    Model modelFromIncludedFile = includeModelHandler.buildModelFromIncludedFile(this, includeModel);
        //    processModelFromIncludedFile(modelFromIncludedFile);
        //  } catch(ModelHandlerException e) {
        //     addError("Failed to process IncludeModelHandler", e);
        //  }
        String includeModelVarName = "includeModel";
        String imhVarName = "includeModelHandler";
        String mfifVarName = "modelFromIncludedFile";
        tmic.configureMethodSpecBuilder.addStatement("$1T $2N = new $1T()", IncludeModel.class, includeModelVarName);
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(originalModel.getFile())) {
            tmic.configureMethodSpecBuilder.addStatement("$N.setFile(subst($S))", includeModelVarName, originalModel.getFile());
        }
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(originalModel.getUrl())) {
            tmic.configureMethodSpecBuilder.addStatement("$N.setUrl(subst($S))", includeModelVarName, originalModel.getUrl());
        }
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(originalModel.getResource())) {
            tmic.configureMethodSpecBuilder.addStatement("$N.setResource(subst($S))", includeModelVarName, originalModel.getResource());
        }
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(originalModel.getOptional())) {
            tmic.configureMethodSpecBuilder.addStatement("$N.setOptional(subst($S))", includeModelVarName, originalModel.getOptional());
        }
        tmic.configureMethodSpecBuilder.addStatement("$1T $2N = new $1T($3N)", IncludeModelHandler.class, imhVarName, tmic.getContextFieldSpec());
        // "this is the calling TylerConfigurator instance of type ContextAwarePropertyContainer"
        tmic.configureMethodSpecBuilder.beginControlFlow("try");
        tmic.configureMethodSpecBuilder.addStatement("$T $N = $N.buildModelFromIncludedFile(this, $N)", Model.class, mfifVarName, imhVarName, includeModelVarName);
        tmic.configureMethodSpecBuilder.addStatement("processModelFromIncludedFile($N)", mfifVarName);
        tmic.configureMethodSpecBuilder.nextControlFlow("catch($T e)", ModelHandlerException.class);
        tmic.configureMethodSpecBuilder.addStatement("addError(\"Failed to process IncludeModelHandler\", e)");
        tmic.configureMethodSpecBuilder.endControlFlow();
    }
}
