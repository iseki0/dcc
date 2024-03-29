package space.iseki.dcc.gen;

import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import space.iseki.dcc.Codec;
import space.iseki.dcc.Decoder;
import space.iseki.dcc.Encoder;
import space.iseki.dcc.FieldMirrorRecord;

import java.io.PrintWriter;
import java.util.List;
import java.util.function.ObjIntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Gen {
    public static final int ENABLE_CHECK = 1;
    public static final int ENABLE_DEBUG_PRINT = 1 << 1;
    private static final String[] LISTOF_MDESC;
    private static final String[] EMPTY_STR_ARRAY = new String[0];
    private static final String MDESC_OBJECT_DECODER = Type.getMethodDescriptor(Types.OBJECT, Types.DECODER);
    private static final String MDESC_VOID_OBJECT_ENCODER = Type.getMethodDescriptor(Types.VOID, Types.OBJECT, Types.ENCODER);
    private static final String MDESC_BOOLEAN_STRING = Type.getMethodDescriptor(Types.BOOLEAN, Types.CODEC, Types.INT);
    private static final String MDESC_VOID_STRING_CLASS_BOOL = Type.getMethodDescriptor(Types.VOID, Types.STRING, Types.CLASS, Types.BOOLEAN);

    private static final String[] CODEC_INAME_ARRAY = new String[]{INames.CODEC};

    static {
        LISTOF_MDESC = new String[12];
        for (int i = 0; i < LISTOF_MDESC.length - 1; i++) {
            LISTOF_MDESC[i] = Type.getMethodDescriptor(Types.LIST, IntStream.range(0, i).mapToObj(a -> Types.OBJECT).toArray(Type[]::new));
        }
        LISTOF_MDESC[11] = Type.getMethodDescriptor(Types.LIST, Type.getType(Object[].class));
    }

    private final int flags;

    public Gen(int flags) {
        this.flags = flags;
    }

    private static <T> void visitBuildImmutableList(MethodVisitor mv, List<T> input, ObjIntConsumer<T> block) {
        var arrayMode = input.size() > 10;
        String mDescriptor = LISTOF_MDESC[Math.min(11, input.size())];
        if (arrayMode) {
            pushInt(mv, input.size());
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        }
        var p = 0;
        for (T it : input) {
            if (arrayMode) {
                mv.visitInsn(Opcodes.DUP);
                pushInt(mv, p);
            }
            block.accept(it, p++);
            if (arrayMode) {
                mv.visitInsn(Opcodes.AASTORE);
            }
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/List", "of", mDescriptor, true);
    }

    private static void pushInt(MethodVisitor mv, int i) {
        if (i >= -1 && i < 6) {
            mv.visitInsn(i + 3);
        } else {
            mv.visitIntInsn(Opcodes.BIPUSH, i);
        }
    }

    private static void pushZeroValue(MethodVisitor mv, Class<?> type) {
        int op;
        int op2 = 0;
        if (type == boolean.class) {
            op = Opcodes.ICONST_0;
        } else if (type == char.class) {
            op = Opcodes.ICONST_0;
            op2 = Opcodes.I2C;
        } else if (type == byte.class) {
            op = Opcodes.ICONST_0;
            op2 = Opcodes.I2B;
        } else if (type == short.class) {
            op = Opcodes.ICONST_0;
            op2 = Opcodes.I2S;
        } else if (type == int.class) {
            op = Opcodes.ICONST_0;
        } else if (type == long.class) {
            op = Opcodes.LCONST_0;
        } else if (type == float.class) {
            op = Opcodes.FCONST_0;
        } else if (type == double.class) {
            op = Opcodes.DCONST_0;
        } else {
            op = Opcodes.ACONST_NULL;
        }
        mv.visitInsn(op);
        if (op2 != 0) {
            mv.visitInsn(op2);
        }
    }

    private static String getInternalNameOfQName(String qname) {
        return qname.replace('.', '/');
    }

    private static void visitInit(ClassVisitor cv) {
        var mv = cv.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, EMPTY_STR_ARRAY);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, INames.OBJECT, "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void visitGetFieldsMirror(ClassVisitor cv, String codecInternalName, DType type) {
        var mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "getFieldsMirror", "()Ljava/util/List;", null, EMPTY_STR_ARRAY);
        mv.visitCode();
        mv.visitFieldInsn(Opcodes.GETSTATIC, codecInternalName, "FIELDS", "Ljava/util/List;");
        mv.visitInsn(Opcodes.DUP);
        var next = new Label();
        mv.visitJumpInsn(Opcodes.IFNULL, next);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(next);
        mv.visitInsn(Opcodes.POP);
        visitBuildImmutableList(mv, type.fields(), (field, i) -> {
            mv.visitTypeInsn(Opcodes.NEW, INames.FIELD_MIRROR_RECORD);
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(field.name());
            ldcClass(mv, field.descriptor());
            pushInt(mv, type.useDefault() && field.optional() ? 1 : 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, INames.FIELD_MIRROR_RECORD, "<init>", MDESC_VOID_STRING_CLASS_BOOL, false);
        });
        mv.visitInsn(Opcodes.DUP);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, codecInternalName, "FIELDS", "Ljava/util/List;");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static String getConstructorMethodDescriptor(DType type, int defaultBitmap, boolean useDefault) {
        var args = type.fields().stream().map(i -> Type.getType(i.descriptor()));
        var suffix = useDefault ? Stream.concat(IntStream.range(0, defaultBitmap).mapToObj((i) -> Types.INT), Stream.of(Types.DEFAULT_CONSTRUCTOR_MARKER)) : Stream.<Type>empty();
        var all = Stream.concat(args, suffix);
        return Type.getMethodDescriptor(Types.VOID, all.toArray(Type[]::new));
    }

    private static void visitEncode(ClassVisitor cv, DType type, String clzIName) {
        var mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "encodeTo", MDESC_VOID_OBJECT_ENCODER, null, EMPTY_STR_ARRAY);
        var labelS = new Label();
        var labelE = new Label();
        mv.visitCode();
        mv.visitLabel(labelS);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, clzIName);
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        List<Field> fields = type.fields();
        for (int i = 0, fieldsSize = fields.size(); i < fieldsSize; i++) {
            Field field = fields.get(i);
            var mc = EncoderMethodCall.of(field.descriptor());
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            pushInt(mv, i);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clzIName, field.getter(), Type.getMethodDescriptor(Type.getType(field.descriptor())), false);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, INames.ENCODER, mc.name, mc.mdesc, true);
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitLabel(labelE);
        mv.visitLocalVariable("t", "L" + clzIName + ";", null, labelS, labelE, 3);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void ldcClass(MethodVisitor mv, String descriptor) {
        if (descriptor.length() == 1) {
            var owner = switch (descriptor) {
                case "B" -> "java/lang/Byte";
                case "C" -> "java/lang/Character";
                case "S" -> "java/lang/Short";
                case "I" -> "java/lang/Integer";
                case "J" -> "java/lang/Long";
                case "F" -> "java/lang/Float";
                case "D" -> "java/lang/Double";
                case "Z" -> "java/lang/Boolean";
                case "V" -> "java/lang/Void";
                default -> throw new RuntimeException("unhandled single character descriptor: " + descriptor);
            };
            mv.visitFieldInsn(Opcodes.GETSTATIC, owner, "TYPE", "Ljava/lang/Class;");
            return;
        }
        mv.visitLdcInsn(Type.getType(descriptor));
    }

    private static void visitClinit(ClassVisitor cv, String thisInternalName) {
        cv.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "INSTANCE", Types.CODEC.getDescriptor(), null, null).visitEnd();
        cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE, "FIELDS", "Ljava/util/List;", null, null).visitEnd();
        var mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "<clinit>", "()V", null, EMPTY_STR_ARRAY);
        mv.visitCode();
        // do version check
        pushInt(mv, 1);
        pushInt(mv, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, INames.CODEC, "versionCheck", "(II)V", true);
        // INSTANCE
        mv.visitTypeInsn(Opcodes.NEW, thisInternalName);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, thisInternalName, "<init>", "()V", false);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, thisInternalName, "INSTANCE", Types.CODEC.getDescriptor());
        // return
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void visitDecode(ClassVisitor cv, DType type, String clzIName) {
        var useDefault = type.useDefault();
        var mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "decodeFrom", MDESC_OBJECT_DECODER, null, EMPTY_STR_ARRAY);
        var totalBitmapVar = useDefault ? (type.fields().size() - 1) / 32 + 1 : 0;
        var startLabel = new Label();
        var endLabel = new Label();
        mv.visitCode();
        mv.visitLabel(startLabel);
        // initialize bitmap local var
        for (int i = 0; i < totalBitmapVar; i++) {
            pushInt(mv, 0);
            mv.visitVarInsn(Opcodes.ISTORE, i + 2);
        }
        // new instance
        mv.visitTypeInsn(Opcodes.NEW, clzIName);
        mv.visitInsn(Opcodes.DUP);
        // fill arguments
        {
            var p = 0;
            for (Field field : type.fields()) {
                visitField(mv, field, p++, type.useDefault());
            }
        }
        // load argument for $default call
        if (useDefault) {
            for (int i = 0; i < totalBitmapVar; i++) {
                mv.visitVarInsn(Opcodes.ILOAD, i + 2);
            }
            mv.visitInsn(Opcodes.ACONST_NULL);
        }
        // call constructor
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, clzIName, "<init>", getConstructorMethodDescriptor(type, totalBitmapVar, useDefault), false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(endLabel);
        for (int i = 0; i < totalBitmapVar; i++) {
            mv.visitLocalVariable("m" + i, Type.getDescriptor(int.class), null, startLabel, endLabel, i + 2);
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void visitField(MethodVisitor mv, Field field, int pos, boolean useDefault) {
        var mc = DecoderMethodCall.of(field.descriptor());
        Label labelSkip = null;
        if (useDefault & field.optional()) {
            var orValue = 1 << (pos - 1) % 32;
            var bitmapIdx = (pos - 1) / 32 + 2;
            var labelGetValue = new Label();
            labelSkip = new Label();
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            pushInt(mv, pos);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, INames.DECODER, "isDefault", MDESC_BOOLEAN_STRING, true);
            mv.visitJumpInsn(Opcodes.IFEQ, labelGetValue);
            pushZeroValue(mv, mc.zero);
            mv.visitVarInsn(Opcodes.ILOAD, bitmapIdx);
            pushInt(mv, orValue);
            mv.visitInsn(Opcodes.IOR);
            mv.visitVarInsn(Opcodes.ISTORE, bitmapIdx);
            mv.visitJumpInsn(Opcodes.GOTO, labelSkip);
            mv.visitLabel(labelGetValue);
        }
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        pushInt(mv, pos);
        var targetType = Type.getType(field.descriptor());
        if (mc.complex) {
            mv.visitLdcInsn(targetType);
        }
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, INames.DECODER, mc.name, mc.mdesc, true);
        if (mc.complex) {
            mv.visitTypeInsn(Opcodes.CHECKCAST, targetType.getInternalName());
        }
        if (labelSkip != null) mv.visitLabel(labelSkip);
    }

    public byte[] generate(DType type) {
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = cw;
        if (isEnabled(ENABLE_CHECK)) {
            cv = new CheckClassAdapter(cv, true);
        }
        if (isEnabled(ENABLE_DEBUG_PRINT)) {
            cv = new TraceClassVisitor(cv, new PrintWriter(System.out));
        }
        var clzIName = type.qname().replace('.', '/');
        var codecInternalName = getInternalNameOfQName(type.qname() + "$DCodec");
        cv.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, codecInternalName, null, INames.OBJECT, CODEC_INAME_ARRAY);
        visitClinit(cv, codecInternalName);
        visitInit(cv);
        visitGetFieldsMirror(cv, codecInternalName, type);
        visitDecode(cv, type, clzIName);
        visitEncode(cv, type, clzIName);
        cv.visitEnd();

        return cw.toByteArray();
    }

    public boolean isEnabled(int flag) {
        return (flag & this.flags) != 0;
    }

    static class INames {
        public static final String CODEC = Types.CODEC.getInternalName();
        public static final String ENCODER = Types.ENCODER.getInternalName();
        public static final String DECODER = Types.DECODER.getInternalName();
        public static final String OBJECT = Types.OBJECT.getInternalName();
        public static final String FIELD_MIRROR_RECORD = Types.FIELD_MIRROR_RECORD.getInternalName();

    }

    static class Types {
        // primitive
        public static final Type BYTE = Type.BYTE_TYPE;
        public static final Type CHAR = Type.CHAR_TYPE;
        public static final Type SHORT = Type.SHORT_TYPE;
        public static final Type INT = Type.INT_TYPE;
        public static final Type LONG = Type.LONG_TYPE;
        public static final Type FLOAT = Type.FLOAT_TYPE;
        public static final Type DOUBLE = Type.DOUBLE_TYPE;
        public static final Type BOOLEAN = Type.BOOLEAN_TYPE;
        public static final Type VOID = Type.VOID_TYPE;

        // others
        public static final Type CLASS = Type.getType(Class.class);
        public static final Type CODEC = Type.getType(Codec.class);
        public static final Type ENCODER = Type.getType(Encoder.class);
        public static final Type DECODER = Type.getType(Decoder.class);
        public static final Type STRING = Type.getType(String.class);
        public static final Type OBJECT = Type.getType(Object.class);
        public static final Type LIST = Type.getType(List.class);
        public static final Type DEFAULT_CONSTRUCTOR_MARKER = Type.getType("Lkotlin/jvm/internal/DefaultConstructorMarker;");
        @SuppressWarnings({"deprecation"})
        public static final Type FIELD_MIRROR_RECORD = Type.getType(FieldMirrorRecord.class);
    }

    record EncoderMethodCall(String name, String mdesc) {

        public static final EncoderMethodCall BYTE = new EncoderMethodCall("setByte", Type.getMethodDescriptor(Types.VOID, Types.CODEC, Types.INT, Types.BYTE));
        public static final EncoderMethodCall CHAR = new EncoderMethodCall("setChar", Type.getMethodDescriptor(Types.VOID, Types.CODEC, Types.INT, Types.CHAR));
        public static final EncoderMethodCall SHORT = new EncoderMethodCall("setShort", Type.getMethodDescriptor(Types.VOID, Types.CODEC, Types.INT, Types.SHORT));
        public static final EncoderMethodCall INT = new EncoderMethodCall("setInt", Type.getMethodDescriptor(Types.VOID, Types.CODEC, Types.INT, Types.INT));
        public static final EncoderMethodCall LONG = new EncoderMethodCall("setLong", Type.getMethodDescriptor(Types.VOID, Types.CODEC, Types.INT, Types.LONG));
        public static final EncoderMethodCall FLOAT = new EncoderMethodCall("setFloat", Type.getMethodDescriptor(Types.VOID, Types.CODEC, Types.INT, Types.FLOAT));
        public static final EncoderMethodCall DOUBLE = new EncoderMethodCall("setDouble", Type.getMethodDescriptor(Types.VOID, Types.CODEC, Types.INT, Types.DOUBLE));
        public static final EncoderMethodCall BOOLEAN = new EncoderMethodCall("setBoolean", Type.getMethodDescriptor(Types.VOID, Types.CODEC, Types.INT, Types.BOOLEAN));
        public static final EncoderMethodCall OBJECT = new EncoderMethodCall("setObject", Type.getMethodDescriptor(Types.VOID, Types.CODEC, Types.INT, Types.OBJECT));

        public static EncoderMethodCall of(String descriptor) {
            //noinspection DuplicatedCode
            return switch (descriptor) {
                case "B" -> BYTE;
                case "C" -> CHAR;
                case "S" -> SHORT;
                case "I" -> INT;
                case "J" -> LONG;
                case "F" -> FLOAT;
                case "D" -> DOUBLE;
                case "Z" -> BOOLEAN;
                default -> OBJECT;
            };
        }
    }

    record DecoderMethodCall(String name, String mdesc, Class<?> zero, boolean complex) {
        public static final DecoderMethodCall BYTE = new DecoderMethodCall("getByte", Type.getMethodDescriptor(Types.BYTE, Types.CODEC, Types.INT), byte.class, false);
        public static final DecoderMethodCall CHAR = new DecoderMethodCall("getChar", Type.getMethodDescriptor(Types.CHAR, Types.CODEC, Types.INT), char.class, false);
        public static final DecoderMethodCall SHORT = new DecoderMethodCall("getShort", Type.getMethodDescriptor(Types.SHORT, Types.CODEC, Types.INT), short.class, false);
        public static final DecoderMethodCall INT = new DecoderMethodCall("getInt", Type.getMethodDescriptor(Types.INT, Types.CODEC, Types.INT), int.class, false);
        public static final DecoderMethodCall LONG = new DecoderMethodCall("getLong", Type.getMethodDescriptor(Types.LONG, Types.CODEC, Types.INT), long.class, false);
        public static final DecoderMethodCall FLOAT = new DecoderMethodCall("getFloat", Type.getMethodDescriptor(Types.FLOAT, Types.CODEC, Types.INT), float.class, false);
        public static final DecoderMethodCall DOUBLE = new DecoderMethodCall("getDouble", Type.getMethodDescriptor(Types.DOUBLE, Types.CODEC, Types.INT), double.class, false);
        public static final DecoderMethodCall BOOLEAN = new DecoderMethodCall("getBoolean", Type.getMethodDescriptor(Types.BOOLEAN, Types.CODEC, Types.INT), boolean.class, false);
        public static final DecoderMethodCall OBJECT = new DecoderMethodCall("getObject", Type.getMethodDescriptor(Types.OBJECT, Types.CODEC, Types.INT, Types.CLASS), Object.class, true);

        public static DecoderMethodCall of(String descriptor) {
            //noinspection DuplicatedCode
            return switch (descriptor) {
                case "B" -> BYTE;
                case "C" -> CHAR;
                case "S" -> SHORT;
                case "I" -> INT;
                case "J" -> LONG;
                case "F" -> FLOAT;
                case "D" -> DOUBLE;
                case "Z" -> BOOLEAN;
                default -> OBJECT;
            };
        }

    }

}
