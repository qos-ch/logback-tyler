/*
 * Copyright (c) 2026 QOS.ch Sarl (Switzerland)
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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.model.processor.ConfigurationModelHandlerFull;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
import ch.qos.logback.core.status.testUtil.StatusChecker;
import ch.qos.logback.core.util.OptionHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The scan attribute is stored raw in generated configurators and interpreted
 * when {@code configure()} runs. This test uses the same method the generator emits.
 */
public class ScanAttributeRuntimeTest {

    static final String SCAN_ATTRIBUTE = "${TYLER_UNDEFINED_SCAN_VAR:-yes}";

    @Test
    void unsetVariableUsesDefaultAndAttemptsToSchedule() {
        LoggerContext loggerContext = new LoggerContext();
        RuntimeScanConfigurator configurator = new RuntimeScanConfigurator();

        configurator.configure(loggerContext);

        assertEquals(Boolean.TRUE, configurator.topScan);
        StatusChecker statusChecker = new StatusChecker(loggerContext);
        statusChecker.assertContainsMatch(ConfigurationModelHandlerFull.FAILED_WATCH_PREDICATE_MESSAGE_1);
        statusChecker.assertNoMatch("Registering a new ReconfigureOnChangeTask");
    }

    @Test
    void falseAtRuntimeDoesNotSchedule() {
        LoggerContext loggerContext = new LoggerContext();
        loggerContext.putProperty("TYLER_UNDEFINED_SCAN_VAR", "false");
        RuntimeScanConfigurator configurator = new RuntimeScanConfigurator();

        configurator.configure(loggerContext);

        assertEquals(Boolean.FALSE, configurator.topScan);
        StatusChecker statusChecker = new StatusChecker(loggerContext);
        statusChecker.assertNoMatch("ReconfigureOnChangeTask");
        statusChecker.assertNoMatch(ConfigurationModelHandlerFull.FAILED_WATCH_PREDICATE_MESSAGE_1);
    }

    @Test
    void yesAtRuntimeEnablesScanning() {
        LoggerContext loggerContext = new LoggerContext();
        loggerContext.putProperty("TYLER_UNDEFINED_SCAN_VAR", "yes");
        RuntimeScanConfigurator configurator = new RuntimeScanConfigurator();

        configurator.configure(loggerContext);

        assertEquals(Boolean.TRUE, configurator.topScan);
        StatusChecker statusChecker = new StatusChecker(loggerContext);
        statusChecker.assertContainsMatch(ConfigurationModelHandlerFull.FAILED_WATCH_PREDICATE_MESSAGE_1);
    }

    /**
     * Same control flow the generator emits for a configuration scan attribute.
     */
    static class RuntimeScanConfigurator extends TylerConfiguratorBase implements Configurator {
        Boolean topScan;

        @Override
        public ExecutionStatus configure(LoggerContext loggerContext) {
            setContext(loggerContext);
            this.topScan = toScanBoolean(SCAN_ATTRIBUTE);
            if (Boolean.TRUE.equals(topScan)) {
                ConfigurationModelHandlerFull configurationMHF = new ConfigurationModelHandlerFull(context);
                String scanPeriodStr = subst(null);
                configurationMHF.detachedPostProcess(scanPeriodStr);
            }
            return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
        }

        private Boolean toScanBoolean(String scanAttribute) {
            String scanStr = subst(scanAttribute);
            if (OptionHelper.isNullOrEmptyOrAllSpaces(scanStr)) {
                return null;
            }
            Boolean parsed = OptionHelper.toBooleanObject(scanStr);
            return parsed != null ? parsed : Boolean.TRUE;
        }
    }
}
