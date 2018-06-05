package logger;

import utils.FieldVarType;
import utils.ParamWrapper;
import utils.RandomSupplier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ClazzLogger extends MyLogger {

    private final List<MethodLogger> methods;
    private final MethodLogger main;
    private MethodLogger run;

    public ClazzLogger(MethodLogger main) {
        this.methods = new ArrayList<>();
        this.variables = new HashMap<>();
        this.main = main;
    }

    public MethodLogger getMain() {
        return main;
    }

    public void setRun(MethodLogger run) {
        if (this.run != null) {
            return; //run method can't be changed
        } else {
            this.run = run;
        }
    }

    public MethodLogger getRun() {
        return this.run;
    }

    public void logMethod(MethodLogger ml) {
        ml.addMethodToExcludedForCalling(ml);
        methods.add(ml);
    }


    public List<MethodLogger> getOverloadedMethods(String name) {
        return methods.stream().filter(m -> m.getName().equals(name)).collect(Collectors.toList());
    }

    public MethodLogger getRandomMethod() {
        if (hasMethods()) {
            return methods.get(random.nextInt(methods.size()));
        } else {
            return null;
        }
    }

    public MethodLogger getRandomCallableMethod(MethodLogger callerMethod) {
        List<MethodLogger> callableMethods;
        if (callerMethod.isStatic()) {
            callableMethods = getStaticMethods();
        } else {
            callableMethods = new ArrayList<>(methods);
        }
        //exclude methods that have called the callerMethod and the callerMethod itself
        callableMethods.removeAll(callerMethod.getMethodsExcludedForCalling());
        callableMethods.remove(callerMethod);
        return callableMethods.isEmpty() ? null : callableMethods.get(random.nextInt((callableMethods.size())));
    }

    private List<MethodLogger> getStaticMethods() {
        return methods.stream().filter(m -> m.isStatic()).collect(Collectors.toList());
    }

    /**
     * @return @code{true} if there are logged methods in this clazzLogger, otherwise @code{false}
     */
    public boolean hasMethods() {
        return !methods.isEmpty();
    }

    public ParamWrapper[] getParamValues(FieldVarType[] paramTypes, MethodLogger method) {
        List<ParamWrapper> values = new ArrayList<>();
        for (FieldVarType t : paramTypes) {
            if (random.nextBoolean()) { //add global variable
                if (!addFieldToParamValues(values, method, t)) {
                    //add local variable if no global variable available
                    if (!addLocalVariableToParamValues(values, method, t)) {
                        //add random value if no variables available
                        values.add(new ParamWrapper(RandomSupplier.getRandomValue(t)));
                    }
                }
            } else { //add local variable
                if (!addLocalVariableToParamValues(values, method, t)) {
                    //add global variable if no local variable available
                    if (!addFieldToParamValues(values, method, t)) {
                        //add random value if no variables available
                        values.add(new ParamWrapper(RandomSupplier.getRandomValue(t)));
                    }
                }
            }
        }
        ParamWrapper[] paramValues = new ParamWrapper[values.size()];
        return values.toArray(paramValues);
    }

    private boolean addFieldToParamValues(List<ParamWrapper> values, MethodLogger method, FieldVarType type) {
        FieldVarLogger fvl = this.getInitializedFieldOfTypeUsableInMethod(method, type);
        if (fvl != null) {
            values.add(new ParamWrapper(fvl));
            return true;
        } else {
            return false;
        }
    }

    private boolean addLocalVariableToParamValues(List<ParamWrapper> values, MethodLogger method, FieldVarType type) {
        FieldVarLogger fvl = this.getInitializedLocalVarOfType(method, type);
        if (fvl != null) {
            values.add(new ParamWrapper(fvl));
            return true;
        } else {
            return false;
        }
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

    public FieldVarLogger getGlobalOrLocalVarOfTypeUsableInMethod(MethodLogger method, FieldVarType type) {
        FieldVarLogger l;
        if (random.nextBoolean()) {
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

    public FieldVarLogger getInitializedFieldOfTypeUsableInMethod(MethodLogger method, FieldVarType type) {
        if (method.isStatic()) {
            return this.getVariableWithPredicate(v -> v.isInitialized() && v.isStatic() && v.getType() == type);
        } else {
            return this.getVariableWithPredicate(v -> v.isInitialized() && v.getType() == type);
        }
    }
}
