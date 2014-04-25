package org.jabsaw.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.jabsaw.impl.model.ClassModel;
import org.jabsaw.impl.model.ModuleModel;
import org.jabsaw.impl.model.ProjectModel;

/**
 * Helper class to print a {@link ProjectModel} to a Graphviz file
 */
public class GraphizPrinter {

	/**
	 * Print the given {@link ProjectModel} as Graphviz .dot file to the given
	 * file
	 */
	public void print(ProjectModel project, File f, boolean includeClasses)
			throws IOException {
		FileWriter writer = new FileWriter(f);
		print(project, writer, includeClasses);
		writer.close();
	}

	/**
	 * Print the given {@link ProjectModel} as Graphviz .dot file to the given
	 * writer
	 */
	public void print(ProjectModel project, Writer w) {
		print(project, w, false);
	}

	/**
	 * Print the given {@link ProjectModel} as Graphviz .dot file to the given
	 * writer. If includeClasses is true, the individual classes are printed as
	 * well.
	 */
	public void print(ProjectModel project, Writer w, boolean includeClasses) {
		PrintWriter out = new PrintWriter(w);

		out.println("digraph G {");
		if (includeClasses) {
			out.println("ratio=1;");
		} else {
			out.println("overlap=false;");
			out.println("spline=true;");
		}

		int i = 0;
		// print nodes
		for (ModuleModel module : project.getModules().values()) {
			if (includeClasses) {
				out.printf("subgraph cluster%d {\n", i++);
			}

			out.printf("  \"MODULE$%s\" [label=\"%s\", shape=box%s];\n", module
					.getQualifiedNameOfRepresentingClass(), module
					.getSimpleName(), includeClasses ? ", color=green" : "");

			if (includeClasses) {
				for (ClassModel clazz : module.getClasses()) {
					out.printf("  \"CLASS$%s\" [label=\"%s\"];\n",
							clazz.getQualifiedName(), clazz.getSimpleName());
				}
			}

			if (includeClasses) {
				out.printf("}\n");
			}
		}

		// print edges

		for (ModuleModel module : project.getModules().values()) {

			for (ModuleModel imported : module.getImportedModules()) {
				out.printf("\"MODULE$%s\"->\"MODULE$%s\"%s;\n",
						module.getQualifiedNameOfRepresentingClass(),
						imported.getQualifiedNameOfRepresentingClass(),
						includeClasses ? " [color=green]" : "");
			}
			for (ModuleModel exported : module.getExportedModules()) {
				out.printf("\"MODULE$%s\"->\"MODULE$%s\" [color=green];\n",
						module.getQualifiedNameOfRepresentingClass(),
						exported.getQualifiedNameOfRepresentingClass());
			}

		}

		if (includeClasses) {
			for (ClassModel clazz : project.getClasses().values()) {
				for (ClassModel used : clazz.getUsesClasses()) {
					if (used.equals(clazz)) {
						continue;
					}
					out.printf("\"CLASS$%s\"->\"CLASS$%s\";\n",
							clazz.getQualifiedName(), used.getQualifiedName());
				}
			}
		}

		out.println("}");
		out.flush();
	}
}
