package io.dqengine.example.http;

import io.dqengine.core.domain.RecordFields;
import io.dqengine.core.domain.RuleLogic;
import io.dqengine.core.port.out.RuleLogicCompiler;
import java.util.List;
import java.util.Set;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.DataBindingMethodResolver;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

final class SpelRuleLogicCompiler implements RuleLogicCompiler {

    private static final Set<String> STRING_METHODS = Set.of(
            "length", "isEmpty", "isBlank", "startsWith", "endsWith", "contains", "equals", "equalsIgnoreCase",
            "substring", "trim", "strip", "toUpperCase", "toLowerCase");

    private final SpelExpressionParser parser = new SpelExpressionParser();
    // Shared across threads: read-only, and the record is passed in as the root object.
    private final EvaluationContext context = SimpleEvaluationContext
            .forPropertyAccessors(new RecordFieldAccessor())
            .withMethodResolvers(new StringMethodResolver())
            .build();

    @Override
    public RuleLogic compile(String ruleId, String logic) {
        Expression expression = parser.parseExpression(logic);
        return fields -> expression.getValue(context, fields, String.class);
    }

    // Goes through RecordFields so the engine sees every read.
    private static final class RecordFieldAccessor implements PropertyAccessor {

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class<?>[] {RecordFields.class};
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) {
            return target instanceof RecordFields;
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) {
            return new TypedValue(((RecordFields) target).value(name).orElse(null));
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) {
            return false;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) {
            throw new UnsupportedOperationException("rule logic cannot change a record");
        }
    }

    private static final class StringMethodResolver implements MethodResolver {

        private final MethodResolver delegate = DataBindingMethodResolver.forInstanceMethodInvocation();

        @Override
        public MethodExecutor resolve(EvaluationContext context, Object target, String name,
                List<TypeDescriptor> argumentTypes) throws AccessException {
            if (!(target instanceof String) || !STRING_METHODS.contains(name)) {
                return null;
            }
            return delegate.resolve(context, target, name, argumentTypes);
        }
    }
}
