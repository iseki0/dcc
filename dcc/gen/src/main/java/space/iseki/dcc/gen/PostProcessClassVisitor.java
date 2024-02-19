package space.iseki.dcc.gen;

import kotlin.Metadata;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class PostProcessClassVisitor extends ClassVisitor {
    static final int API = Opcodes.ASM9;

    private boolean modified;

    private final Map<String, String> getterDescriptor = new HashMap<>();

    /**
     * fields only be recorded in <em>non-kotlin mode</em>
     */
    private final List<String> fields = new ArrayList<>();
    private Metadata kmRaw;
    private boolean isRecord;
    private boolean enableAnalyze;

    public PostProcessClassVisitor(ClassVisitor classVisitor) {
        super(API, classVisitor);
    }
    private DType dType;
    private String clazzName;

    public DType getDType() {
        return dType;
    }

    public boolean isModified() {
        return modified;
    }

    private void analyzeMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            return;
        }
        if (!isRecord && (name.length() < 4 || !name.startsWith("get"))) {
            return;
        }
        getterDescriptor.put(name, Type.getMethodType(descriptor).getReturnType().getDescriptor());
    }

    private void analyzeRecordComponent(String name, String descriptor, String signature) {
        getterDescriptor.put(name, descriptor);
        fields.add(name);
    }

    private void throwGetterNotFound(String field) {
        throw new IllegalStateException("getter of field \"" + field + "\" not found");
    }

    private void analyzeEnd() {
        var kmData = kmRaw != null ? Utils.readDataKM(kmRaw) : null;
        if (kmData == null) {
            if (!isRecord) throw new IllegalStateException("require kotlin data class or record");
        }
        var r = new ArrayList<Field>();
        var useOptional = false;
        if (kmData != null) {
            for (KmData.F f : kmData.fields()) {
                var getter = f.name();
                if (!isRecord) {
                    // replace to getXxx
                    getter = "get" + Character.toUpperCase(getter.charAt(0)) + getter.substring(1);
                }
                var desc = getterDescriptor.get(getter);
                if (desc == null) throwGetterNotFound(f.name());
                var field = new Field(f.name(), desc, f.optional(), getter);
                r.add(field);
                useOptional = useOptional || field.optional();
            }
        } else {
            for (String name : fields) {
                var desc = getterDescriptor.get(name);
                if (desc == null) throwGetterNotFound(name);
                r.add(new Field(name, desc, false, name));
            }
        }
        dType = new DType(clazzName, r, useOptional);
    }


    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        if (enableAnalyze) analyzeRecordComponent(name, descriptor, signature);
        return super.visitRecordComponent(name, descriptor, signature);
    }

    @Override
    public void visitEnd() {
        if (enableAnalyze) analyzeEnd();
        super.visitEnd();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        clazzName = name;
        isRecord = Objects.equals(superName, "java/lang/Record");
        super.visit(version, access, name, signature, superName, interfaces);
    }


    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        var s = super.visitAnnotation(descriptor, visible);
        switch (descriptor) {
            case "Lkotlin/Metadata;" -> {
                return new KotlinMetadataVisitor(s, meta -> kmRaw = meta);
            }
            case "Lspace/iseki/dcc/Dcc;" -> enableAnalyze = true;
        }
        return s;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (enableAnalyze) analyzeMethod(access, name, descriptor, signature, exceptions);
        return new LdcReplacerMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions), mo -> modified = modified || mo);
    }


    static class ArrayCollectorAnnotationVisitor extends AnnotationVisitor {
        private final Consumer<List<Object>> end;
        private final ArrayList<Object> arrayList = new ArrayList<>();

        protected ArrayCollectorAnnotationVisitor(int api, AnnotationVisitor annotationVisitor, Consumer<List<Object>> end) {
            super(api, annotationVisitor);
            this.end = end;
        }

        @Override
        public void visit(String name, Object value) {
            arrayList.add(value);
            super.visit(name, value);
        }

        @Override
        public void visitEnd() {
            end.accept(arrayList);
            super.visitEnd();
        }
    }

    static class KotlinMetadataVisitor extends AnnotationVisitor {
        private final Consumer<Metadata> end;
        private int k;
        private int[] mv;
        private int[] bv;
        private String[] d1;
        private String[] d2;
        private String xs;
        private String pn;
        private int xi;

        protected KotlinMetadataVisitor(AnnotationVisitor annotationVisitor, Consumer<Metadata> end) {
            super(API, annotationVisitor);
            this.end = end;
        }

        @Override
        public void visitEnd() {
            end.accept(Utils.genMetadata(k, mv, bv, d1, d2, xs, pn, xi));
            super.visitEnd();
        }

        @Override
        public void visit(String name, Object value) {
            switch (name) {
                case "k":
                    k = (Integer) value;
                    break;
                case "mv":
                    mv = (int[]) value;
                    break;
                case "bv":
                    bv = (int[]) value;
                    break;
                case "xs":
                    xs = (String) value;
                    break;
                case "pn":
                    pn = (String) value;
                    break;
                case "xi":
                    xi = (int) value;
                    break;
            }
            super.visit(name, value);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return new ArrayCollectorAnnotationVisitor(api, super.visitArray(name), list -> {
                switch (name) {
                    case "d1":
                        //noinspection SuspiciousToArrayCall
                        d1 = list.toArray(String[]::new);
                        break;
                    case "d2":
                        //noinspection SuspiciousToArrayCall
                        d2 = list.toArray(String[]::new);
                        break;
                }
            });
        }
    }

    static class LdcReplacerMethodVisitor extends NMethodVisitor {
        private Type lastLdc = null;
        private final Consumer<Boolean> consumer;
        private boolean modified;

        protected LdcReplacerMethodVisitor(MethodVisitor methodVisitor, Consumer<Boolean> consumer) {
            super(API, methodVisitor);
            this.consumer = consumer;
        }

        @Override
        protected void doVisit() {
            clearLastLdc();
        }

        private void clearLastLdc() {
            if (lastLdc != null) {
                if (mv != null) mv.visitLdcInsn(lastLdc);
                lastLdc = null;
            }
        }

        private void doGetField() {
            modified = true;
            var codecName = lastLdc.getInternalName() + "$DCodec";
            lastLdc = null;
            visitFieldInsn(Opcodes.GETSTATIC, codecName, "INSTANCE", Gen.Types.CODEC.getDescriptor());
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (lastLdc != null) {
                clearLastLdc();
            }
            if (value instanceof Type) {
                lastLdc = (Type) value;
            } else {
                super.visitLdcInsn(value);
            }
        }

        @Override
        public void visitLabel(Label label) {
            if (mv != null) mv.visitLabel(label);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (lastLdc == null || opcode != Opcodes.INVOKESTATIC || !Objects.equals(name, "getCodec") || !Objects.equals(owner, "space/iseki/dcc/Data")) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                return;
            }
            doGetField();
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (mv != null) mv.visitLineNumber(line, start);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
            if (lastLdc != null && opcode == Opcodes.INVOKESTATIC && Objects.equals(name, "getCodec") && Objects.equals(owner, "space/iseki/dcc/Data")) {
                doGetField();
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor);
            }
        }

        @Override
        public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        }

        @Override
        public void visitEnd() {
            if (consumer != null) consumer.accept(modified);
            super.visitEnd();
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(0, 0);
        }
    }

}

