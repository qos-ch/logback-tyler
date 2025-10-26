package ch.qos.logback.tyler.base.handler;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.boolex.PropertyCondition;
import ch.qos.logback.core.model.AppenderModel;
import ch.qos.logback.core.model.ComponentModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.conditional.ByPropertiesConditionModel;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.SequenceNumberGenerator;

public class ByPropertiesConditionModelHandler extends ComponentModelHandler {

    static int COUNT;

    public ByPropertiesConditionModelHandler(Context context) {
        super(context);
        instanceNum = COUNT++;
    }

    @Override
    String getTargetType() {
        return PropertyCondition.class.getSimpleName();
    }


    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext mic) {
        return new ByPropertiesConditionModelHandler(context);
    }

    @Override
    public void handle(ModelInterpretationContext modelInterpretationContext, Model model) throws ModelHandlerException {
        ByPropertiesConditionModel byPropertiesConditionModel = (ByPropertiesConditionModel) model;
    }

}
