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

package ch.qos.logback.tyler.base.generated;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.model.IncludeModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.IncludeModelHandler;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.testUtil.StringListAppender;
import java.lang.Override;
import java.lang.String;


class IncludeTylerConfigurator extends TylerConfiguratorBase implements Configurator {
    /**
     * <p>This method performs configuration per {@link Configurator} interface.</p>
     *
     * <p>If <code>TylerConfigurator</code> is installed as a configurator service, this
     * method will be called by logback-classic during initialization.</p>
     */
    @Override
    public Configurator.ExecutionStatus configure(LoggerContext loggerContext) {
        setContext(loggerContext);
        Appender appenderLIST = setupAppenderLIST();
        propertyModelHandlerHelper.handlePropertyModel(this, "JO_PREFIX", "src/test/input/joran", "", "", "");
        include("${JO_PREFIX}/included.xml", null, null, null);
        Logger logger_ROOT = setupLogger("ROOT", "debug", null);
        logger_ROOT.addAppender(appenderLIST);
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

    Appender setupAppenderLIST() {
        StringListAppender appenderLIST = new StringListAppender();
        appenderLIST.setContext(context);
        appenderLIST.setName("LIST");

        // Configure component of type PatternLayout
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(context);
        patternLayout.setPattern("%msg");
        // ===========no parent setter
        patternLayout.start();
        // Inject component of type PatternLayout into parent
        appenderLIST.setLayout(patternLayout);

        appenderLIST.start();
        return appenderLIST;
    }

    private void include(String fileStr, String urlStr, String resourceStr, String optionalStr) {
        IncludeModel includeModel = new IncludeModel();
        includeModel.setFile(subst(fileStr));
        includeModel.setUrl(subst(urlStr));
        includeModel.setResource(subst(resourceStr));
        includeModel.setOptional(subst(optionalStr));
        IncludeModelHandler includeModelHandler = new IncludeModelHandler(context);
        try {
            Model modelFromIncludedFile = includeModelHandler.buildModelFromIncludedFile(this, includeModel);
            processModelFromIncludedFile(modelFromIncludedFile);
        } catch(ModelHandlerException e) {
            addError("Failed to process IncludeModelHandler", e);
        }
    }
}
// 22:12:21,610 |-INFO in ch.qos.logback.core.model.processor.DefaultProcessor@560348e6 - End of configuration.
