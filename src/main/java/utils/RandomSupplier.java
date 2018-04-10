package utils;

import java.lang.reflect.Modifier;
import java.util.Random;

public class RandomSupplier {
    static private int methodCharNum = 97;
    static private int varCharNum = 97;
    static private int varRepeat = 0;
    static private int methodRepeat = 0;
    private final static Random random = new Random();
    static private final String stringCandidates = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * @return a new unique Variable-name, if only names generated by RandomSupplier are used
     */
    public static String getVarName() {
        if (varCharNum == 123) {
            varRepeat++;
            varCharNum = 97;
        }
        char c = (char) varCharNum;
        varCharNum++;
        String character = String.valueOf(c);
        String name = character;
        for (int i = 0; i < varRepeat; i++) {
            name = name + character;
        }
        return name;
    }

    /**
     * @return @return a new unique Method-name, if only names generated by RandomSupplier are used
     */
    public static String getMethodName() {
        if (methodCharNum == 123) {
            methodRepeat++;
            methodCharNum = 97;
        }
        char c = (char) methodCharNum;
        methodCharNum++;
        String character = String.valueOf(c);
        String name = "method" + character.toUpperCase();
        for (int i = 0; i < methodRepeat; i++) {
            name = name + character;
        }
        return name;
    }

    /**
     * @return a random FieldVarType
     */
    public static FieldVarType getFieldVarType() {
        int r = random.nextInt(FieldVarType.values().length - 1); //exclude voidType
        return FieldVarType.values()[r];
    }

    /**
     * @return an random array of FieldVarTypes
     */
    public static FieldVarType[] getFieldVarTypes() {
        int number = 1 + random.nextInt(FieldVarType.values().length - 2);
        FieldVarType[] types = new FieldVarType[number];
        for (int i = 0; i < number; i++) {
            types[i] = getFieldVarType();
        }
        return types;
    }

    /**
     * @return a random FieldType including type Void for ReturnType of a Method
     */
    public static FieldVarType getReturnType() {
        int r = random.nextInt(FieldVarType.values().length);
        return FieldVarType.values()[r];
    }

    /**
     * @param ft the type of the value
     * @return a random value of given FieldVarType
     */
    public static Object getValue(FieldVarType ft) {
        if (ft.getClazzType().getName().startsWith("java.lang")) {
            //for Objects 25% chance to be initialized with null
            if (random.nextInt(4) == 0) return null;
        }
        switch (ft) {
            case Byte:
                return (byte) random.nextInt();
            case Short:
                return (short) random.nextInt();
            case Int:
                return random.nextInt();
            case Long:
                return random.nextLong();
            case Float:
                return random.nextFloat();
            case Double:
                return random.nextDouble();
            case Boolean:
                return random.nextBoolean();
            case Char:
                return stringCandidates.charAt(random.nextInt(stringCandidates.length()));
            case String:
                return getString();
            default:
                return null;
        }
    }

    /**
     * @return a randomly generated String
     */
    public static String getString() {
        int length = random.nextInt(20);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(stringCandidates.charAt(random.nextInt(stringCandidates.length())));
        }
        return sb.toString();
    }


    /**
     * @return returns random modifiers with modifier static always included
     */
    public static int getModifiers() {
        int numberOfModifiers = 1 + random.nextInt(3);
        int[] modifiers = new int[numberOfModifiers];
        modifiers[0] = Modifier.STATIC;
        if (numberOfModifiers == 1) return mergeModifiers(modifiers);
        boolean hasAccessModifier = false;
        boolean isFinal = false;
        for (int i = 1; i < numberOfModifiers; i++) {
            boolean final_ = random.nextBoolean();
            if (final_) {
                if (!isFinal) {
                    modifiers[i] = Modifier.FINAL;
                    continue;
                }
            }
            int r = random.nextInt(3);
            switch (r) {
                case 0:
                    if (hasAccessModifier) {
                        modifiers[i] = Modifier.FINAL;
                        break;
                    }
                    modifiers[i] = Modifier.PUBLIC;
                    hasAccessModifier = true;
                    break;
                case 1:
                    if (hasAccessModifier) {
                        modifiers[i] = Modifier.FINAL;
                        break;
                    }
                    modifiers[i] = Modifier.PRIVATE;
                    hasAccessModifier = true;
                    break;
                case 2:
                    if (hasAccessModifier) {
                        modifiers[i] = Modifier.FINAL;
                        break;
                    }
                    modifiers[i] = Modifier.PROTECTED;
                    hasAccessModifier = true;
                    break;
            }

        }
        return mergeModifiers(modifiers);
    }

    /**
     * megeres the given array of modifiers into one Integer-variable
     *
     * @param modifiers the modifiers to merge
     * @return the merged modifiers
     */
    static int mergeModifiers(int[] modifiers) {
        int merged_modifiers = modifiers[0];
        for (int i = 1; i < modifiers.length; i++) {
            merged_modifiers |= modifiers[i];
        }
        return merged_modifiers;
    }
}
