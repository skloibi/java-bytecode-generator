package at.jku.ssw.java.bytecode.generator.logger;

import at.jku.ssw.java.bytecode.generator.utils.FieldVarType;
import at.jku.ssw.java.bytecode.generator.utils.ParamWrapper;
import at.jku.ssw.java.bytecode.generator.utils.RandomSupplier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClazzLogger extends Logger {

    private final List<MethodLogger> methods;
    private final MethodLogger main;
    private MethodLogger run;

    public ClazzLogger(MethodLogger main) {
        this.methods = new ArrayList<>();
        // only use these if result should be non-deterministic
        // this.methods.add(new MethodLogger("hashCode", Modifier.PUBLIC, FieldVarType.INT, true));
        // this.methods.add(new MethodLogger("toString", Modifier.PUBLIC, FieldVarType.STRING, true));
        this.variables = new HashMap<>();
        this.main = main;
    }

    public MethodLogger getMain() {
        return main;
    }

    public void setRun(MethodLogger run) {
        if (this.run == null) {
            this.run = run;
        }
    }

    public List<MethodLogger> getMethods() {
        return methods;
    }

    public MethodLogger getRun() {
        return this.run;
    }

    public void logMethod(MethodLogger ml) {
        methods.add(ml);
    }

    public List<MethodLogger> getOverloadedMethods(String name) {
        return methods.stream().filter(m -> m.getName().equals(name)).collect(Collectors.toList());
    }

    public MethodLogger getRandomMethod() {
        if (hasMethods()) {
            return methods.get(RANDOM.nextInt(methods.size()));
        } else {
            return null;
        }
    }

    public MethodLogger getRandomCallableMethod(MethodLogger callingMethod) {
        List<MethodLogger> callableMethods = getCallableMethods(callingMethod);
        return callableMethods.isEmpty() ? null : callableMethods.get(RANDOM.nextInt(callableMethods.size()));
    }

    private List<MethodLogger> getCallableMethods(MethodLogger callingMethod) {
        List<MethodLogger> callableMethods;
        if (callingMethod.isStatic()) {
            callableMethods = getStaticMethods();
        } else {
            callableMethods = new ArrayList<>(methods);
        }
        callableMethods.remove(callingMethod);
        removeAllExcludedForCalling(callableMethods, callingMethod.getMethodsExcludedForCalling());
        return callableMethods;
    }

    public MethodLogger getRandomCallableMethodOfType(MethodLogger callingMethod, FieldVarType fieldVarType) {
        List<MethodLogger> callableMethods = getCallableMethods(
                callingMethod).stream().filter(m -> m.getReturnType() == fieldVarType).collect(Collectors.toList());
        return callableMethods.isEmpty() ? null : callableMethods.get(RANDOM.nextInt(callableMethods.size()));
    }

    private void removeAllExcludedForCalling(List<MethodLogger> callableMethods, Set<MethodLogger> excludedForCalling) {
        if (excludedForCalling.isEmpty()) {
            return;
        }
        callableMethods.removeAll(excludedForCalling);
        for (MethodLogger m : excludedForCalling) {
            removeAllExcludedForCalling(callableMethods, m.getMethodsExcludedForCalling());
        }
    }

    private List<MethodLogger> getStaticMethods() {
        return methods.stream().filter(MethodLogger::isStatic).collect(Collectors.toList());
    }

    public boolean hasMethods() {
        return !methods.isEmpty();
    }

    private boolean addFieldToParamValues(List<ParamWrapper> values, MethodLogger method, FieldVarType type) {
        FieldVarLogger fvl = this.getInitializedFieldOfTypeUsableInMethod(method, type);
        if (fvl != null) {
            values.add(new ParamWrapper<>(fvl));
            return true;
        } else {
            return false;
        }
    }

    private boolean addLocalVariableToParamValues(List<ParamWrapper> values, MethodLogger method, FieldVarType type) {
        FieldVarLogger fvl = this.getInitializedLocalVarOfType(method, type);
        if (fvl != null) {
            values.add(new ParamWrapper<>(fvl));
            return true;
        } else {
            return false;
        }
    }

    public ParamWrapper[] getParamValues(FieldVarType[] paramTypes, MethodLogger method) {
        List<ParamWrapper> values = new ArrayList<>();
        for (FieldVarType t : paramTypes) {
            if (RANDOM.nextBoolean()) { //add global variable
                if (!addFieldToParamValues(values, method, t)) {
                    //add local variable if no global variable available
                    if (!addLocalVariableToParamValues(values, method, t)) {
                        //add RANDOM value if no variables available
                        values.add(new ParamWrapper<>(RandomSupplier.getRandomCastedValue(t)));
                    }
                }
            } else { //add local variable
                if (!addLocalVariableToParamValues(values, method, t)) {
                    //add global variable if no local variable available
                    if (!addFieldToParamValues(values, method, t)) {
                        //add RANDOM value if no variables available
                        values.add(new ParamWrapper<>(RandomSupplier.getRandomCastedValue(t)));
                    }
                }
            }
        }
        ParamWrapper[] paramValues = new ParamWrapper[values.size()];
        return values.toArray(paramValues);
    }

    public FieldVarLogger getNonFinalFieldUsableInMethod(MethodLogger method) {
        if (method.isStatic()) {
            return this.getVariableWithPredicate(v -> v.isStatic() && !v.isFinal());
        } else {
            return this.getVariableWithPredicate(v -> !v.isFinal());
        }
    }

    public FieldVarLogger getNonFinalCompatibleFieldUsableInMethod(MethodLogger method, FieldVarType type) {
        if (method.isStatic()) {
            return this.getVariableWithPredicate(v -> v.isStatic() && !
                    v.isFinal() && FieldVarType.getCompatibleTypes(type).contains(v.getType()));
        } else {
            return this.getVariableWithPredicate(v -> !v.isFinal() &&
                    FieldVarType.getCompatibleTypes(type).contains(v.getType()));
        }
    }

    public FieldVarLogger getNonFinalInitializedCompatibleFieldUsableInMethod(MethodLogger method, FieldVarType type) {
        if (method.isStatic()) {
            return this.getVariableWithPredicate(v -> v.isStatic() && v.isInitialized() &&
                    !v.isFinal() && FieldVarType.getCompatibleTypes(type).contains(v.getType()));
        } else {
            return this.getVariableWithPredicate(v -> !v.isFinal() &&
                    v.isInitialized() && FieldVarType.getCompatibleTypes(type).contains(v.getType()));
        }
    }

    public FieldVarLogger getInitializedLocalVarOfType(MethodLogger method, FieldVarType type) {
        return method.getVariableWithPredicate(v -> v.isInitialized() && v.getType() == type);
    }

    public FieldVarLogger getInitializedCompatibleLocalVar(MethodLogger method, FieldVarType type) {
        return method.getVariableWithPredicate(v -> v.isInitialized() &&
                FieldVarType.getCompatibleTypes(type).contains(v.getType()));
    }

    public FieldVarLogger getNonFinalLocalVar(MethodLogger method) {
        return method.getVariableWithPredicate(v -> !v.isFinal());
    }

    public FieldVarLogger getNonFinalCompatibleLocalVar(MethodLogger method, FieldVarType type) {
        return method.getVariableWithPredicate(v -> !v.isFinal() &&
                FieldVarType.getCompatibleTypes(type).contains(v.getType()));
    }

    public FieldVarLogger getInitializedFieldOfTypeUsableInMethod(MethodLogger method, FieldVarType type) {
        if (method.isStatic()) {
            return this.getVariableWithPredicate(v -> v.isInitialized() && v.isStatic() && v.getType() == type);
        } else {
            return this.getVariableWithPredicate(v -> v.isInitialized() && v.getType() == type);
        }
    }

    public FieldVarLogger getGlobalOrLocalVarInitializedOfTypeUsableInMethod(MethodLogger method, FieldVarType type) {
        FieldVarLogger l;
        if (RANDOM.nextBoolean()) {
            l = getInitializedLocalVarOfType(method, type);
            if (l == null) {
                l = getInitializedFieldOfTypeUsableInMethod(method, type);
            }
        } else {
            l = getInitializedFieldOfTypeUsableInMethod(method, type);
            if (l == null) {
                l = getInitializedLocalVarOfType(method, type);
            }
        }
        return l;
    }

    public FieldVarLogger getNonFinalFieldOfTypeUsableInMethod(MethodLogger method, FieldVarType type) {
        if (method.isStatic()) {
            return this.getVariableWithPredicate(v -> v.isStatic() && !v.isFinal() && v.getType() == type);
        } else {
            return this.getVariableWithPredicate(v -> !v.isFinal() && v.getType() == type);
        }
    }

    public FieldVarLogger getNonFinalLocalVarOfType(MethodLogger method, FieldVarType type) {
        return method.getVariableWithPredicate(v -> !v.isFinal() && v.getType() == type);
    }

}