package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * logs Information about a generated class
 */
public class ClazzLogger extends MyLogger {

    private List<MethodLogger> methods;
    private MethodLogger main;

    public ClazzLogger(MethodLogger main) {
        methods = new ArrayList<>();
        variables = new HashMap<>();
        this.main = main;
    }

    /**
     * logs Information about a generated Method
     *
     * @param ml the MethodLogger in which the Information is stored
     */
    public void logMethod(MethodLogger ml) {
        methods.add(ml);
    }

    public MethodLogger getMain() {
        return main;
    }

    /**
     * @param method the logger of the method, which's local variables are returned
     * @return a list of all FieldVarLogger-Objects of the variables and parameters of the Method
     */
    public List<FieldVarLogger> getLocals(MethodLogger method) {
        return method.getVariables();
    }

    /**
     * @return the MethodLogger of a randomly chosen method, that is logged in the clazzLogger
     */
    public MethodLogger getRandomMethod() {
        if (hasMethods()) return methods.get(random.nextInt(methods.size()));
        else return null;
    }


    public MethodLogger getRandomCallableMethod(MethodLogger callerMethod) {
        List<MethodLogger> callableMethods;
        if (callerMethod.isStatic()) callableMethods = getStaticMethods();
        else callableMethods = new ArrayList<>(methods);
        //exclude methods that have called the callerMethod and the callerMethod itself
        callableMethods.removeAll(callerMethod.getMethodsExcludedForCalling());
        callableMethods.remove(callerMethod);
        return callableMethods.isEmpty() ? null : callableMethods.get(random.nextInt((callableMethods.size())));
    }

    private List<MethodLogger> getStaticMethods() {
        if (hasMethods()) {
            return methods.stream().filter(m -> m.isStatic()).collect(Collectors.toList());
        } else return null;
    }

    /**
     * @return @code{true} if there are logged methods in this clazzLogger, otherwise @code{false}
     */
    public boolean hasMethods() {
        return !methods.isEmpty();
    }

    /**
     * @param type the return-type of the randomly choosen method
     * @return the MethodLogger of a randomly chosen method with given return-type
     */
    public MethodLogger getRandomMethodWithReturnTypeUsableInMethod(MethodLogger callerMethod, FieldVarType type) {
        if (hasMethods()) {
            List<MethodLogger> retTypeMethods;
            if (callerMethod.isStatic()) {
                retTypeMethods = methods.stream().filter(
                        m -> m.getReturnType() == type && m.isStatic()).collect(Collectors.toList());
            } else {
                retTypeMethods = methods.stream().filter(
                        m -> m.getReturnType() == type).collect(Collectors.toList());
            }
            retTypeMethods.removeAll(callerMethod.getMethodsExcludedForCalling());
            retTypeMethods.remove(callerMethod);
            return retTypeMethods.isEmpty() ? null : retTypeMethods.get(random.nextInt(retTypeMethods.size()));
        } else return null;
    }

    public ParamWrapper[] getParamValues(FieldVarType[] paramTypes, MethodLogger method) {
        List<ParamWrapper> values = new ArrayList<>();
        for (FieldVarType t : paramTypes) {
            if (random.nextBoolean()) { //add global variable
                if (!addFieldToParamValues(values, method, t)) {
                    //add local variable if no global variable available
                    if (!addLocalVariableToParamValues(values, method, t)) {
                        //add random value if no variables available
                        values.add(new ParamWrapper(RandomSupplier.getRandomValueAsString(t)));
                    }
                }
            } else { //add local variable
                if (!addLocalVariableToParamValues(values, method, t)) {
                    //add global variable if no local variable available
                    if (!addFieldToParamValues(values, method, t)) {
                        //add random value if no variables available
                        values.add(new ParamWrapper(RandomSupplier.getRandomValueAsString(t)));
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
        } else return false;
    }

    private boolean addLocalVariableToParamValues(List<ParamWrapper> values, MethodLogger method, FieldVarType type) {
        FieldVarLogger fvl = this.getInitializedLocalVarOfType(method, type);
        if (fvl != null) {
            values.add(new ParamWrapper(fvl));
            return true;
        } else return false;
    }

    public FieldVarLogger getNonFinalFieldUsableInMethod(MethodLogger method) {
        if (method.isStatic()) {
            return this.getVariableWithPredicate(v -> v.isStatic() && !v.isFinal());
        } else {
            return this.getVariableWithPredicate(v -> !v.isFinal());
        }
    }

    public FieldVarLogger getNonFinalFieldOfTypeUsableInMethod(MethodLogger method, FieldVarType type) {
        if (method.isStatic()) {
            return this.getVariableWithPredicate(v -> v.isStatic() && !v.isFinal() && v.getType() == type);
        } else {
            return this.getVariableWithPredicate(v -> !v.isFinal() && v.getType() == type);
        }
    }

    public FieldVarLogger getInitializedLocalVarOfType(MethodLogger method, FieldVarType type) {
        return method.getVariableWithPredicate(
                v -> v.isInitialized() && v.getType() == type);
    }

    public FieldVarLogger getNonFinalLocalVar(MethodLogger method) {
        return method.getVariableWithPredicate(v -> !v.isFinal());
    }

    public FieldVarLogger getInitializedFieldOfTypeUsableInMethod(MethodLogger method, FieldVarType returnType) {
        if (method.isStatic()) {
            return this.getVariableWithPredicate(
                    v -> v.isInitialized() && v.isStatic() && v.getType() == returnType);
        } else {
            return this.getVariableWithPredicate(
                    v -> v.isInitialized() && v.getType() == returnType);
        }
    }
}
