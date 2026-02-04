/*
 * Logback-tyler translates logback-classic XML configuration files into
 * Java.
 *
 * Copyright (C) 2024-2024-2026, QOS.ch. All rights reserved.
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

package ch.qos.logback.tyler.base.handler;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ActionUtil;
import ch.qos.logback.core.joran.action.TimestampAction;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.TimestampModel;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.CachingDateFormatter;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import com.squareup.javapoet.MethodSpec;

public class TimestampModelHandler extends ModelHandlerBase {

    boolean inError = false;
    static final String TIMESTAMP_METHOD_NAME = "timestamp";
    public TimestampModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext ic) {
        return new TimestampModelHandler(context);
    }


    @Override
    protected Class<TimestampModel> getSupportedModelClass() {
        return TimestampModel.class;
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        TimestampModel timestampModel = (TimestampModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
        String keyStr = timestampModel.getKey();
        if (OptionHelper.isNullOrEmptyOrAllSpaces(keyStr)) {
            addError("Attribute named [" + Action.KEY_ATTRIBUTE + "] cannot be empty");
            inError = true;
        }

        String datePatternStr = timestampModel.getDatePattern();
        if (OptionHelper.isNullOrEmptyOrAllSpaces(datePatternStr)) {
            addError("Attribute named [" + TimestampAction.DATE_PATTERN_ATTRIBUTE + "] cannot be empty");
            inError = true;
        }

        if(inError) {
            addError("Skipping method generation for timestamp. Line " + model.getLineNumber());
            return;
        }

        addJavaStatement(tmic, timestampModel);
    }

    void addJavaStatement(TylerModelInterpretationContext tmic, TimestampModel timestampModel) {
        String scopeStr = timestampModel.getScopeStr();
        if(scopeStr == null) {
            scopeStr = "";
        }
        final String keyStr = timestampModel.getKey();
        final String scopeVarName = "scope";
        final String datePatternStr = timestampModel.getDatePattern();
        final String cdfVarName = "cdf";
        final String timevalVarName = "timeValue";
        String timeReferenceVarName = "timeReference";

        final String timeReferenceStr = timestampModel.getTimeReference();



        MethodSpec.Builder timestampMethodSpecBuilder = MethodSpec.methodBuilder(toMethodName(keyStr)).
                returns(void.class)
                .addStatement("$T $N = $T.stringToScope($S)", ActionUtil.Scope.class, scopeVarName, ActionUtil.class,
                        scopeStr)
                .addStatement("$1T $2N = new $1T($3S)", CachingDateFormatter.class, cdfVarName, datePatternStr);

        if (TimestampModel.CONTEXT_BIRTH.equalsIgnoreCase(timeReferenceStr)) {
            timestampMethodSpecBuilder.addStatement("addInfo(\"Using context birth as time reference.\"");
            timestampMethodSpecBuilder.addStatement("long $N = $N.getBirthTime()", timeReferenceVarName, tmic.getContextFieldSpec());
        } else {
            timestampMethodSpecBuilder.addStatement("long $N = System.currentTimeMillis()", timeReferenceVarName);
            timestampMethodSpecBuilder.addStatement("addInfo(\"Using current interpretation time, i.e. now, as time reference.\")");
        }

        timestampMethodSpecBuilder.addStatement("String $N = $N.format($N)", timevalVarName, cdfVarName, timeReferenceVarName);

        timestampMethodSpecBuilder.addStatement("addInfo(\"Adding property to the context with key='\"+$S+\"' and value=\"+$N+\" to the \"+$N+\" scope\")"
                , keyStr, timevalVarName, scopeVarName);

        timestampMethodSpecBuilder.addStatement("$T.setProperty(this, $S, $N, $N)", ActionUtil.class, keyStr, timevalVarName, scopeVarName);

        MethodSpec timestampMethodSpec = timestampMethodSpecBuilder.build();
        tmic.tylerConfiguratorTSB.addMethod(timestampMethodSpec);
        tmic.configureMethodSpecBuilder.addStatement("$N()", timestampMethodSpec);

    }

    String toMethodName(String k) {
        return TIMESTAMP_METHOD_NAME+'_'+k;
    }
}
