package generators;

import javassist.CannotCompileException;
import javassist.CtMethod;
import logger.MethodLogger;
import utils.RandomSupplier;

import java.util.LinkedList;

public class ControlFlowGenerator extends Generator {
    private class IfContext {
        int numberOfElseIf;
        boolean hasElse;
        int deepness;

        public IfContext(int deepness) {
            this.deepness = deepness;
            hasElse = false;
            numberOfElseIf = 0;
        }
    }

    private enum ControlType {
        ifType,
        elseType,
        forWhileType,
        doWhileType
    }

    private final LinkedList<IfContext> openIfContexts = new LinkedList<>();
    private final StringBuilder controlSrc = new StringBuilder();
    private int deepness = 0;
    private final int ifBranchingFactor;
    private final int maxLoopIterations;
    private final RandomCodeGenerator randomCodeGenerator;
    private final MathGenerator mathGenerator;

    public ControlFlowGenerator(RandomCodeGenerator randomCodeGenerator, MathGenerator mathGenerator) {
        super(randomCodeGenerator.getClazzFileContainer());
        this.randomCodeGenerator = randomCodeGenerator;
        this.ifBranchingFactor = randomCodeGenerator.getController().getIfBranchingFactor();
        this.maxLoopIterations = randomCodeGenerator.getController().getMaxLoopIterations();
        this.mathGenerator = mathGenerator;
    }

    //==========================================IF ELSEIF ELSE==========================================================

    public void generateIfElseStatement(MethodLogger contextMethod) {
        if (openIfContexts.size() != 0 && deepness == openIfContexts.getLast().deepness) {
            switch (RANDOM.nextInt(3)) {
                case 0:
                    if (openIfContexts.getLast().hasElse == false &&
                            openIfContexts.getLast().numberOfElseIf < ifBranchingFactor) {
                        openElseIfStatement(contextMethod);
                        this.generateBody(contextMethod, ControlType.elseType);
                    }
                    break;
                case 1:
                    if (openIfContexts.getLast().hasElse == false) {
                        openElseStatement();
                        this.generateBody(contextMethod, ControlType.elseType);
                    }
                    break;
                case 2:
                    openIfStatement(contextMethod);
                    this.generateBody(contextMethod, ControlType.ifType);
            }
        } else {
            this.openIfStatement(contextMethod);
            this.generateBody(contextMethod, ControlType.ifType);
        }
    }

    private void openIfStatement(MethodLogger contextMethod) {
        controlSrc.append("if(" + getIfCondition(contextMethod) + ") {");
        deepness++;
        IfContext c = new IfContext(deepness);
        openIfContexts.add(c);
    }

    private void openElseStatement() {
        controlSrc.append("} else {");
        openIfContexts.getLast().hasElse = true;
    }

    private void openElseIfStatement(MethodLogger contextMethod) {
        openIfContexts.getLast().numberOfElseIf++;
        controlSrc.append("} else if(" + getIfCondition(contextMethod) + " ) {");
    }

    private void closeIFStatement() {
        controlSrc.append("}");
        openIfContexts.removeLast();
        deepness--;
    }

    private String getIfCondition(MethodLogger method) {
        MathGenerator.OpStatKind condKind = null;
        switch (RANDOM.nextInt(4)) {
            case 0:
                condKind = MathGenerator.OpStatKind.LOGICAL;
                break;
            case 1:
                condKind = MathGenerator.OpStatKind.ARITHMETIC_LOGICAL;
                break;
            case 2:
                condKind = MathGenerator.OpStatKind.BITWISE_LOGICAL;
                break;
            case 3:
                condKind = MathGenerator.OpStatKind.ARITHMETIC_LOGICAL_BITWISE;
                break;
        }
        String src = mathGenerator.srcGenerateOperatorStatement(
                method, randomCodeGenerator.getController().getMaxOperatorsInOperatorStatement(), condKind);
        StringBuilder condition;
        if (src.contains("if")) {
            condition = new StringBuilder(
                    mathGenerator.srcGenerateOperatorStatement(
                            method, randomCodeGenerator.getController().
                                    getMaxOperatorsInOperatorStatement(), MathGenerator.OpStatKind.LOGICAL));
        } else {
            condition = new StringBuilder(src);
        }
        condition.deleteCharAt(condition.length() - 1);
        return condition.toString();
    }

    //=================================================DO WHILE=========================================================

    public void generateDoWhileStatement(MethodLogger contextMethod) {
        String condition = this.openDoWhileStatement();
        this.generateBody(contextMethod, ControlType.doWhileType, condition);
    }

    private String openDoWhileStatement() {
        String varName = this.getClazzContainer().getRandomSupplier().getVarName();
        deepness++;
        if (RANDOM.nextBoolean()) {
            controlSrc.append("int " + varName + " = 0; do { " + varName + "++;");
            return varName + " < " + RANDOM.nextInt(maxLoopIterations);
        } else {
            controlSrc.append("int " + varName + " = " + RANDOM.nextInt(maxLoopIterations) + "; do { " + varName + "--;");
            return varName + " > 0";
        }
    }

    private void closeDoWhileStatement(String condition) {
        controlSrc.append("} while(" + condition + ");");
        deepness--;
    }

    //==================================================FOR/WHILE=======================================================

    public void generateWhileStatement(MethodLogger contextMethod) {
        this.openWhileStatement();
        this.generateBody(contextMethod, ControlType.forWhileType);
    }

    private void openWhileStatement() {
        String varName = this.getClazzContainer().getRandomSupplier().getVarName();
        if (RANDOM.nextBoolean()) {
            controlSrc.append("int " + varName + " = 0; while(" +
                    varName + " < " + RANDOM.nextInt(maxLoopIterations) + ") { " + varName + "++; ");
        } else {
            controlSrc.append("int " + varName + " = " + RANDOM.nextInt(maxLoopIterations) + "; while(" +
                    varName + " > 0) { " + varName + "--; ");
        }
        ++deepness;
    }

    private void closeForWhileStatement() {
        controlSrc.append("}");
        deepness--;
    }

    public void generateForStatement(MethodLogger contextMethod) {
        this.openForStatement();
        this.generateBody(contextMethod, ControlType.forWhileType);
    }

    private void openForStatement() {
        RandomSupplier supplier = this.getClazzContainer().getRandomSupplier();
        String varName = supplier.getVarName();
        int it = RANDOM.nextInt(this.maxLoopIterations + 1);
        controlSrc.append("for(int " + varName + " = 0; " + varName + " < " + it + "; " + varName + "++) {");
        deepness++;
    }

    //==================================================COMMON==========================================================


    private void generateBody(MethodLogger contextMethod, ControlType controlType, String... condition) {
        RandomCodeGenerator.Context.CONTROL_CONTEXT.setContextMethod(contextMethod);
        randomCodeGenerator.generate(RandomCodeGenerator.Context.CONTROL_CONTEXT);
        if (controlType == ControlType.ifType) {
            this.closeIFStatement();
        } else if (controlType == ControlType.forWhileType) {
            this.closeForWhileStatement();
        } else if (controlType == ControlType.doWhileType) {
            this.closeDoWhileStatement(condition[0]);
        }
        if (this.getDeepness() == 0) {
            this.insertControlSrcIntoMethod(contextMethod);
        }
    }

    private void insertControlSrcIntoMethod(MethodLogger method) {
        CtMethod ctMethod = this.getCtMethod(method);
        try {
            ctMethod.insertAfter(controlSrc.toString());
            controlSrc.setLength(0);
        } catch (CannotCompileException e) {
            throw new AssertionError(e);
        }
    }

    public void addCodeToControlSrc(String code) {
        if (deepness > 0) {
            controlSrc.append(code);
        } else {
            System.err.println("Cannot insert code, no open control-flow-block");
        }
    }

    public int getDeepness() {
        return deepness;
    }
}



