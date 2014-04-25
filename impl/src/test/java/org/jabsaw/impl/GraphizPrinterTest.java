package org.jabsaw.impl;

import java.io.IOException;
import java.io.PrintWriter;

import org.jabsaw.impl.model.ProjectModel;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

public class GraphizPrinterTest {

	@Test
	public void test() throws IOException {
		GraphizPrinter printer = new GraphizPrinter();
		ClassParser parser = new ClassParser();
		parser.parse(new ClassReader(TestClassA.class.getName()));
		parser.parse(new ClassReader(TestClassB.class.getName()));
		parser.parse(new ClassReader(TestModuleA.class.getName()));
		parser.parse(new ClassReader(TestModuleB.class.getName()));

		ProjectModel project = parser.getProject();
		project.resolveDependencies();
		printer.print(project, new PrintWriter(System.out));
	}
}
