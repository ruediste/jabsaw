package org.jabsaw.impl.model;

import java.util.*;

import org.jgrapht.EdgeFactory;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * Represents a project, consisting of {@link ModuleModel}s and
 * {@link ClassModel}s.
 *
 */
public class ProjectModel {

	/**
	 * Classes by {@link ClassModel#qualifiedName}
	 */
	private final HashMap<String, ClassModel> classes = new HashMap<>();

	/**
	 * Modules by {@link ModuleModel#qualifiedNameOfRepresentingClass}
	 */
	final private HashMap<String, ModuleModel> modules = new HashMap<>();

	public Map<String, ClassModel> getClasses() {
		return Collections.unmodifiableMap(classes);
	}

	public ClassModel getClassModel(String qualifiedName) {
		return classes.get(qualifiedName);
	}

	public Map<String, ModuleModel> getModules() {
		return Collections.unmodifiableMap(modules);
	}

	public Set<ModuleModel> getMatchingModules(ClassModel clazz) {
		Set<ModuleModel> result = new HashSet<>();
		for (ModuleModel module : modules.values()) {
			if (module.isIncluded(clazz)) {
				result.add(module);
			}
		}
		return result;
	}

	void addClass(ClassModel clazz) {
		classes.put(clazz.getQualifiedName(), clazz);
	}

	void addModule(ModuleModel module) {
		modules.put(module.getQualifiedNameOfRepresentingClass(), module);
	}

	boolean dependenciesResolved = false;

	/**
	 * Resolve class names recorded during parsing to the actual model objects
	 * and calculate transitive closures.
	 */
	public void resolveDependencies() {
		if (dependenciesResolved) {
			throw new Error("Can resolve dependencies only once");
		}
		dependenciesResolved = true;

		for (ClassModel classModel : classes.values()) {
			classModel.resolveDependencies();
		}

		for (ModuleModel moduleModel : modules.values()) {
			moduleModel.resolveDependencies();
		}
	}

	boolean transitiveClosuresCalculated = false;

	public void calculateTransitiveClosures() {
		if (!dependenciesResolved) {
			throw new Error("call resolveDependencies() first");
		}
		if (transitiveClosuresCalculated) {
			throw new Error("can calculate transitive closures only once");
		}
		transitiveClosuresCalculated = true;

		calculateExportedModules();
		calculateAccessibleModules();
	}

	private static class Edge {

	}

	public void checkDependencyCycles(List<String> errors) {
		if (!dependenciesResolved) {
			throw new Error("Call resolveDependencies() first");
		}

		SimpleDirectedGraph<ModuleModel, Edge> g = buildExportGraph();
		// add import dependencies
		for (ModuleModel module : modules.values()) {
			for (ModuleModel imported : module.importedModules) {
				g.addEdge(module, imported);
			}
		}

		CycleDetector<ModuleModel, Edge> detector = new CycleDetector<>(g);
		Set<ModuleModel> cycleModules = detector.findCycles();

		if (!cycleModules.isEmpty()) {
			errors.add("Found cycle in module dependency graph. Involved modules: "
					+ cycleModules);
		}
	}

	private void calculateExportedModules() {
		SimpleDirectedGraph<ModuleModel, Edge> g = buildExportGraph();

		TransitiveClosure.INSTANCE.closeSimpleDirectedGraph(g);

		for (ModuleModel module : modules.values()) {
			module.allExportedModules.add(module);
			for (Edge e : g.outgoingEdgesOf(module)) {
				module.allExportedModules.add(g.getEdgeTarget(e));
			}
		}
	}

	private SimpleDirectedGraph<ModuleModel, Edge> buildExportGraph() {
		SimpleDirectedGraph<ModuleModel, Edge> g = new SimpleDirectedGraph<>(
				new EdgeFactory<ModuleModel, Edge>() {

					@Override
					public Edge createEdge(ModuleModel sourceVertex,
							ModuleModel targetVertex) {
						return new Edge();
					}
				});

		// add all Modules
		for (ModuleModel module : modules.values()) {
			g.addVertex(module);
		}

		// add export dependencies
		for (ModuleModel module : modules.values()) {
			for (ModuleModel exported : module.exportedModules) {
				g.addEdge(module, exported);
			}
		}
		return g;
	}

	private void calculateAccessibleModules() {
		for (ModuleModel module : modules.values()) {
			// add module itself
			module.allAccessibleModules.add(module);

			// add imported and exported modules, including transitive exports
			Set<ModuleModel> set = new HashSet<>();
			set.addAll(module.exportedModules);
			set.addAll(module.importedModules);

			for (ModuleModel imported : set) {
				module.allAccessibleModules.addAll(imported.allExportedModules);
			}
		}
	}

	public ModuleModel getModule(String qualifiedNameOfRepresentingClass) {
		return modules.get(qualifiedNameOfRepresentingClass);
	}

	/**
	 * Check the classes for dependencies and collect all errors
	 */
	public void checkClasses(List<String> errors) {
		for (ClassModel clazz : classes.values()) {
			clazz.checkDependencies(errors);
		}
	}

	/**
	 * Check if all classes belong to a module
	 */
	public void checkAllClassesInModule(List<String> errors) {
		for (ClassModel clazz : classes.values()) {
			if (clazz.getModule() == null) {
				errors.add("Class " + clazz + " is in no module");
			}
		}
	}

	public String details() {
		StringBuilder sb = new StringBuilder();
		for (ModuleModel module : modules.values()) {
			sb.append(module.details());
			sb.append("\n");
		}
		for (ClassModel clazz : classes.values()) {
			sb.append(clazz.details());
			sb.append("\n");
		}
		return sb.toString();
	}

	public boolean isDependenciesResolved() {
		return dependenciesResolved;
	}

	public boolean isTransitiveClosuresCalculated() {
		return transitiveClosuresCalculated;
	}
}
