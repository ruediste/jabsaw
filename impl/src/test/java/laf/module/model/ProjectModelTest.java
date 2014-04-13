package laf.module.model;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

public class ProjectModelTest {

	@Test
	public void testSelfExportAndAccessibility() {
		ProjectModel project = new ProjectModel();

		ModuleModel a = new ModuleModel(project, "foo");
		project.dependenciesResolved = true;
		project.calculateTransitiveClosures();
		assertEquals(1, a.allExportedModules.size());
		assertEquals(1, a.allAccessibleModules.size());
		assertTrue(a.allExportedModules.contains(a));
		assertTrue(a.allAccessibleModules.contains(a));
	}

	@Test
	public void testImport() {
		ProjectModel project = new ProjectModel();

		ModuleModel a = new ModuleModel(project, "foo");
		ModuleModel b = new ModuleModel(project, "bar");
		a.importedModules.add(b);

		project.dependenciesResolved = true;
		project.calculateTransitiveClosures();
		assertEquals(1, a.allExportedModules.size());
		assertEquals(2, a.allAccessibleModules.size());
		assertTrue(a.allExportedModules.contains(a));
		assertTrue(a.allAccessibleModules.contains(a));
		assertTrue(a.allAccessibleModules.contains(b));
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
		assertFalse(errors.isEmpty());
	}

	@Test
	public void testExport() {
		ProjectModel project = new ProjectModel();

		ModuleModel a = new ModuleModel(project, "foo");
		ModuleModel b = new ModuleModel(project, "bar");
		a.exportedModules.add(b);

		project.dependenciesResolved = true;
		project.calculateTransitiveClosures();
		assertEquals(2, a.allExportedModules.size());
		assertEquals(2, a.allAccessibleModules.size());
		assertTrue(a.allExportedModules.contains(a));
		assertTrue(a.allExportedModules.contains(b));
		assertTrue(a.allAccessibleModules.contains(a));
		assertTrue(a.allAccessibleModules.contains(b));
	}

	@Test
	public void testTransitiveExport() {
		ProjectModel project = new ProjectModel();

		ModuleModel a = new ModuleModel(project, "foo");
		ModuleModel b = new ModuleModel(project, "bar");
		ModuleModel c = new ModuleModel(project, "barc");
		a.importedModules.add(b);
		b.exportedModules.add(c);

		project.dependenciesResolved = true;
		project.calculateTransitiveClosures();

		assertEquals(2, b.allExportedModules.size());

		assertEquals(1, a.allExportedModules.size());
		assertEquals(3, a.allAccessibleModules.size());
		assertTrue(a.allExportedModules.contains(a));
		assertTrue(a.allAccessibleModules.contains(a));
		assertTrue(a.allAccessibleModules.contains(b));
		assertTrue(a.allAccessibleModules.contains(c));
	}

}
