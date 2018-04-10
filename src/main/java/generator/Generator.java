package generator;

import javassist.*;
import utils.ClazzFileContainer;

import java.io.IOException;

import utils.ClazzLogger;
import utils.FieldVarType;

/**
 * capable of generating the smallest executable class-file
 */
class Generator {

    private ClazzFileContainer clazzContainer;

    /**
     * Takes an existing utils.ClazzFileContainer to extend
     *
     * @param cf the container for the class-file with additional information
     */
    public Generator(ClazzFileContainer cf) {
        this.clazzContainer = cf;
    }

    /**
     * creates a generator.Generator with a new utils.ClazzFileContainer
     *
     * @param filename name of the class-file to be generated
     */
    public Generator(String filename) {
        this.clazzContainer = new ClazzFileContainer(filename);
    }

    /**
     * creates a generator.Generator with a new utils.ClazzFileContainer with a default filename
     */
    public Generator() {
        this.clazzContainer = new ClazzFileContainer("GenClazz");
    }

    public ClazzFileContainer getClazzContainer() {
        return clazzContainer;
    }

    /**
     * @return the class-file of this generator
     */
    public CtClass getClazzFile() {
        return clazzContainer.getClazzFile();
    }


    /**
     * write the CtClass-Object as a .class file
     */
    public void writeFile() {
        try {
            this.getClazzFile().writeFile();
        } catch (NotFoundException | IOException | CannotCompileException e) {
            System.err.println("Cannot write class-file");
            e.printStackTrace();
        }
    }

    /**
     * @return the ClazzLogger-Object of the class, generated by this Generator
     */
    public ClazzLogger getClazzLogger() {
        return this.clazzContainer.getClazzLogger();
    }

    /**
     * @param methodName the name of the method to return
     * @return CtMethod-Object of the method with given name, null if this method does not exist
     */
    public CtMethod getMethod(String methodName) {
        if (this.getClazzLogger().hasMethod(methodName)) {
            try {
                return this.getClazzFile().getDeclaredMethod(methodName);
            } catch (NotFoundException e) {
                System.err.println("Method " + methodName + " not found");
                e.printStackTrace();
                return null;
            }
        } else {
            System.err.println("Method " + methodName + " does not exist in the generated class");
            return null;
        }
    }
}

