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
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.hook.DefaultShutdownHook;
import ch.qos.logback.core.hook.ShutdownHook;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.ShutdownHookModel;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StringUtil;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.ClassUtil;
import ch.qos.logback.tyler.base.util.VariableNameUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;

import static ch.qos.logback.tyler.base.TylerConstants.SETUP;

public class ShutdownHookModelHandler  extends ModelHandlerBase  {

    static final String DEFAULT_SHUTDOWN_HOOK_CLASSNAME = DefaultShutdownHook.class.getName();
    static final String OLD_SHUTDOWN_HOOK_CLASSNAME = "ch.qos.logback.core.hook.DelayingShutdownHook";
    static public final String RENAME_WARNING = OLD_SHUTDOWN_HOOK_CLASSNAME + " was renamed as "+ DEFAULT_SHUTDOWN_HOOK_CLASSNAME;

    ImplicitModelHandlerData implicitModelHandlerData;
    boolean inError = false;

    public ShutdownHookModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext mic) {
        return new ShutdownHookModelHandler(context);
    }

    @Override
    protected Class<ShutdownHookModel> getSupportedModelClass() {
        return ShutdownHookModel.class;
    }


    @Override
    public void handle(ModelInterpretationContext mic, Model model)
            throws ModelHandlerException {

        ShutdownHookModel shutdownHookModel = (ShutdownHookModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        String className = shutdownHookModel.getClassName();
        if (OptionHelper.isNullOrEmptyOrAllSpaces(className)) {
            className = DEFAULT_SHUTDOWN_HOOK_CLASSNAME;
            addInfo("Assuming className [" + className + "]");
        } else {
            className = mic.getImport(className);
            if(className.equals(OLD_SHUTDOWN_HOOK_CLASSNAME)) {
                className = DEFAULT_SHUTDOWN_HOOK_CLASSNAME;
                addWarn(RENAME_WARNING);
                addWarn("Please use the new class name");
            }
        }

        addInfo("About to instantiate shutdown hook of type [" + className + "]");

        MethodSpec.Builder methodSpec = addJavaStatement(tmic, className);
        this.implicitModelHandlerData = ImplicitModelHandlerData.makeInstance(this, methodSpec, className);
        if(implicitModelHandlerData != null) {
            mic.pushObject(implicitModelHandlerData);
        } else {
            addError("Could not make implicitModelHandlerData for ["+className+"]");
            model.markAsSkipped();
            inError = true;
        }

    }

    MethodSpec.Builder  addJavaStatement(TylerModelInterpretationContext tmic, String fqcnStr) {
        String simpleName = ClassUtil.extractSimpleClassName(fqcnStr);
        ClassName desiredCN = ClassName.get(ClassUtil.extractPackageName(fqcnStr),
                simpleName);

        final String variableName = StringUtil.lowercaseFirstLetter(simpleName);
        final String hookTreadVariableName = "hookThread";
        final FieldSpec contextFieldSpec = tmic.getContextFieldSpec();

        MethodSpec.Builder setupMethodSpec = MethodSpec.methodBuilder(
                SETUP + simpleName).returns(void.class);
        setupMethodSpec.addStatement("$1T $2N = new $1T()", desiredCN, variableName);
        setupMethodSpec.addStatement("$N.setContext($N)", variableName, contextFieldSpec);
        setupMethodSpec.addCode("\n");
        setupMethodSpec.addStatement("Thread $N = new Thread($N, \"Logback shutdown hook [\" + $N.getName() + \"]\")", hookTreadVariableName, variableName, contextFieldSpec);
        setupMethodSpec.addStatement("addInfo(\"Registering shutdown hook with JVM runtime.\")");

        setupMethodSpec.addStatement("$N.putObject($T.SHUTDOWN_HOOK_THREAD, $N)", contextFieldSpec, CoreConstants.class, hookTreadVariableName);
        setupMethodSpec.addStatement("Runtime.getRuntime().addShutdownHook($N)", hookTreadVariableName);

        return setupMethodSpec;

    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        if (inError) {
            return;
        }
        Object o = mic.peekObject();
        if (o != implicitModelHandlerData) {
            addWarn("The object at the of the stack is not the ImplicitModelHandlerData pushed earlier.");
        } else {
            TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
            tmic.popObject();
            MethodSpec.Builder mMethodBuilder = implicitModelHandlerData.methodSpecBuilder;
            MethodSpec methodSpec = mMethodBuilder.build();
            tmic.tylerConfiguratorTSB.addMethod(methodSpec);
            tmic.configureMethodSpecBuilder.addStatement("$N()", methodSpec);
        }
    }
}
