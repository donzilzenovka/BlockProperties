package toast.blockProperties.core;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Adds metadata-aware lookups to block queries and entity contact with blocks. */
public final class MetadataLightTransformer implements IClassTransformer {
    private static final String BLOCK_CLASS = "net.minecraft.block.Block";
    private static final String ENTITY_CLASS = "net.minecraft.entity.Entity";
    private static final String HOOK_OWNER = "toast/blockProperties/MetadataLightRegistry";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        if (BLOCK_CLASS.equals(transformedName)) return transformBlock(basicClass);
        if (isEntityContactClass(transformedName)) return transformEntityContact(transformedName, basicClass);
        return basicClass;
    }

    private static byte[] transformBlock(byte[] basicClass) {
        final boolean[] patched = new boolean[4];
        ClassReader reader = new ClassReader(basicClass);
        final String blockName = reader.getClassName();
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
                if ("getLightValue".equals(name) && isWorldIntDescriptor(desc)) {
                    patched[0] = true;
                    return intOverride(parent, blockName, desc, "getLightValue");
                }
                if ("getLightOpacity".equals(name) && isWorldIntDescriptor(desc)) {
                    patched[1] = true;
                    return intOverride(parent, blockName, desc, "getLightOpacity");
                }
                if (("getBlockHardness".equals(name) || "func_149712_f".equals(name)) && isWorldFloatDescriptor(desc)) {
                    patched[2] = true;
                    return floatOverride(parent, blockName, desc, "getBlockHardness", 1, 2, 3, 4);
                }
                if ("getExplosionResistance".equals(name) && isResistanceDescriptor(desc)) {
                    patched[3] = true;
                    String worldDesc = desc.substring(desc.indexOf(';') + 1, desc.indexOf(";III") + 1);
                    String hookDesc = "(L" + blockName + ";" + worldDesc + "III)F";
                    return floatOverride(parent, hookDesc, "getExplosionResistance", 2, 3, 4, 5);
                }
                return parent;
            }
        };
        reader.accept(visitor, 0);
        for (int i = 0; i < patched.length; i++) {
            if (!patched[i]) throw new IllegalStateException("Block Properties could not install every metadata block-stat hook");
        }
        return writer.toByteArray();
    }

    private static MethodVisitor intOverride(MethodVisitor parent, String blockName, String desc, final String hook) {
        final String hookDesc = "(L" + blockName + ";" + desc.substring(1);
        return new MethodVisitor(Opcodes.ASM5, parent) {
            @Override
            public void visitCode() {
                super.visitCode();
                Label fallback = new Label();
                visitVarInsn(Opcodes.ALOAD, 0);
                visitVarInsn(Opcodes.ALOAD, 1);
                visitVarInsn(Opcodes.ILOAD, 2);
                visitVarInsn(Opcodes.ILOAD, 3);
                visitVarInsn(Opcodes.ILOAD, 4);
                visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, hook, hookDesc, false);
                visitInsn(Opcodes.DUP);
                visitInsn(Opcodes.ICONST_M1);
                visitJumpInsn(Opcodes.IF_ICMPEQ, fallback);
                visitInsn(Opcodes.IRETURN);
                visitLabel(fallback);
                visitInsn(Opcodes.POP);
            }
        };
    }

    private static MethodVisitor floatOverride(MethodVisitor parent, String blockName, String desc,
            String hook, int world, int x, int y, int z) {
        return floatOverride(parent, "(L" + blockName + ";" + desc.substring(1), hook, world, x, y, z);
    }

    private static MethodVisitor floatOverride(MethodVisitor parent, final String hookDesc, final String hook,
            final int world, final int x, final int y, final int z) {
        return new MethodVisitor(Opcodes.ASM5, parent) {
            @Override
            public void visitCode() {
                super.visitCode();
                Label fallback = new Label();
                visitVarInsn(Opcodes.ALOAD, 0);
                visitVarInsn(Opcodes.ALOAD, world);
                visitVarInsn(Opcodes.ILOAD, x);
                visitVarInsn(Opcodes.ILOAD, y);
                visitVarInsn(Opcodes.ILOAD, z);
                visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, hook, hookDesc, false);
                visitInsn(Opcodes.DUP);
                visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "isNaN", "(F)Z", false);
                visitJumpInsn(Opcodes.IFNE, fallback);
                visitInsn(Opcodes.FRETURN);
                visitLabel(fallback);
                visitInsn(Opcodes.POP);
            }
        };
    }

    private static byte[] transformEntityContact(final String transformedName, byte[] basicClass) {
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, final String methodName, final String desc,
                    String signature, String[] exceptions) {
                MethodVisitor parent = super.visitMethod(access, methodName, desc, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM5, parent) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String fieldDesc) {
                        if (opcode == Opcodes.GETFIELD && isBlockOwner(owner) && isSlipperiness(name, fieldDesc)) {
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, "getSlipperinessAtFeet",
                                    "(L" + owner + ";Lnet/minecraft/entity/Entity;)F", false);
                            return;
                        }
                        if (opcode == Opcodes.GETFIELD && isBlockOwner(owner) && isStepSound(name, fieldDesc)) {
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            if (ENTITY_CLASS.equals(transformedName) && isStepMethod(methodName, desc)) {
                                super.visitVarInsn(Opcodes.ILOAD, 1);
                                super.visitVarInsn(Opcodes.ILOAD, 2);
                                super.visitVarInsn(Opcodes.ILOAD, 3);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, "getSoundType",
                                        "(L" + owner + ";Lnet/minecraft/entity/Entity;III)" + fieldDesc, false);
                            }
                            else {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, "getSoundTypeAtFeet",
                                        "(L" + owner + ";Lnet/minecraft/entity/Entity;)" + fieldDesc, false);
                            }
                            return;
                        }
                        super.visitFieldInsn(opcode, owner, name, fieldDesc);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static boolean isEntityContactClass(String name) {
        return ENTITY_CLASS.equals(name) || "net.minecraft.entity.EntityLivingBase".equals(name) ||
                "net.minecraft.entity.EntityFlying".equals(name) || "net.minecraft.entity.item.EntityItem".equals(name) ||
                "net.minecraft.entity.item.EntityXPOrb".equals(name) || "net.minecraft.entity.passive.EntityHorse".equals(name);
    }

    private static boolean isBlockOwner(String owner) {
        return "net/minecraft/block/Block".equals(owner);
    }

    private static boolean isSlipperiness(String name, String desc) {
        return "F".equals(desc) && ("slipperiness".equals(name) || "field_149765_K".equals(name));
    }

    private static boolean isStepSound(String name, String desc) {
        return desc.endsWith("Block$SoundType;") && ("stepSound".equals(name) || "field_149762_H".equals(name));
    }

    private static boolean isStepMethod(String name, String desc) {
        return ("func_145780_a".equals(name) || "playStepSound".equals(name)) && desc.startsWith("(III");
    }

    private static boolean isWorldIntDescriptor(String desc) {
        return desc != null && desc.startsWith("(L") && desc.endsWith(";III)I");
    }

    private static boolean isWorldFloatDescriptor(String desc) {
        return desc != null && desc.startsWith("(L") && desc.endsWith(";III)F");
    }

    private static boolean isResistanceDescriptor(String desc) {
        return desc != null && desc.startsWith("(L") && desc.endsWith(";IIIDDD)F");
    }
}
