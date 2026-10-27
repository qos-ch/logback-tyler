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

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.model.ContextNameModel;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.tyler.base.ModelToJava;
import ch.qos.logback.tyler.base.TranslationOptions;

/**
 * Extends Tyler with a custom element and replaces one of Tyler's handlers, via the protected
 * extension points of {@link ModelToJava}.
 */
public class CustomModelToJava extends ModelToJava {

    public CustomModelToJava(Context context, TranslationOptions translationOptions) {
        super(context, translationOptions);
    }

    @Override
    protected JoranConfigurator createJoranConfigurator() {
        return new CustomJoranConfigurator();
    }

    @Override
    protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        super.addModelHandlerAssociations(defaultProcessor);
        defaultProcessor.addHandler(MarkerModel.class, TylerMarkerModelHandler::makeInstance);
        defaultProcessor.addHandler(ContextNameModel.class, UpperCaseContextNameModelHandler::makeInstance);
    }
}
