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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.IncludeModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.IncludeModelHandler;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;

import java.net.URL;

/**
 * Handles &lt;include&gt; elements by inlining the contents of the included file at translation time.
 *
 * <p>In order to remain compatible with GraalVM native images, the generated configurator never
 * falls back to XML parsing at runtime. Consequently, if the included file is not available at
 * translation time, a warning is issued and the &lt;include&gt; element is omitted from the
 * generated code.</p>
 *
 * <p>Variables are not supported in the attributes of &lt;include&gt; elements. However, variables
 * can be used within included files.</p>
 */
public class TylerIncludeModelHandler extends ModelHandlerBase {

    static final String VARIABLE_START = "${";

    /**
     * Description of the include element if its contents were inlined, null otherwise.
     */
    String inlinedDescription;

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
        String description = describe(includeModel);

        if (!tmic.getTranslationOptions().isTranslationTimeInclusionEnabled()) {
            omitInclude(tmic, includeModel, description, "translation-time inclusion is disabled");
            return;
        }

        if (containsVariable(includeModel.getFile()) || containsVariable(includeModel.getUrl())
                        || containsVariable(includeModel.getResource())) {
            omitInclude(tmic, includeModel, description, "variables are not supported in include attributes");
            return;
        }

        // as attributes cannot contain variables, circular inclusion necessarily repeats an include element
        if (!tmic.enterInclude(description)) {
            omitInclude(tmic, includeModel, description, "circular inclusion");
            return;
        }

        resolveResourceWithClassLoaderOfOptions(tmic, includeModel);

        IncludeModelHandler includeModelHandler = new IncludeModelHandler(context);
        // topURL and topScan are null as included files are not watched at translation time
        Model modelFromIncludedFile = includeModelHandler.buildModelFromIncludedFile(tmic, null, null, includeModel);
        if (modelFromIncludedFile == null) {
            tmic.exitInclude(description);
            omitInclude(tmic, includeModel, description, "included file not available at translation time");
            return;
        }

        inlinedDescription = description;
        tmic.configureMethodSpecBuilder.addComment("Start of contents inlined from $L", description);
        // the included models are processed by DefaultProcessor as children of the include model
        includeModel.getSubModels().addAll(modelFromIncludedFile.getSubModels());
    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        if (inlinedDescription == null)
            return;

        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
        tmic.configureMethodSpecBuilder.addComment("End of contents inlined from $L", inlinedDescription);
        tmic.exitInclude(inlinedDescription);
    }

    /**
     * Logback looks up resources with its own class loader. If the translation options specify a class
     * loader, the resource is looked up with that class loader instead, and the include model refers to
     * its URL.
     */
    void resolveResourceWithClassLoaderOfOptions(TylerModelInterpretationContext tmic, IncludeModel includeModel) {
        ClassLoader classLoader = tmic.getTranslationOptions().getClassLoader();
        String resource = includeModel.getResource();
        if (classLoader == null || OptionHelper.isNullOrEmptyOrAllSpaces(resource)) {
            return;
        }
        URL url = classLoader.getResource(resource.trim());
        if (url != null) {
            includeModel.setResource(null);
            includeModel.setUrl(url.toString());
        }
    }

    void omitInclude(TylerModelInterpretationContext tmic, IncludeModel includeModel, String description, String reason) {
        addWarn("Omitting " + description + " on line " + includeModel.getLineNumber() + ": " + reason
                        + ". Its contents will be missing from the generated configurator.");
        tmic.configureMethodSpecBuilder.addComment("$L omitted: $L", description, reason);
    }

    static boolean containsVariable(String attribute) {
        return attribute != null && attribute.contains(VARIABLE_START);
    }

    static String describe(IncludeModel includeModel) {
        StringBuilder sb = new StringBuilder("<include");
        appendAttribute(sb, "file", includeModel.getFile());
        appendAttribute(sb, "url", includeModel.getUrl());
        appendAttribute(sb, "resource", includeModel.getResource());
        appendAttribute(sb, "optional", includeModel.getOptional());
        return sb.append("/>").toString();
    }

    static private void appendAttribute(StringBuilder sb, String name, String value) {
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(value))
            sb.append(' ').append(name).append("=\"").append(value).append('"');
    }
}
