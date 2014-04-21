package org.jabsaw.impl.model;

import org.junit.Assert;
import org.junit.Test;

public class ClassModelTest {

	@Test
	public void testGetToplevelClass() throws Exception {
		ProjectModel project = new ProjectModel();
		ClassModel modelA = new ClassModel(project, "toplevel");
		ClassModel modelB = new ClassModel(project, "inner");
		modelB.outerClass = modelA;
		Assert.assertEquals(modelA, modelB.getToplevelClass());
	}

}
