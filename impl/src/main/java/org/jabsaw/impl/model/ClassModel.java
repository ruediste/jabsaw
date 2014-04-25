package org.jabsaw.impl.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassModel implements ModelNode {

	private final ProjectModel projectModel;
	private final String qualifiedName;

	ClassModel outerClass;

	public ClassModel(ProjectModel projectModel, String qualifiedName) {
		this.projectModel = projectModel;
		this.qualifiedName = qualifiedName;
		projectModel.addClass(this);
	}

	ModuleModel module;

	final Set<ClassModel> usesClasses = new HashSet<>();
	final Set<String> usesClassNames = new HashSet<>();

	public String getQualifiedName() {
		return qualifiedName;
	}

	public ProjectModel getProjectModel() {
		return projectModel;
	}

	public Set<ClassModel> getUsesClasses() {
		return Collections.unmodifiableSet(usesClasses);
	}

	public void addUsesClass(ClassModel clazz) {
		usesClasses.add(clazz);
	}

	public Set<String> getUsesClassNames() {
		return Collections.unmodifiableSet(usesClassNames);
	}

	public void addUsesClassName(String name) {
		usesClassNames.add(name);
	}

	public ModuleModel getModule() {
		return module;
	}

	public void setModule(ModuleModel module) {
		if (this.module != null) {
			this.module.classes.remove(this);
		}
		this.module = module;
		if (module != null) {
			module.addClass(this);
		}
	}

	public void resolveModule() {
		// resolve module
		Set<ModuleModel> matchingModules = projectModel
				.getMatchingModules(this);
		if (matchingModules.size() > 1) {
			throw new RuntimeException("Multiple Modules found for class "
					+ qualifiedName + ": " + matchingModules);
		}

		if (matchingModules.size() == 1) {
			setModule(matchingModules.iterator().next());
		}
	}

	public String outerClassName;

	public void resolveUsedClasses() {
		// resolve usage dependencies
		for (String usesClassName : usesClassNames) {
			ClassModel classModel = projectModel.getClassModel(usesClassName);
			if (classModel != null) {
				addUsesClass(classModel.getToplevelClass());
			}
		}
	}

	public void resolveOuterClass() {
		// resolve inner classes
		for (String name : innerClassNames) {
			ClassModel clazz = projectModel.getClassModel(name);
			if (clazz != null) {
				clazz.outerClass = this;
			}
		}

		// resolve outer class
		if (outerClassName != null) {
			ClassModel clazz = projectModel.getClassModel(outerClassName);
			if (clazz != null) {
				outerClass = clazz;
			}
		}
	}

	public void checkAccessibilityOfUsedClasses(List<String> errors) {
		if (module == null) {
			return;
		}

		for (ClassModel clazz : usesClasses) {
			if (!module.isAccessible(clazz)) {
				errors.add("Class " + this + " references class " + clazz
						+ ", which is not accessible for classes in module "
						+ module);
			}
		}
	}

	@Override
	public String toString() {
		return qualifiedName;
	}

	public String details() {
		StringBuilder sb = new StringBuilder();
		sb.append("Class " + qualifiedName + "\n");
		sb.append("module: " + module + "\n");
		sb.append("usesClasses: " + usesClasses + "\n");
		return sb.toString();
	}

	private ClassModel toplevelClass;
	public Set<String> innerClassNames = new HashSet<>();

	ClassModel getToplevelClass() {
		Set<ClassModel> visited = new HashSet<>();
		if (toplevelClass == null) {
			toplevelClass = this;
			while (true) {
				if (toplevelClass.outerClass != null
						&& visited.add(toplevelClass)) {
					toplevelClass = toplevelClass.outerClass;
				} else {
					break;
				}
			}
		}
		return toplevelClass;
	}

	/**
	 * All classes this class depends upon. Includes all transitive dependencies
	 * of this class, even if the dependencies are not in a module themselves.
	 */
	public Set<ClassModel> getAllClassDependencies() {
		HashSet<ClassModel> result = new HashSet<>();
		ClassModel.getAllClassDependencies(result, this);
		return result;
	}

	static void getAllClassDependencies(HashSet<ClassModel> result,
			ClassModel classModel) {
		if (!result.add(classModel)) {
			return;
		}
		for (ClassModel clazz : classModel.usesClasses) {
			ClassModel.getAllClassDependencies(result, clazz);
		}
	}

	public String getSimpleName() {
		String[] parts = qualifiedName.split("\\.");
		return parts[parts.length - 1];
	}
}
