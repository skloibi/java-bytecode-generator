package logger;

import utils.FieldVarType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

abstract class MyLogger {
    Map<String, FieldVarLogger> variables;
    static Random random = new Random();

    public void logVariable(String name, FieldVarType type, int modifiers, boolean initialized) {
        FieldVarLogger f = new FieldVarLogger(name, modifiers, type, initialized);
        variables.put(name, f);
    }

    public boolean hasVariables() {
        return !variables.isEmpty();
    }

    public FieldVarLogger getVariableWithPredicate(Predicate<FieldVarLogger> predicate) {
        if (!hasVariables()) {
            return null;
        }
        List<FieldVarLogger> predicateVars = getVariablesWithPredicate(predicate);
        if (predicateVars.isEmpty()) {
            return null;
        }
        return predicateVars.get(random.nextInt(predicateVars.size()));
    }

    public List<FieldVarLogger> getVariablesWithPredicate(Predicate<FieldVarLogger> predicate) {
        return variables.values().stream().filter(
                predicate).collect(Collectors.toList());
    }
}