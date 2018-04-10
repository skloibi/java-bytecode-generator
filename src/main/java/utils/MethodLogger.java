package utils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * stores information about a Method
 */
public class MethodLogger extends MyLogger {

    private String name;

    private int modifiers;

    private List<FieldVarLogger> params;


    private FieldVarType returnType;

    public MethodLogger(String name, int modifiers, FieldVarType returnType) {
        this.name = name;
        this.modifiers = modifiers;
        this.returnType = returnType;
        this.params = new ArrayList<>();
        variables = new HashMap<>();
    }

    /**
     * logs a Parameter of the logged method
     *
     * @param paramName
     * @param type
     */
    public void logParam(String paramName, FieldVarType type) {
        FieldVarLogger l = new FieldVarLogger(paramName, modifiers, type, false);
        params.add(l);
    }

    /**
     * @return the name of the logged method
     */
    public String getName() {
        return name;
    }

    /**
     * @return @code{true} if the logged method is Static
     */
    public boolean isStatic() {
        return (modifiers & Modifier.STATIC) != 0;
    }

    public FieldVarType[] getParamsTypes() {
        FieldVarType[] types = new FieldVarType[params.size()];
        int i = 0;
        for (FieldVarLogger l : params) {
            types[i] = l.getType();
            i++;
        }
        return types;
    }

    public FieldVarType getReturnType() {
        return returnType;
    }

    public void setReturnType(FieldVarType returnType) {
        this.returnType = returnType;
    }
}
