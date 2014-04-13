package laf.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import laf.module.model.ClassModel;
import laf.module.model.ModuleModel;

import org.junit.Test;
import org.objectweb.asm.ClassReader;

public class ClassParserTest {

	private static class Foo {
	}

	@LafModule(exclude = Foo.class, excludePattern = "bar.*", exported = Foo.class, imported = Foo.class, include = Foo.class, includePattern = "bar.bar")
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
		assertEquals(TestModule.class.getName(),
				module.getQualifiedNameOfRepresentingClass());
		assertTrue(module.getExclusionPatterns().toString()
				.contains(Foo.class.getName()));
		assertTrue(module.getExclusionPatterns().toString().contains("bar.*"));
		assertTrue(module.getExportedModuleNames()
				.contains(Foo.class.getName()));
		assertTrue(module.getImportedModuleNames()
				.contains(Foo.class.getName()));
		assertTrue(module.getInclusionPatterns().toString()
				.contains(Foo.class.getName()));
		assertTrue(module.getInclusionPatterns().toString().contains("bar.bar"));
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
			testStaticClass(null, testField);
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
				TestCalledMethodReturn.class, TestCalledMethodParameter.class,
				// TestLocalVariableAnnotation.class,
				TestLocalVariabe.class, TestAccessedField.class);
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
		assertTrue("expected " + element + " to be contained in " + set,
				set.contains(element));
	}
}
