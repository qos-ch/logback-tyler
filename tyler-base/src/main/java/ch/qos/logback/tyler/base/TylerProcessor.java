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
 *
 */


package ch.qos.logback.tyler.base;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.AppenderModel;
import ch.qos.logback.core.model.AppenderRefModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.model.processor.ModelFilter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.classic.model.LoggerModel;
import ch.qos.logback.classic.model.RootLoggerModel;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Processor emitting code in two phases, mirroring the two phases of {@link DefaultProcessor} in Joran.
 *
 * <p>In the first phase, models are handled in document order, except for models processed in the
 * second phase by Joran, i.e. appenders, loggers, the root logger and appender references. These are
 * deferred together with their guard, i.e. the conditions enclosing them, see
 * {@link TylerModelInterpretationContext#currentGuards()}.</p>
 *
 * <p>In the second phase, deferred models are handled with their submodels: first appenders, ordered
 * so that an appender is set up after the appenders it references, then loggers and the root logger in
 * document order, then any remaining deferred models. The code emitted for a deferred model with a
 * guard is enclosed in an <code>if</code> statement on that guard. Consecutive models with the same
 * guard share the same <code>if</code> statement. While a deferred model is handled, its guard is
 * pushed again, so that handlers see the same conditions as in the first phase.</p>
 *
 * <p>Consequently, properties defined anywhere in the configuration are set before loggers and
 * appenders, and appenders may be referenced before they are declared.</p>
 *
 * @since 1.0.6
 */
public class TylerProcessor extends DefaultProcessor {

    /**
     * @param guards the conditions enclosing the model, outermost first
     */
    record DeferredModel(Model model, List<CodeBlock> guards) {
    }

    final TylerModelInterpretationContext tmic;
    final List<DeferredModel> deferredModels = new ArrayList<>();
    boolean deferring = false;

    public TylerProcessor(Context context, TylerModelInterpretationContext tmic) {
        super(context, tmic);
        this.tmic = tmic;
    }

    /**
     * @throws IllegalStateException if appenders reference each other in a cycle
     */
    @Override
    public void process(Model topModel) {
        if (topModel == null) {
            addError("Expecting non null model to process");
            return;
        }
        // models not allowed in the first phase are handled in the second
        getPhaseOneFilter().denyAll();
        getPhaseTwoFilter().allowAll();

        mic.pushObject(context);

        deferring = true;
        tmic.setDeferralFilter(getPhaseOneFilter());
        mainTraverse(topModel, getPhaseOneFilter());
        deferring = false;
        tmic.setDeferralFilter(null);

        declareAppenders();
        emitDeferredModels(orderDeferredModels());

        addInfo("End of configuration.");
        mic.popObject();
    }

    @Override
    protected int mainTraverse(Model model, ModelFilter modelFilter) {
        if (deferring && modelFilter.decide(model) == FilterReply.DENY) {
            deferredModels.add(new DeferredModel(model, tmic.currentGuards()));
            return 0;
        }
        return super.mainTraverse(model, modelFilter);
    }

    /**
     * Make the appenders and their guards known to handlers of the second phase, see
     * {@link TylerModelInterpretationContext#getAppenderAvailability(String)}.
     */
    void declareAppenders() {
        for (DeferredModel deferredModel : deferredModels) {
            if (deferredModel.model() instanceof AppenderModel appenderModel) {
                tmic.declareAppender(tmic.subst(appenderModel.getName()), deferredModel.guards());
            }
        }
    }

    List<DeferredModel> orderDeferredModels() {
        List<DeferredModel> appenders = new ArrayList<>();
        List<DeferredModel> loggers = new ArrayList<>();
        List<DeferredModel> others = new ArrayList<>();
        for (DeferredModel deferredModel : deferredModels) {
            Model model = deferredModel.model();
            if (model instanceof AppenderModel) {
                appenders.add(deferredModel);
            } else if (model instanceof LoggerModel || model instanceof RootLoggerModel) {
                loggers.add(deferredModel);
            } else {
                others.add(deferredModel);
            }
        }

        List<DeferredModel> result = new AppenderSorter(appenders).sort();
        result.addAll(loggers);
        result.addAll(others);
        return result;
    }

    void emitDeferredModels(List<DeferredModel> orderedModels) {
        MethodSpec.Builder configureMSB = tmic.getConfigureMethodSpecBuilder();
        List<CodeBlock> openGuards = List.of();
        for (DeferredModel deferredModel : orderedModels) {
            List<CodeBlock> guards = deferredModel.guards();
            if (!guards.equals(openGuards)) {
                if (!openGuards.isEmpty()) {
                    configureMSB.endControlFlow();
                }
                if (!guards.isEmpty()) {
                    configureMSB.beginControlFlow("if ($L)", CodeBlock.join(guards, " && "));
                }
                openGuards = guards;
            }
            guards.forEach(tmic::pushGuard);
            mainTraverse(deferredModel.model(), getPhaseTwoFilter());
            guards.forEach(guard -> tmic.popGuard());
        }
        if (!openGuards.isEmpty()) {
            configureMSB.endControlFlow();
        }
    }

    /**
     * Orders appenders so that each appender comes after the appenders it references. Otherwise, document
     * order is preserved. References to undeclared appenders are ignored.
     */
    class AppenderSorter {

        final List<DeferredModel> appenders;
        final Map<String, List<DeferredModel>> appendersByName = new LinkedHashMap<>();
        final Map<Model, Boolean> doneMap = new IdentityHashMap<>();
        final List<DeferredModel> path = new ArrayList<>();
        final List<DeferredModel> result = new ArrayList<>();

        AppenderSorter(List<DeferredModel> appenders) {
            this.appenders = appenders;
            for (DeferredModel appender : appenders) {
                appendersByName.computeIfAbsent(nameOf(appender), k -> new ArrayList<>()).add(appender);
            }
        }

        List<DeferredModel> sort() {
            appenders.forEach(this::visit);
            return result;
        }

        void visit(DeferredModel appender) {
            Boolean done = doneMap.get(appender.model());
            if (Boolean.TRUE.equals(done)) {
                return;
            }
            if (Boolean.FALSE.equals(done)) {
                throw new IllegalStateException("Appender reference cycle: " + describeCycle(appender));
            }

            doneMap.put(appender.model(), Boolean.FALSE);
            path.add(appender);
            List<String> references = new ArrayList<>();
            collectReferences(appender.model(), references);
            for (String reference : references) {
                appendersByName.getOrDefault(reference, List.of()).forEach(this::visit);
            }
            path.remove(path.size() - 1);
            doneMap.put(appender.model(), Boolean.TRUE);
            result.add(appender);
        }

        String describeCycle(DeferredModel appender) {
            int start = 0;
            while (path.get(start) != appender) {
                start++;
            }
            List<String> names = path.subList(start, path.size()).stream().map(this::nameOf).collect(Collectors.toCollection(ArrayList::new));
            names.add(nameOf(appender));
            return String.join(" -> ", names);
        }

        void collectReferences(Model model, List<String> references) {
            for (Model submodel : model.getSubModels()) {
                if (submodel instanceof AppenderRefModel appenderRefModel) {
                    references.add(tmic.subst(appenderRefModel.getRef()));
                }
                collectReferences(submodel, references);
            }
        }

        String nameOf(DeferredModel appender) {
            return tmic.subst(((AppenderModel) appender.model()).getName());
        }
    }
}
