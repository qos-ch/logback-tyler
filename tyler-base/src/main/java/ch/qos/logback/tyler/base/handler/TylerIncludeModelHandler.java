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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

import javax.lang.model.element.Modifier;

public class TylerIncludeModelHandler extends ModelHandlerBase {

    final static String INCLUDE_METHOD_NAME = "include";

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

        addIncludeMethod(tmic);
        tmic.configureMethodSpecBuilder.addStatement("$N($S, $S, $S, $S)", INCLUDE_METHOD_NAME, originalModel.getFile(), originalModel.getUrl(),
                        originalModel.getResource(), originalModel.getOptional());

    }

    void addIncludeMethod(TylerModelInterpretationContext tmic) {


        // insert method at most once
        if(tmic.mapOfMethodSpecBuilders.containsKey(INCLUDE_METHOD_NAME))
            return;

        // code to produce
        //
        // void include(String fileStr, String urlStr, String resourceStr, String optionalStr) {

        //    IncludeModel includeModel = new IncludeModel();
        //
        //    includeModel.setFile(fileStr);  // use actual string value of pcModel.getFile()
        //
        //    includeModel.setUrl(urlStr); // use actual string value of pcModel.getUrl()
        //    includeModel.setResource(resourceStr);
        //    includeModel.setOptional(optionalStr);
        //    IncludeModelHandler includeModelHandler = new IncludeModelHandler(context);
        //    try {
        //      Model modelFromIncludedFile = includeModelHandler.buildModelFromIncludedFile(this, includeModel);
        //      processModelFromIncludedFile(modelFromIncludedFile);
        //    } catch(ModelHandlerException e) {
        //       addError("Failed to process IncludeModelHandler", e);
        //    }
        // }

        String includeModelVarName = "includeModel";
        String imhVarName = "includeModelHandler";
        String mfifVarName = "modelFromIncludedFile";

        final String fileStrVarName = "fileStr";
        final String urlStrVarName = "urlStr";
        final String resourceStrVarName = "resourceStr";
        final String optionalStrVarName = "optionalStr";


        final ParameterSpec fileStr_ParameterSpec = ParameterSpec.builder(String.class, fileStrVarName).build();
        final ParameterSpec urStr_ParameterSpec = ParameterSpec.builder(String.class, urlStrVarName).build();
        final ParameterSpec resourceStr_ParameterSpec = ParameterSpec.builder(String.class, resourceStrVarName).build();
        final ParameterSpec optionalStr_ParameterSpec = ParameterSpec.builder(String.class, optionalStrVarName).build();

        MethodSpec.Builder msBuilder = MethodSpec.methodBuilder(INCLUDE_METHOD_NAME).addModifiers(Modifier.PRIVATE).addParameter(fileStr_ParameterSpec)
                        .addParameter(urStr_ParameterSpec).addParameter(resourceStr_ParameterSpec).addParameter(optionalStr_ParameterSpec).returns(void.class);

        msBuilder.addStatement("$1T $2N = new $1T()", IncludeModel.class, includeModelVarName);
        msBuilder.addStatement("$N.setFile(subst($N))", includeModelVarName, fileStrVarName);
        msBuilder.addStatement("$N.setUrl(subst($N))", includeModelVarName, urlStrVarName);
        msBuilder.addStatement("$N.setResource(subst($N))", includeModelVarName, resourceStrVarName);
        msBuilder.addStatement("$N.setOptional(subst($N))", includeModelVarName, optionalStrVarName);
        msBuilder.addStatement("$1T $2N = new $1T($3N)", IncludeModelHandler.class, imhVarName, tmic.getContextFieldSpec());

        // "this is the calling TylerConfigurator instance of type ContextAwarePropertyContainer"
        msBuilder.beginControlFlow("try");
        msBuilder.addStatement("$T $N = $N.buildModelFromIncludedFile(this, $N)", Model.class, mfifVarName, imhVarName, includeModelVarName);
        msBuilder.addStatement("processModelFromIncludedFile($N)", mfifVarName);
        msBuilder.nextControlFlow("catch($T e)", ModelHandlerException.class);
        msBuilder.addStatement("addError(\"Failed to process IncludeModelHandler\", e)");
        msBuilder.endControlFlow();

        tmic.mapOfMethodSpecBuilders.put(INCLUDE_METHOD_NAME, msBuilder);

        msBuilder.addJavadoc("""
        <p>Warning: please note that at translation time logback-tyler usually does not have  
        access to the included file.<p>
        
        <p>It follows that the code in this method (produced by logback-tyler) falls back to invoking 
        logback-classic's default configurator, i.e. JoranConfigurator which will invoke an XML 
        parser.</p>
        
        <p>If you wish to avoid calling <code>JoranConfigurator</code>, then you can insert 
        the contents of the included XML file into the encompassing file manually before performing 
        the translation.</p>
        
        <p>Also note that PropertiesConfigurator introduced in version 1.5.8, allows for setting 
        logger levels via a properties file. The location of properties files can be specified 
        as a file path as well a URL via HTTP or HTTPS protocols. Watching files and 
        reconfiguration upon change are also supported. Logback-tyler supports 
        <code>PropertiesConfigurator</code>.</p>
                
        """);
    }
}
