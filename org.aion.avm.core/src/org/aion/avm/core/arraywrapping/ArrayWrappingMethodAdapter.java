package org.aion.avm.core.arraywrapping;

import org.aion.avm.arraywrapper.*;
import org.aion.avm.core.util.Assert;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ArrayWrappingMethodAdapter extends AdviceAdapter implements Opcodes {

    private Type typeInt = Type.getType(int.class);

    private Type typeA = Type.getType(org.aion.avm.arraywrapper.Array.class);
    private Type typeBA = Type.getType(ByteArray.class);
    private Type typeCA = Type.getType(CharArray.class);
    private Type typeDA = Type.getType(DoubleArray.class);
    private Type typeFA = Type.getType(FloatArray.class);
    private Type typeIA = Type.getType(IntArray.class);
    private Type typeLA = Type.getType(LongArray.class);
    private Type typeSA = Type.getType(ShortArray.class);


    public ArrayWrappingMethodAdapter(final MethodVisitor mv, final int access, final String name, final String desc)
    {
        super(Opcodes.ASM6, mv, access, name, desc);
    }

    @Override
    public void visitInsn(final int opcode) {

        Method m;

        switch (opcode) {
            // Static type
            case Opcodes.BALOAD:
                m = Method.getMethod("byte get(int)");
                invokeVirtual(typeBA, m);
                break;
            case Opcodes.CALOAD:
                m = Method.getMethod("char get(int)");
                invokeVirtual(typeCA, m);
                break;
            case Opcodes.DALOAD:
                m = Method.getMethod("double get(int)");
                invokeVirtual(typeDA, m);
                break;
            case Opcodes.FALOAD:
                m = Method.getMethod("float get(int)");
                invokeVirtual(typeFA, m);
                break;
            case Opcodes.IALOAD:
                m = Method.getMethod("int get(int)");
                invokeVirtual(typeIA, m);
                break;
            case Opcodes.LALOAD:
                m = Method.getMethod("long get(int)");
                invokeVirtual(typeLA, m);
                break;
            case Opcodes.SALOAD:
                m = Method.getMethod("short get(int)");
                invokeVirtual(typeSA, m);
                break;
            case Opcodes.BASTORE:
                m = Method.getMethod("void set(int, byte)");
                invokeVirtual(typeBA, m);
                break;
            case Opcodes.CASTORE:
                m = Method.getMethod("void set(int, char)");
                invokeVirtual(typeCA, m);
                break;
            case Opcodes.DASTORE:
                m = Method.getMethod("void set(int, double)");
                invokeVirtual(typeDA, m);
                break;
            case Opcodes.FASTORE:
                m = Method.getMethod("void set(int, float)");
                invokeVirtual(typeFA, m);
                break;
            case Opcodes.IASTORE:
                m = Method.getMethod("void set(int, int)");
                invokeVirtual(typeIA, m);
                break;
            case Opcodes.LASTORE:
                m = Method.getMethod("void set(int, long)");
                invokeVirtual(typeLA, m);
                break;
            case Opcodes.SASTORE:
                m = Method.getMethod("void set(int, short)");
                invokeVirtual(typeSA, m);
                break;

            case Opcodes.AALOAD:
                break;

            case Opcodes.AASTORE:
                break;

            case Opcodes.ARRAYLENGTH:
                m = Method.getMethod("int length()");
                invokeVirtual(typeA, m);
                break;

            default:
                this.mv.visitInsn(opcode);
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        Method m;

        if (opcode == Opcodes.NEWARRAY) {
            switch (operand) {
                case Opcodes.T_BOOLEAN:
                case Opcodes.T_BYTE:
                    m = Method.getMethod("org.aion.avm.arraywrapper.ByteArray initArray(int)");
                    invokeStatic(typeBA, m);
                    break;
                case Opcodes.T_SHORT:
                    m = Method.getMethod("org.aion.avm.arraywrapper.ShortArray initArray(int)");
                    invokeStatic(typeSA, m);
                    break;
                case Opcodes.T_INT:
                    m = Method.getMethod("org.aion.avm.arraywrapper.IntArray initArray(int)");
                    invokeStatic(typeIA, m);
                    break;
                case Opcodes.T_LONG:
                    m = Method.getMethod("org.aion.avm.arraywrapper.LongArray initArray(int)");
                    invokeStatic(typeLA, m);
                    break;
                case Opcodes.T_CHAR:
                    m = Method.getMethod("org.aion.avm.arraywrapper.CharArray initArray(int)");
                    invokeStatic(typeCA, m);
                    break;
                case Opcodes.T_FLOAT:
                    m = Method.getMethod("org.aion.avm.arraywrapper.FloatArray initArray(int)");
                    invokeStatic(typeFA, m);
                    break;
                case Opcodes.T_DOUBLE:
                    m = Method.getMethod("org.aion.avm.arraywrapper.DoubleArray initArray(int)");
                    invokeStatic(typeDA, m);
                    break;
                default:
                    this.mv.visitIntInsn(opcode, operand);
            }
        }else{
            this.mv.visitIntInsn(opcode, operand);
        }
    }
}