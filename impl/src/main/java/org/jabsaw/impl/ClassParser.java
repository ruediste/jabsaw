package org.jabsaw.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import org.jabsaw.impl.model.ClassModel;
import org.jabsaw.impl.model.ModuleModel;
import org.jabsaw.impl.model.ProjectModel;
import org.jabsaw.impl.pattern.ClassPattern;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

public class ClassParser {

	private final ProjectModel project = new ProjectModel();

	public ProjectModel getProject() {
		return project;
	}

	public interface DirectoryParsingCallback {
		void parsingFile(Path file);

		void error(String error);
	}

	public void parseDirectory(final ArrayList<String> errors, Path directory,
			final DirectoryParsingCallback callback) {
		try {
			Files.walkFileTree(directory, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile()
							&& file.getFileName().toString().endsWith(".class")) {
						callback.parsingFile(file);

						try {
							parse(file);
						} catch (Throwable t) {
							StringWriter writer = new StringWriter();
							t.printStackTrace(new PrintWriter(writer));
							callback.error("Error while parsing " + file + ": "
									+ t.getMessage() + "\n" + writer.toString());
						}
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file,
						IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir,
						IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Error while reading input files", e);
		}
	}

	public void parse(Path file) throws FileNotFoundException, IOException {
		try (FileInputStream fis = new FileInputStream(file.toFile())) {
			ClassReader reader = new ClassReader(fis);
			parse(reader);
		}
	}

	public void parse(ClassReader reader) {
		reader.accept(new ParsingClassVisitor(), ClassReader.SKIP_FRAMES);
	}

	class ParsingClassVisitor extends ClassVisitor {

		ClassModel classModel;

		public ParsingClassVisitor() {
			super(Opcodes.ASM5);
		}

		@Override
		public void visit(int version, int access, String name,
				String signature, String superName, String[] interfaces) {
			classModel = new ClassModel(project, Type.getObjectType(name)
					.getClassName());
			handleClassOrMethodSignature(classModel, signature);
			handleType(classModel, superName);
			for (String s : interfaces) {
				handleType(classModel, s);
			}
		}

		@Override
		public void visitOuterClass(String owner, String name, String desc) {
			super.visitOuterClass(owner, name, desc);
			classModel.outerClassName = Type.getObjectType(owner)
					.getClassName();
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			if ("org.jabsaw.Module".equals(Type.getType(desc).getClassName())) {
				return new ModuleAnnotationVisitor(
						classModel.getQualifiedName());
			} else {
				handleTypeDescriptor(classModel, desc);
				return new ParsingAnnotationVisitor(classModel);
			}
		}

		@Override
		public void visitInnerClass(String name, String outerName,
				String innerName, int access) {
			classModel.innerClassNames.add(Type.getObjectType(name)
					.getClassName());
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {
			handleClassOrMethodSignature(classModel, signature);
			handleMethodDescriptor(classModel, desc);
			if (exceptions != null) {
				for (String s : exceptions) {
					handleType(classModel, s);
				}
			}
			return new ParsingMethodVisitor(classModel);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String desc,
				String signature, Object value) {
			handleFieldSignature(classModel, signature);
			handleTypeDescriptor(classModel, desc);
			return new ParsingFieldVisitor(classModel);
		}

	}

	public class ParsingFieldVisitor extends FieldVisitor {

		private ClassModel classModel;

		public ParsingFieldVisitor(ClassModel classModel) {
			super(Opcodes.ASM5);
			this.classModel = classModel;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			handleTypeDescriptor(classModel, desc);
			return new ParsingAnnotationVisitor(classModel);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef,
				TypePath typePath, String desc, boolean visible) {
			handleTypeDescriptor(classModel, desc);
			return new ParsingAnnotationVisitor(classModel);
		}

	}

	public class ModuleAnnotationVisitor extends AnnotationVisitor {

		ModuleModel module;

		public ModuleAnnotationVisitor(String enclosingClass) {
			super(Opcodes.ASM5);
			module = new ModuleModel(project, enclosingClass);
			module.addInclusionPattern(new ClassPattern(module.getPackage(),
					".*"));
		}

		@Override
		public void visit(String name, Object value) {
			if ("imported".equals(name)) {
				for (Type t : (Type[]) value) {
					module.addImportedModuleName(t.getClassName());
				}
			}

			if ("exported".equals(name)) {
				for (Type t : (Type[]) value) {
					module.addExportedModuleName(t.getClassName());
				}
			}

			if ("include".equals(name)) {
				for (Type t : (Type[]) value) {
					module.addInclusionPattern(new ClassPattern(module
							.getPackage(), t.getClassName()));
				}
			}

			if ("includePattern".equals(name)) {
				for (String s : (String[]) value) {

					module.addInclusionPattern(new ClassPattern(module
							.getPackage(), s));
				}
			}

			if ("exclude".equals(name)) {
				for (Type t : (Type[]) value) {
					module.addExclusionPattern(new ClassPattern(module
							.getPackage(), t.getClassName()));
				}
			}

			if ("excludePattern".equals(name)) {
				String[] imported = (String[]) value;
				for (String s : imported) {
					module.addExclusionPattern(new ClassPattern(module
							.getPackage(), s));
				}
			}
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			return new ModuleAnnotationArrayVisitor(module, name);
		}

	}

	public class ModuleAnnotationArrayVisitor extends AnnotationVisitor {

		ModuleModel module;
		private String arrayName;

		public ModuleAnnotationArrayVisitor(ModuleModel module, String arrayName) {
			super(Opcodes.ASM5);
			this.module = module;
			this.arrayName = arrayName;
		}

		@Override
		public void visit(String name, Object value) {
			if ("imported".equals(arrayName)) {
				module.addImportedModuleName(((Type) value).getClassName());
			}

			if ("exported".equals(arrayName)) {
				module.addExportedModuleName(((Type) value).getClassName());
			}

			if ("include".equals(arrayName)) {
				module.addInclusionPattern(new ClassPattern(
						module.getPackage(), ((Type) value).getClassName()));
			}

			if ("includePattern".equals(arrayName)) {

				module.addInclusionPattern(new ClassPattern(
						module.getPackage(), (String) value));
			}

			if ("exclude".equals(arrayName)) {
				module.addExclusionPattern(new ClassPattern(
						module.getPackage(), ((Type) value).getClassName()));
			}

			if ("excludePattern".equals(arrayName)) {
				module.addExclusionPattern(new ClassPattern(
						module.getPackage(), (String) value));
			}
		}
	}

	public class ParsingAnnotationVisitor extends AnnotationVisitor {

		private ClassModel classModel;

		public ParsingAnnotationVisitor(ClassModel classModel) {
			super(Opcodes.ASM5);
			this.classModel = classModel;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String desc) {
			handleTypeDescriptor(classModel, desc);
			return this;
		}

		@Override
		public void visit(String name, Object value) {
			if (value instanceof Type) {
				classModel.addUsesClassName(((Type) value).getClassName());
			}
			if (value instanceof Type[]) {
				for (Type t : (Type[]) value) {
					classModel.addUsesClassName(t.getClassName());
				}
			}
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			return this;
		}

		@Override
		public void visitEnum(String name, String desc, String value) {
			classModel
					.addUsesClassName(Type.getObjectType(desc).getClassName());
		}
	}

	public class ParsingMethodVisitor extends MethodVisitor {

		private ClassModel classModel;

		public ParsingMethodVisitor(ClassModel classModel) {
			super(Opcodes.ASM5);
			this.classModel = classModel;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			handleTypeDescriptor(classModel, desc);
			return new ParsingAnnotationVisitor(classModel);
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			return new ParsingAnnotationVisitor(classModel);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef,
				TypePath typePath, String desc, boolean visible) {
			handleTypeDescriptor(classModel, desc);
			return new ParsingAnnotationVisitor(classModel);
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter,
				String desc, boolean visible) {
			handleTypeDescriptor(classModel, desc);
			return new ParsingAnnotationVisitor(classModel);
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			handleType(classModel, type);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name,
				String desc) {
			handleTypeDescriptor(classModel, desc);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name,
				String desc) {
			handleMethodDescriptor(classModel, desc);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name,
				String desc, boolean itf) {
			handleMethodDescriptor(classModel, desc);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String desc,
				Handle bsm, Object... bsmArgs) {
			handleMethodDescriptor(classModel, desc);
		}

		@Override
		public void visitMultiANewArrayInsn(String desc, int dims) {
			handleTypeDescriptor(classModel, desc);
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef,
				TypePath typePath, String desc, boolean visible) {
			handleTypeDescriptor(classModel, desc);
			return new ParsingAnnotationVisitor(classModel);
		}

		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler,
				String type) {
			handleType(classModel, type);
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef,
				TypePath typePath, String desc, boolean visible) {
			handleTypeDescriptor(classModel, desc);
			return new ParsingAnnotationVisitor(classModel);
		}

		@Override
		public void visitLocalVariable(String name, String desc,
				String signature, Label start, Label end, int index) {
			handleTypeDescriptor(classModel, desc);
			handleFieldSignature(classModel, signature);
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
				TypePath typePath, Label[] start, Label[] end, int[] index,
				String desc, boolean visible) {
			handleTypeDescriptor(classModel, desc);
			return new ParsingAnnotationVisitor(classModel);
		}

	}

	public void handleClassOrMethodSignature(ClassModel classModel,
			String signature) {
		if (signature != null) {
			SignatureReader reader = new SignatureReader(signature);
			reader.accept(new ParsingSignatureReader(classModel));
		}
	}

	public void handleFieldSignature(ClassModel classModel, String signature) {
		if (signature != null) {
			SignatureReader reader = new SignatureReader(signature);
			reader.acceptType(new ParsingSignatureReader(classModel));
		}

	}

	public void handleMethodDescriptor(ClassModel classModel, String desc) {
		Type type = Type.getType(desc);
		handleType(classModel, type.getReturnType());
		for (Type p : type.getArgumentTypes()) {
			handleType(classModel, p);
		}
	}

	public class ParsingSignatureReader extends SignatureVisitor {

		private ClassModel classModel;

		public ParsingSignatureReader(ClassModel classModel) {
			super(Opcodes.ASM5);
			this.classModel = classModel;
		}

		@Override
		public SignatureVisitor visitClassBound() {
			return this;
		}

		@Override
		public SignatureVisitor visitInterfaceBound() {
			return this;
		}

		@Override
		public SignatureVisitor visitSuperclass() {
			return this;
		}

		@Override
		public SignatureVisitor visitInterface() {
			return this;
		}

		@Override
		public SignatureVisitor visitParameterType() {
			return this;
		}

		@Override
		public SignatureVisitor visitReturnType() {
			return this;
		}

		@Override
		public SignatureVisitor visitExceptionType() {
			return this;
		}

		@Override
		public SignatureVisitor visitArrayType() {
			return this;
		}

		@Override
		public void visitClassType(String name) {
			handleType(classModel, name);
		}
	}

	void handleTypeDescriptor(ClassModel classModel, String desc) {
		handleType(classModel, Type.getType(desc));
	}

	void handleType(ClassModel classModel, String internalName) {
		if (internalName != null) {
			handleType(classModel, Type.getObjectType(internalName));
		}
	}

	void handleType(ClassModel classModel, Type type) {
		classModel.addUsesClassName(type.getClassName());
	}

}
