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
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;

import static ch.qos.logback.tyler.base.util.StringToVariableStament.booleanObjectToString;

public class TylerPropertiesConfiguratorModelHandler extends ModelHandlerBase {

    public TylerPropertiesConfiguratorModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext ic) {
        return new TylerPropertiesConfiguratorModelHandler(context);
    }

    @Override
    protected Class<PropertiesConfiguratorModel> getSupportedModelClass() {
        return PropertiesConfiguratorModel.class;
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        PropertiesConfiguratorModel propertyConfiguratorModel = (PropertiesConfiguratorModel) model;
        TylerModelInterpretationContext tmic  = (TylerModelInterpretationContext) mic;

        addJavaStatement(tmic, propertyConfiguratorModel);
    }

    protected void addJavaStatement(TylerModelInterpretationContext tmic, PropertiesConfiguratorModel pcModel) {

        // code to produce
        //PropertiesConfiguratorModel propertyConfiguratorModel = new PropertiesConfiguratorModel();
        //propertyConfiguratorModel.setFile(pcModel.getFile());  // use actual string value of pcModel.getFile()
        //propertyConfiguratorModel.setUrl(pcModel.getUrl()); // use actual string value of pcModel.getUrl()
        //propertyConfiguratorModel.setResource(pcModel.getResource());
        //propertyConfiguratorModel.setOptional(pcModel.getOptional());
        //PropertiesConfiguratorModelHandler propertiesConfiguratorModelHandler = new PropertiesConfiguratorModelHandler(context);
        //propertiesConfiguratorModelHandler.detachedHandle((ContextAwarePropertyContainer) this, propertyConfiguratorModel, topScanBoolean);

        Boolean topScanBoolean = tmic.getTopScanBoolean();

        String pcmVarName = "propertyConfiguratorModel";
        String pcmhVarName = "propertiesConfiguratorModelHandler";

        tmic.configureMethodSpecBuilder.addStatement("$1T $2N = new $1T()", PropertiesConfiguratorModel.class, pcmVarName);
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(pcModel.getFile())) {
            tmic.configureMethodSpecBuilder.addStatement("$N.setFile(subst($S))", pcmVarName, pcModel.getFile());
        }
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(pcModel.getUrl())) {
            tmic.configureMethodSpecBuilder.addStatement("$N.setUrl(subst($S))", pcmVarName, pcModel.getUrl());
        }
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(pcModel.getResource())) {
            tmic.configureMethodSpecBuilder.addStatement("$N.setResource(subst($S))", pcmVarName, pcModel.getResource());
        }
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(pcModel.getOptional())) {
            tmic.configureMethodSpecBuilder.addStatement("$N.setOptional(subst($S))", pcmVarName, pcModel.getOptional());
        }
        tmic.configureMethodSpecBuilder.addStatement("$1T $2N = new $1T($3N)", PropertiesConfiguratorModelHandler.class, pcmhVarName,
                        tmic.getContextFieldSpec());
        // "this is the calling TylerConfigurator instance of type ContextAwarePropertyContainer"
        tmic.configureMethodSpecBuilder.beginControlFlow("try");

        tmic.configureMethodSpecBuilder.addStatement("$N.detachedHandle(this, $N, $N)", pcmhVarName, pcmVarName, tmic.topScanFieldName);
        tmic.configureMethodSpecBuilder.nextControlFlow("catch($T e)", ModelHandlerException.class);
        tmic.configureMethodSpecBuilder.addStatement("addError(\"Failed to process PropertyConfiguratorModel\", e)");
        tmic.configureMethodSpecBuilder.endControlFlow();

    }
}
