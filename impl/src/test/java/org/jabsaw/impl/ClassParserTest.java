package org.jabsaw.impl;

import java.io.IOException;
import java.util.Set;

import org.jabsaw.Module;
import org.jabsaw.impl.model.ClassModel;
import org.jabsaw.impl.model.ModuleModel;
import org.jabsaw.impl.model.ProjectModel;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

public class ClassParserTest {

	private static class Foo {
	}

	@Module(name = "Test1", description = "Desc", exclude = Foo.class, excludePattern = "bar.*", exported = Foo.class, imported = Foo.class, include = Foo.class, includePattern = "bar.bar", includePackage = false)
	private static class TestModule {

	}

	@Test
	public void parseModule() throws IOException {
		ClassParser parser = new ClassParser();
		ClassReader reader = new ClassReader(getClass().getResourceAsStream(
				"ClassParserTest$TestModule.class"));
		parser.parse(reader);
		ModuleModel module = parser.getProject().getModule(
				TestModule.class.getName());

		Assert.assertEquals("Test1", module.getName());
		Assert.assertEquals("Desc", module.getDescription());
		Assert.assertEquals(TestModule.class.getName(),
				module.getQualifiedNameOfRepresentingClass());
		Assert.assertTrue(module.getExclusionPatterns().toString()
				.contains(Foo.class.getName()));
		Assert.assertTrue(module.getExclusionPatterns().toString()
				.contains("bar.*"));
		Assert.assertTrue(module.getExportedModuleNames().contains(
				Foo.class.getName()));
		Assert.assertTrue(module.getImportedModuleNames().contains(
				Foo.class.getName()));
		Assert.assertTrue(module.getInclusionPatterns().toString()
				.contains(Foo.class.getName()));
		Assert.assertTrue(module.getInclusionPatterns().toString()
				.contains("bar.bar"));

		Assert.assertFalse(module.getInclusionPatterns().toString()
				.contains(".*"));
	}

	private @interface TestNestedAnnotationDirect {

	}

	private @interface TestNestedAnnotationArray {

	}

	private @interface TestAnnotationClass {
		TestNestedAnnotationDirect foo();

		TestNestedAnnotationArray[] bar();
	}

	private static class TestField {
	}

	private static class TestMethodReturn {
	}

	private static class TestMethodParameter {
	}

	private static class TestSuperClass {

	}

	private interface TestInterface {

	}

	private @interface TestAnnotationField {

	}

	private @interface TestAnnotationMethod {

	}

	private @interface TestAnnotationMethodParameter {

	}

	private class TestLocalVariabe {

	}

	private class TestCalledMethodReturn {

	}

	private class TestCalledMethodParameter {

	}

	private class TestAccessedField {

	}

	private class TestLoadedType {

	}

	private class TestLoadedArrayType {

	}

	private class TestInstanceOfType {

	}

	private @interface TestLocalVariableAnnotation {

	}

	private static TestCalledMethodReturn testStaticClass(
			TestCalledMethodParameter p, Object o) {
		return null;

	}

	private static TestAccessedField testField;

	@TestAnnotationClass(bar = { @TestNestedAnnotationArray }, foo = @TestNestedAnnotationDirect)
	private static class TestClass extends TestSuperClass implements
			TestInterface {

		@TestAnnotationField
		TestField foo;

		@TestAnnotationMethod
		TestMethodReturn method(
				@TestAnnotationMethodParameter TestMethodParameter p)
				throws RuntimeException {
			@SuppressWarnings("unused")
			@TestLocalVariableAnnotation
			TestLocalVariabe foo = null;
			ClassParserTest.testStaticClass(null, ClassParserTest.testField);
			System.out.println(TestLoadedType.class);
			System.out.println(TestLoadedArrayType[].class);
			System.out.println(new Object() instanceof TestInstanceOfType);
			return null;
		}

	}

	@Test
	public void parseClass() throws IOException {
		ClassParser parser = new ClassParser();
		ClassReader reader = new ClassReader(getClass().getResourceAsStream(
				"ClassParserTest$TestClass.class"));
		parser.parse(reader);
		ClassModel clazz = parser.getProject().getClassModel(
				TestClass.class.getName());

		assertContainsNames(clazz.getUsesClassNames(),
				TestAnnotationClass.class, TestNestedAnnotationDirect.class,
				TestNestedAnnotationArray.class, TestField.class,
				TestMethodReturn.class, TestMethodParameter.class,
				TestInterface.class, TestSuperClass.class,
				RuntimeException.class, TestAnnotationField.class,
				TestAnnotationMethod.class,
				TestAnnotationMethodParameter.class,
				TestCalledMethodReturn.class,
				TestCalledMethodParameter.class,
				// TestLocalVariableAnnotation.class,
				TestLoadedType.class, TestLoadedArrayType.class,
				TestInstanceOfType.class, TestLocalVariabe.class,
				TestAccessedField.class);
	}

	private void assertContainsNames(Set<String> names, Class<?>... cls) {
		for (Class<?> c : cls) {
			assertContainsName(c, names);
		}
	}

	private void assertContainsName(Class<?> cls, Set<String> names) {
		assertContains(cls.getName(), names);
	}

	private <T> void assertContains(T element, Set<T> set) {
		Assert.assertTrue("expected " + element + " to be contained in " + set,
				set.contains(element));
	}

	@Test
	public void parseClassWithInnerClass() throws IOException {
		ClassParser parser = new ClassParser();
		ClassReader reader = new ClassReader(getClass().getResourceAsStream(
				"ClassNestingTestClass.class"));
		parser.parse(reader);
		reader = new ClassReader(getClass().getResourceAsStream(
				"ClassNestingTestClass$InnerClass.class"));
		parser.parse(reader);
		ProjectModel project = parser.getProject();
		// project.resolveDependencies();
		Assert.assertEquals(2, project.getClasses().values().size());
		Assert.assertFalse(project.getClassModel(ClassNestingTestClass.class
				.getName()).innerClassNames.isEmpty());
		project.resolveDependencies();
		Assert.assertEquals(1, project.getClasses().values().size());

	}

}
