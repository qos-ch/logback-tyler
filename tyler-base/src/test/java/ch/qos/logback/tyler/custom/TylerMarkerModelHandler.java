/*
 * Logback-tyler translates logback-classic XML configuration files into
 * Java.
 *
 * Copyright (C) 2024-2026, QOS.ch. All rights reserved.
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
 */

package ch.qos.logback.tyler.custom;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;

import javax.lang.model.element.Modifier;
import java.util.Objects;

/**
 * Translates &lt;marker name="x"/&gt; into a call to a <code>marker("x")</code> helper method added to
 * the generated class, exercising the public accessors of {@link TylerModelInterpretationContext}.
 */
public class TylerMarkerModelHandler extends ModelHandlerBase {

    static final String MARKER_METHOD_NAME = "marker";
    static final String MARKER_COUNT_FIELD_NAME = "markerCount";

    public TylerMarkerModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext mic) {
        return new TylerMarkerModelHandler(context);
    }

    @Override
    protected Class<MarkerModel> getSupportedModelClass() {
        return MarkerModel.class;
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        MarkerModel markerModel = (MarkerModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        addMarkerMethod(tmic);
        tmic.getConfigureMethodSpecBuilder().addStatement("$N($S)", MARKER_METHOD_NAME, markerModel.getName());
    }

    void addMarkerMethod(TylerModelInterpretationContext tmic) {
        // insert method at most once
        if (tmic.containsMethodSpecBuilder(MARKER_METHOD_NAME))
            return;

        tmic.addFieldSpec(FieldSpec.builder(int.class, MARKER_COUNT_FIELD_NAME, Modifier.PRIVATE).build());
        tmic.addStaticImport(Objects.class, "requireNonNull");

        MethodSpec.Builder msBuilder = MethodSpec.methodBuilder(MARKER_METHOD_NAME).addModifiers(Modifier.PRIVATE)
                        .returns(void.class).addParameter(String.class, "name")
                        .addStatement("$N++", MARKER_COUNT_FIELD_NAME)
                        .addStatement("addInfo(\"marker \" + requireNonNull(name))");
        tmic.addMethodSpecBuilder(MARKER_METHOD_NAME, msBuilder);
    }
}
