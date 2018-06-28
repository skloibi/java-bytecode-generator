package generators;

import javassist.*;
import logger.FieldVarLogger;
import logger.MethodLogger;
import utils.*;

import java.util.*;

import static utils.Operator.*;
import static utils.Operator.OpStatKind.*;

class MathGenerator extends MethodCaller {

    private static boolean noDivByZero;
    private static boolean noOverflow;

    private final static Map<String, String> OVERFLOW_METHODS = new HashMap<>();

    private final Set<String> checkForDivByZero = new HashSet<>();
    private final Set<FieldVarLogger> incDecrementOperands = new HashSet<>();

    private static CtClass mathClazz;

    {
        try {
            mathClazz = ClassPool.getDefault().get("java.lang.Math");
        } catch (NotFoundException e) {
            throw new AssertionError(e);
        }
        OVERFLOW_METHODS.put("java.lang.Math.addExact(int,int)", "if(%2$s > 0 ? " +
                "Integer.MAX_VALUE - %2$s > %1$s : Integer.MIN_VALUE - %2$s < %1$s) {");
        OVERFLOW_METHODS.put("java.lang.Math.addExact(long,long)", "if(%2$s > 0 ? " +
                "Long.MAX_VALUE - %2$s > %1$s : Long.MIN_VALUE - %2$s < %1$s) {");
        OVERFLOW_METHODS.put("java.lang.Math.decrementExact(int)", "if( %s > Integer.MIN_VALUE) {");
        OVERFLOW_METHODS.put("java.lang.Math.decrementExact(long)", "if( %s > Long.MIN_VALUE) {");
        OVERFLOW_METHODS.put("java.lang.Math.incrementExact(int)", "if( %s < Integer.MAX_VALUE) {");
        OVERFLOW_METHODS.put("java.lang.Math.incrementExact(long)", "if( %s < Long.MAX_VALUE) {");
        OVERFLOW_METHODS.put("java.lang.Math.negateExact(int)", "if( %s > Integer.MIN_VALUE) {");
        OVERFLOW_METHODS.put("java.lang.Math.negateExact(long)", "if( %s > Long.MIN_VALUE) {");
        OVERFLOW_METHODS.put("java.lang.Math.subtractExact(int,int)", "if(%2$s > 0 ? " +
                "Integer.MAX_VALUE - %2$s < %1$s: Integer.MIN_VALUE - %2$s > %1$s) {");
        OVERFLOW_METHODS.put("java.lang.Math.subtractExact(long,long)", "if(%2$s > 0 ? " +
                "Long.MAX_VALUE - %2$s < %1$s : Long.MIN_VALUE - %2$s > %1$s) {");
        OVERFLOW_METHODS.put("java.lang.Math.toIntExact(long)",
                "if( %1$s <= Integer.MAX_VALUE && %1$s >= Integer.MIN_VALUE) {");
        OVERFLOW_METHODS.put("java.lang.Math.multiplyExact(int,int)",
                "if(%1$s == 0 || Math.abs(Integer.MIN_VALUE/%1$s) > Math.abs(%2$s) && %2$s != Integer.MIN_VALUE) {");
        OVERFLOW_METHODS.put("java.lang.Math.multiplyExact(long,int)", "" +
                "if(%1$s == 0 || Math.abs(Long.MIN_VALUE/%1$s) > Math.abs(%2$s) && %2$s != Integer.MIN_VALUE) {");
        OVERFLOW_METHODS.put("java.lang.Math.multiplyExact(long,long)",
                "if(%1$s == 0 || Math.abs(Long.MIN_VALUE/%1$s) > Math.abs(%2$s) && %2$s != Long.MIN_VALUE) {");
        String modDivCondition = "if(%s != 0) {";
        OVERFLOW_METHODS.put("java.lang.Math.floorDiv(int,int)", modDivCondition);
        OVERFLOW_METHODS.put("java.lang.Math.floorDiv(long,int)", modDivCondition);
        OVERFLOW_METHODS.put("java.lang.Math.floorDiv(long,long)", modDivCondition);
        OVERFLOW_METHODS.put("java.lang.Math.floorMod(int,int)", modDivCondition);
        OVERFLOW_METHODS.put("java.lang.Math.floorMod(long,int)", modDivCondition);
        OVERFLOW_METHODS.put("java.lang.Math.floorMod(long,long)", modDivCondition);
    }

    public MathGenerator(ClazzFileContainer cf, boolean noOverflow, boolean noDivByZero) {
        super(cf);
        this.noOverflow = noOverflow;
        this.noDivByZero = noDivByZero;
    }

    //===============================================CALL MATH METHODS==================================================

    public void generateMathMethodCall(MethodLogger method) {
        String callString = srcGenerateMathMethodCall(method);
        insertIntoMethodBody(method, callString);
    }

    public String srcGenerateMathMethodCall(MethodLogger method) {
        CtMethod mathMethod = getMathMethod();
        String methodName = mathMethod.getName();
        String signature = mathMethod.getSignature();
        FieldVarType[] paramTypes = getParamTypes(signature);
        ParamWrapper[] paramValues = getClazzLogger().getParamValues(paramTypes, method);
        if (OVERFLOW_METHODS.containsKey(mathMethod.getLongName()) && (noOverflow || noDivByZero)) {
            String noExceptionIf = getNoExceptionIf(mathMethod.getLongName(), paramValues, paramTypes);
            if (noExceptionIf == null) {
                return null;
            }
            String src = noExceptionIf + "Math." +
                    this.generateMethodCallString(methodName, paramTypes, paramValues) + "}";
            return src;
        } else {
            return "Math." + this.generateMethodCallString(methodName, paramTypes, paramValues);
        }
    }

    public void setFieldToMathReturnValue(MethodLogger method) {
        String src = srcSetFieldToMathReturnValue(method);
        insertIntoMethodBody(method, src);
    }

    public String srcSetFieldToMathReturnValue(MethodLogger method) {
        CtMethod mathMethod = getMathMethod();
        String signature = mathMethod.getSignature();
        FieldVarType returnType = getType(signature.charAt(signature.length() - 1));
        if (this.getClazzLogger().hasVariables()) {
            FieldVarLogger fieldVar = this.getClazzLogger().getNonFinalCompatibleFieldUsableInMethod(method, returnType);
            if (fieldVar == null) {
                return null;
            }
            return srcSetVariableToMathReturnValue(mathMethod, method, fieldVar);
        } else {
            return null;
        }
    }

    private String srcSetVariableToMathReturnValue(CtMethod mathMethod, MethodLogger method, FieldVarLogger fieldVar) {
        FieldVarType[] paramTypes = getParamTypes(mathMethod.getSignature());
        ParamWrapper[] paramValues = getClazzLogger().getParamValues(paramTypes, method);
        if (OVERFLOW_METHODS.containsKey(mathMethod.getLongName()) && noOverflow) {
            String noOverFlowIf = getNoExceptionIf(mathMethod.getLongName(), paramValues, paramTypes);
            if (noOverFlowIf == null) return null;
            String src = noOverFlowIf + fieldVar.getName() + " = " + "Math." +
                    this.generateMethodCallString(mathMethod.getName(), paramTypes, paramValues) + "}";
            return src;
        } else return fieldVar.getName() + " = " + "Math." +
                this.generateMethodCallString(mathMethod.getName(), paramTypes, paramValues);
    }

    public void setLocalVarToMathReturnValue(MethodLogger method) {
        String src = srcSetLocalVarToMathReturnValue(method);
        insertIntoMethodBody(method, src);
    }

    public String srcSetLocalVarToMathReturnValue(MethodLogger method) {
        CtMethod mathMethod = getMathMethod();
        String signature = mathMethod.getSignature();
        FieldVarType returnType = getType(signature.charAt(signature.length() - 1));
        if (method.hasVariables()) {
            FieldVarLogger fieldVar = this.getClazzLogger().getNonFinalCompatibleLocalVar(method, returnType);
            if (fieldVar == null) {
                return null;
            }
            return srcSetVariableToMathReturnValue(mathMethod, method, fieldVar);
        } else {
            return null;
        }
    }

    //=============================================OPERATOR STATEMENTS==================================================

    public void generateOperatorStatement(MethodLogger method, int maxOperations, OpStatKind opStatKind) {
        String src = srcGenerateOperatorStatement(method, maxOperations, opStatKind, false);
        if (src != null) {
            insertIntoMethodBody(method, src);
        }
    }

    public String srcGenerateOperatorStatement(MethodLogger method, int maxOperations, OpStatKind opStatKind) {
        return srcGenerateOperatorStatement(method, maxOperations, opStatKind, false);
    }

    public String srcGenerateOperatorStatement(MethodLogger method, int maxOperations, OpStatKind opStatKind, boolean useNoVars) {
        int numberOfOperands = 2 + ((maxOperations > 1) ? RANDOM.nextInt(maxOperations - 1) : 0);
        StringBuilder src = new StringBuilder();
        switch (opStatKind) {
            case ARITHMETIC:
            case LOGICAL:
            case BITWISE:
                src = srcGenerateOperatorStatementOfKind(method, numberOfOperands, opStatKind, useNoVars);
                break;
            case ARITHMETIC_BITWISE:
                src = arithmeticBitwiseStatement(method, numberOfOperands, useNoVars);
                break;
            case ARITHMETIC_LOGICAL:
                src = combinedWithLogicalStatement(ARITHMETIC, method, numberOfOperands, useNoVars);
                break;
            case BITWISE_LOGICAL:
                src = combinedWithLogicalStatement(BITWISE, method, numberOfOperands, useNoVars);
                break;
            case ARITHMETIC_LOGICAL_BITWISE:
                src = combinedWithLogicalStatement(ARITHMETIC_BITWISE, method, numberOfOperands, useNoVars);
        }
        if (!checkForDivByZero.isEmpty()) {
            src = addIfToOperatorStatement(src, checkForDivByZero);
            checkForDivByZero.clear();
        }
        incDecrementOperands.clear();
        return src.toString();
    }

    public void setLocalVarToOperatorStatement(MethodLogger method, int maxOperations, OpStatKind opStatKind) {
        String src = srcSetLocalVarToOperatorStatement(method, maxOperations, opStatKind);
        if (src != null) {
            insertIntoMethodBody(method, src);
        }
    }

    public String srcSetLocalVarToOperatorStatement(MethodLogger method, int maxOperations, OpStatKind opStatKind) {
        FieldVarLogger f = fetchLocalAssignVarForOperandStatement(method, opStatKind);
        if (f == null) {
            return null;
        }
        StringBuilder src = new StringBuilder(srcGenerateOperatorStatement(method, maxOperations, opStatKind, false));
        if (src.indexOf("if") != -1) {
            src.insert(src.indexOf("{") + 1, f.getName() + " = (" + f.getType() + ") (");
        } else {
            src.insert(0, f.getName() + " = (" + f.getType() + ") (");
        }
        src.insert(src.indexOf(";"), ")");
        return src.toString();
    }

    public void setFieldToOperatorStatement(MethodLogger method, int maxOperations, OpStatKind opStatKind) {
        String src = srcSetFieldToOperatorStatement(method, maxOperations, opStatKind);
        if (src != null) {
            insertIntoMethodBody(method, src);
        }
    }

    public String srcSetFieldToOperatorStatement(MethodLogger method, int maxOperations, OpStatKind opStatKind) {
        FieldVarLogger f = fetchGlobalAssignVarForOperandStatement(method, opStatKind);
        if (f == null) {
            return null;
        }
        StringBuilder src = new StringBuilder(srcGenerateOperatorStatement(method, maxOperations, opStatKind, false));
        if (src.indexOf("if") != -1) {
            src.insert(src.indexOf("{") + 1, f.getName() + " = (" + f.getType() + ") (");
        } else {
            src.insert(0, f.getName() + " = (" + f.getType() + ") (");
        }
        src.insert(src.indexOf(";"), ")");
        return src.toString();
    }

    //================================================UTILITY===========================================================

    private static CtMethod getMathMethod() {
        CtMethod[] methods = mathClazz.getDeclaredMethods();
        methods = Arrays.stream(methods).filter(m -> (m.getModifiers() & Modifier.PUBLIC) == 1).toArray(CtMethod[]::new);
        Random random = new Random();
        return methods[random.nextInt(methods.length)];
    }

    private static String getNoExceptionIf(String longName, ParamWrapper[] paramValues, FieldVarType[] paramTypes) {
        String[] params = new String[2];
        params[0] = paramToCorrectStringFormat(paramTypes[0], paramValues[0]);
        if (paramTypes.length == 2) {
            params[1] = paramToCorrectStringFormat(paramTypes[1], paramValues[1]);
        }
        if (noOverflow) {
            switch (longName) {
                case "java.lang.Math.addExact(int,int)":
                case "java.lang.Math.addExact(long,long)":
                case "java.lang.Math.subtractExact(int,int)":
                case "java.lang.Math.subtractExact(long,long)":
                    return String.format(OVERFLOW_METHODS.get(longName), params[1], params[0]);
                case "java.lang.Math.decrementExact(int)":
                case "java.lang.Math.decrementExact(long)":
                case "java.lang.Math.incrementExact(int)":
                case "java.lang.Math.incrementExact(long)":
                case "java.lang.Math.negateExact(int)":
                case "java.lang.Math.negateExact(long)":
                case "java.lang.Math.toIntExact(long)":
                    return String.format(OVERFLOW_METHODS.get(longName), params[0]);
                case "java.lang.Math.multiplyExact(int,int)":
                case "java.lang.Math.multiplyExact(long,int)":
                case "java.lang.Math.multiplyExact(long,long)":
                    return String.format(OVERFLOW_METHODS.get(longName), params[0], params[1]);
            }
        }
        if (noDivByZero) {
            switch (longName) {
                case "java.lang.Math.floorDiv(int,int)":
                case "java.lang.Math.floorDiv(long,int)":
                case "java.lang.Math.floorMod(int,int)":
                case "java.lang.Math.floorMod(long,int)":
                case "java.lang.Math.floorDiv(long,long)":
                case "java.lang.Math.floorMod(long,long)":
                    return String.format(OVERFLOW_METHODS.get(longName), params[1]);
            }
        }
        return null;
    }

    private static FieldVarType getType(char t) {
        switch (t) {
            case 'D':
                return FieldVarType.DOUBLE;
            case 'I':
                return FieldVarType.INT;
            case 'F':
                return FieldVarType.FLOAT;
            case 'J':
                return FieldVarType.LONG;
            default:
                return null;
        }
    }

    private static FieldVarType[] getParamTypes(String methodSignature) {
        List<FieldVarType> paramTypes = new ArrayList<>();
        for (int i = 1; i < methodSignature.length() - 2; i++) {
            paramTypes.add(getType(methodSignature.charAt(i)));
        }
        FieldVarType[] paramTypesArray = new FieldVarType[paramTypes.size()];
        return paramTypes.toArray(paramTypesArray);
    }


    private FieldVarLogger fetchLocalAssignVarForOperandStatement(MethodLogger method, OpStatKind opStatKind) {
        FieldVarType type = fetchAssignVarTypeForOperandStatement(opStatKind);
        return this.getClazzLogger().getNonFinalLocalVarOfType(method, type);
    }

    private FieldVarLogger fetchGlobalAssignVarForOperandStatement(MethodLogger method, OpStatKind opStatKind) {
        FieldVarType type = fetchAssignVarTypeForOperandStatement(opStatKind);
        return this.getClazzLogger().getNonFinalFieldOfTypeUsableInMethod(method, type);
    }

    private FieldVarType fetchAssignVarTypeForOperandStatement(OpStatKind opStatKind) {
        List<FieldVarType> types = new ArrayList<>();
        switch (opStatKind) {
            case LOGICAL:
                types.add(FieldVarType.BOOLEAN);
                break;
            case ARITHMETIC_LOGICAL:
            case BITWISE_LOGICAL:
            case ARITHMETIC_LOGICAL_BITWISE:
                types.add(FieldVarType.BOOLEAN);
            case BITWISE:
            case ARITHMETIC_BITWISE:
            case ARITHMETIC:
                types.addAll(FieldVarType.getNumericTypes());
        }
        return types.get(RANDOM.nextInt(types.size()));
    }

    private StringBuilder arithmeticBitwiseStatement(MethodLogger method, int numberOfOperands, boolean useNoVars) {
        StringBuilder src = new StringBuilder();
        int maxPartitionSize = numberOfOperands / 2;
        Operator operator = null;
        while (numberOfOperands > 0) {
            int operandsInPartition = 1 + ((maxPartitionSize > 1) ? RANDOM.nextInt(maxPartitionSize - 1) : 0);
            StringBuilder statement;
            FieldVarType type;
            if (RANDOM.nextBoolean()) {
                statement = srcGenerateOperatorStatementOfKind(method, operandsInPartition, ARITHMETIC, useNoVars);
                operator = getNonDivNonUnaryArithmeticOperator();
            } else {
                statement = srcGenerateOperatorStatementOfKind(method, operandsInPartition, BITWISE, useNoVars);
                operator = getOperator(BITWISE, true);
            }
            type = getOperandType(BITWISE);
            statement.insert(0, "(");
            statement.replace(statement.indexOf(";"), statement.indexOf(";") + 1, ")");
            statement.insert(0, "(" + type + ")");
            statement.append(operator);
            src.append(statement);
            numberOfOperands -= operandsInPartition;
        }
        src.delete(src.length() - operator.toString().length(), src.length());
        src.append(";");
        return src;
    }

    private StringBuilder combinedWithLogicalStatement(OpStatKind bitAndOrArithmetic, MethodLogger method, int numberOfOperands, boolean useNoVars) {
        StringBuilder src = new StringBuilder();
        List<Operator> relOperators = Operator.getOperatorsOfKind(RELATIONAL);
        int maxPartitionSize = numberOfOperands / 2;
        boolean openRel = false;
        Operator operator = null;
        while (numberOfOperands > 0 || openRel) {
            int operandsInPartition = 1 + ((maxPartitionSize > 1) ? RANDOM.nextInt(maxPartitionSize - 1) : 0);
            StringBuilder statement = new StringBuilder();
            if ((RANDOM.nextBoolean() && !openRel)) {
                statement.append("(");
                statement.append(srcGenerateOperatorStatementOfKind(method, operandsInPartition, LOGICAL, useNoVars));
                statement.replace(statement.indexOf(";"), statement.indexOf(";") + 1, ")");
                operator = getOperator(LOGICAL, true);
            } else {
                if (!openRel) {
                    src.append("(");
                }
                statement.append("(");
                if (bitAndOrArithmetic == ARITHMETIC_BITWISE) {
                    statement.append(arithmeticBitwiseStatement(method, operandsInPartition, useNoVars));
                } else {
                    statement.append(srcGenerateOperatorStatementOfKind(method, operandsInPartition, bitAndOrArithmetic, useNoVars));
                }
                openRel = !openRel;
                statement.replace(statement.indexOf(";"), statement.indexOf(";") + 1, ")");
                if (openRel) {
                    operator = relOperators.get(RANDOM.nextInt(relOperators.size()));
                } else {
                    operator = getOperator(LOGICAL, true);
                    statement.append(")");
                }
            }
            src.append(statement);
            src.append(operator);
            numberOfOperands -= operandsInPartition;
        }
        src.delete(src.length() - operator.toString().length(), src.length());
        src.append(";");
        return src;
    }

    private StringBuilder srcGenerateOperatorStatementOfKind(MethodLogger method, int nbrOfOperands, OpStatKind opStatKind, boolean useNoVars) {
        Operator operator = null;
        StringBuilder operatorStatement = new StringBuilder();
        boolean useNonUnary;
        FieldVarLogger f = null;
        boolean addToCheckForDivByZero = false;
        for (int i = 0; i < nbrOfOperands; i++) {
            useNonUnary = false;
            if (!useNoVars) {
                f = fetchOperand(method, opStatKind);
            }
            String operand;
            FieldVarType type;
            if (f == null || (operator == DIV || operator == MOD) && incDecrementOperands.contains(f)) {
                type = getOperandType(opStatKind);
                if (type == FieldVarType.BOOLEAN) {
                    operand = RandomSupplier.getRandomCastedValue(type);
                } else if (operator == DIV || operator == MOD) {
                    operand = RandomSupplier.getRandomNumericValue(type, true);
                } else {
                    operand = RandomSupplier.getRandomNumericValue(type, false);
                }

                if (!(operand.equals("true") || operand.equals("false"))) {
                    useNonUnary = true;
                }
                addToCheckForDivByZero = false;
            } else {
                operand = f.getName();
                if (f.isFinal() || (operator == MOD || operator == DIV)) {
                    useNonUnary = true;
                }
                if (addToCheckForDivByZero) {
                    checkForDivByZero.add(operand);
                    addToCheckForDivByZero = false;
                }
            }
            operator = getOperator(opStatKind, useNonUnary);
            if (operator.isUnary()) {
                if (f != null && (operator == PLUS_PLUS || operator == MINUS_MINUS)) {
                    incDecrementOperands.add(f);
                    if (RANDOM.nextBoolean()) {
                        operatorStatement.append(operand + operator);
                    } else {
                        operatorStatement.append(operator + operand);
                    }
                } else {
                    operatorStatement.append(operator + operand);
                }
                operator = getOperator(opStatKind, true);
                operatorStatement.append(operator);
            } else {
                operatorStatement.append(operand + operator);
            }
            if (noDivByZero && (operator == MOD || operator == DIV)) {
                addToCheckForDivByZero = true;
            }
        }
        operatorStatement.delete(operatorStatement.length() - operator.toString().length(), operatorStatement.length());
        operatorStatement.append(";");
        return operatorStatement;
    }

    private static StringBuilder addIfToOperatorStatement(StringBuilder statement, Set<String> checkForDivByZero) {
        String[] values = checkForDivByZero.toArray(new String[0]);
        StringBuilder ifStatement = new StringBuilder("if(");
        ifStatement.append(values[0] + UNEQ + "0");
        for (int i = 1; i < checkForDivByZero.size(); i++) {
            ifStatement.append(COND_AND + values[i] + UNEQ + "0");
        }
        ifStatement.append(") {");
        statement.insert(0, ifStatement);
        statement.append("}");
        return statement;
    }

    private static Operator getOperator(OpStatKind opStatKind, boolean nonUnary) {
        List<Operator> operators;
        if (nonUnary) {
            operators = Operator.getNonUnaryOperatorsOfKind(opStatKind);
        } else {
            operators = Operator.getOperatorsOfKind(opStatKind);
        }
        return operators.get(RANDOM.nextInt(operators.size()));
    }

    private static Operator getNonDivNonUnaryArithmeticOperator() {
        List<Operator> operators = Operator.getNonUnaryOperatorsOfKind(ARITHMETIC);
        operators.remove(MOD);
        operators.remove(DIV);
        return operators.get(RANDOM.nextInt(operators.size()));
    }

    private FieldVarType getOperandType(OpStatKind opStatKind) {
        List<FieldVarType> types = new ArrayList<>();
        switch (opStatKind) {
            case LOGICAL:
                types.add(FieldVarType.BOOLEAN);
                break;
            case ARITHMETIC:
                types = new ArrayList<>(FieldVarType.getNumericTypes());
                break;
            case BITWISE:
                types = new ArrayList<>(FieldVarType.getNumericTypes());
                types.remove(FieldVarType.FLOAT);
                types.remove(FieldVarType.DOUBLE);
                types.remove(FieldVarType.LONG);
                break;
        }
        return types.get(RANDOM.nextInt(types.size()));
    }

    private FieldVarLogger fetchOperand(MethodLogger method, OpStatKind opStatKind) {
        FieldVarType type = getOperandType(opStatKind);
        return this.getClazzLogger().getGlobalOrLocalVarInitializedOfTypeUsableInMethod(method, type);
    }

}