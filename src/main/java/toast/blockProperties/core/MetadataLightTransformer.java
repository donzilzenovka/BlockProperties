package toast.blockProperties.core;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Adds a metadata-aware override ahead of Forge's world-aware Block light lookup. */
public final class MetadataLightTransformer implements IClassTransformer {
    private static final String TARGET_CLASS = "net.minecraft.block.Block";
    private static final String HOOK_OWNER = "toast/blockProperties/MetadataLightRegistry";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }

        final boolean[] patched = new boolean[1];
        ClassReader reader = new ClassReader(basicClass);
        final String blockName = reader.getClassName();
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
                MethodVisitor parent = super.visitMethod(access, methodName, desc, signature, exceptions);
                if (!"getLightValue".equals(methodName) || !isWorldLightDescriptor(desc)) {
                    return parent;
                }
                patched[0] = true;
                final String hookDesc = "(L" + blockName + ";" + desc.substring(1);
                return new MethodVisitor(Opcodes.ASM5, parent) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label useVanilla = new Label();
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitVarInsn(Opcodes.ALOAD, 1);
                        super.visitVarInsn(Opcodes.ILOAD, 2);
                        super.visitVarInsn(Opcodes.ILOAD, 3);
                        super.visitVarInsn(Opcodes.ILOAD, 4);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, "getLightValue", hookDesc, false);
                        super.visitInsn(Opcodes.DUP);
                        super.visitInsn(Opcodes.ICONST_M1);
                        super.visitJumpInsn(Opcodes.IF_ICMPEQ, useVanilla);
                        super.visitInsn(Opcodes.IRETURN);
                        super.visitLabel(useVanilla);
                        super.visitInsn(Opcodes.POP);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        if (!patched[0]) {
            throw new IllegalStateException("Block Properties could not find Block.getLightValue(IBlockAccess,int,int,int)");
        }
        return writer.toByteArray();
    }

    private static boolean isWorldLightDescriptor(String desc) {
        return desc != null && desc.startsWith("(L") && desc.endsWith(";III)I");
    }
}
