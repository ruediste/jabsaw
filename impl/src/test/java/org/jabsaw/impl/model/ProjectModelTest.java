package org.jabsaw.impl.model;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

public class ProjectModelTest {

	@Test
	public void testSelfExportAndAccessibility() {
		ProjectModel project = new ProjectModel();

		ModuleModel a = new ModuleModel(project, "foo");
		project.dependenciesResolved = true;
		project.calculateModuleDepenendencies();
		Assert.assertEquals(1, a.allExportedModules.size());
		Assert.assertEquals(1, a.allAccessibleModules.size());
		Assert.assertTrue(a.allExportedModules.contains(a));
		Assert.assertTrue(a.allAccessibleModules.contains(a));
	}

	@Test
	public void testImport() {
		ProjectModel project = new ProjectModel();

		ModuleModel a = new ModuleModel(project, "foo");
		ModuleModel b = new ModuleModel(project, "bar");
		a.importedModules.add(b);

		project.dependenciesResolved = true;
		project.calculateModuleDepenendencies();
		Assert.assertEquals(1, a.allExportedModules.size());
		Assert.assertEquals(2, a.allAccessibleModules.size());
		Assert.assertTrue(a.allExportedModules.contains(a));
		Assert.assertTrue(a.allAccessibleModules.contains(a));
		Assert.assertTrue(a.allAccessibleModules.contains(b));
	}

	@Test
	public void testDependencyCycle() {
		ProjectModel project = new ProjectModel();

		ModuleModel a = new ModuleModel(project, "foo");
		ModuleModel b = new ModuleModel(project, "bar");
		a.importedModules.add(b);
		b.importedModules.add(a);

		project.dependenciesResolved = true;
		ArrayList<String> errors = new ArrayList<>();
		project.checkDependencyCycles(errors);
		Assert.assertFalse(errors.isEmpty());
	}

	@Test
	public void testExport() {
		ProjectModel project = new ProjectModel();

		ModuleModel a = new ModuleModel(project, "foo");
		ModuleModel b = new ModuleModel(project, "bar");
		a.exportedModules.add(b);

		project.dependenciesResolved = true;
		project.calculateModuleDepenendencies();
		Assert.assertEquals(2, a.allExportedModules.size());
		Assert.assertEquals(2, a.allAccessibleModules.size());
		Assert.assertTrue(a.allExportedModules.contains(a));
		Assert.assertTrue(a.allExportedModules.contains(b));
		Assert.assertTrue(a.allAccessibleModules.contains(a));
		Assert.assertTrue(a.allAccessibleModules.contains(b));
	}

	@Test
	public void testTransitiveExport() {
		ProjectModel project = new ProjectModel();

		ModuleModel a = new ModuleModel(project, "foo");
		ModuleModel b = new ModuleModel(project, "bar");
		ModuleModel c = new ModuleModel(project, "barc");
		a.importedModules.add(b);
		b.exportedModules.add(c);

		project.calculateModuleDepenendencies();

		Assert.assertEquals(2, b.allExportedModules.size());

		Assert.assertEquals(1, a.allExportedModules.size());
		Assert.assertEquals(3, a.allAccessibleModules.size());
		Assert.assertTrue(a.allExportedModules.contains(a));
		Assert.assertTrue(a.allAccessibleModules.contains(a));
		Assert.assertTrue(a.allAccessibleModules.contains(b));
		Assert.assertTrue(a.allAccessibleModules.contains(c));
	}

	@Test
	public void innerClassesMergingDependencyFromToplevel() {
		ProjectModel project = new ProjectModel();

		ClassModel a = new ClassModel(project, "a");
		ClassModel b = new ClassModel(project, "b");
		ClassModel c = new ClassModel(project, "c");

		b.outerClassName = "a";
		c.addUsesClassName("b");

		project.resolveDependencies();

		Assert.assertTrue(c.getUsesClasses().contains(a));
		Assert.assertFalse(c.getUsesClasses().contains(b));
		Assert.assertNotNull(project.getClassModel("a"));
		Assert.assertNull(project.getClassModel("b"));
		Assert.assertNotNull(project.getClassModel("c"));
	}

	@Test
	public void innerClassesMergingDependencyFromInner() {
		ProjectModel project = new ProjectModel();

		ClassModel a = new ClassModel(project, "a");
		ClassModel b = new ClassModel(project, "b");
		ClassModel c = new ClassModel(project, "c");
		ClassModel d = new ClassModel(project, "d");

		b.outerClassName = "a";
		d.outerClassName = "c";
		d.addUsesClassName("b");

		project.resolveDependencies();

		Assert.assertTrue(c.getUsesClasses().contains(a));
		Assert.assertFalse(c.getUsesClasses().contains(b));

		Assert.assertNotNull(project.getClassModel("a"));
		Assert.assertNull(project.getClassModel("b"));
		Assert.assertNotNull(project.getClassModel("c"));
		Assert.assertNull(project.getClassModel("d"));
	}
}
