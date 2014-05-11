package org.jabsaw.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jabsaw.impl.ClassParser;
import org.jabsaw.impl.ClassParser.DirectoryParsingCallback;
import org.jabsaw.impl.GraphizPrinter;
import org.jabsaw.impl.model.ProjectModel;

/**
 * Checks if the constraints satisfied by the modules are respected.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class CheckModulesMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
	private File outputDirectory;

	@Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
	private File targetDirectory;
	/**
	 * If true, the modules are checked for dependency cycles. Default: true.
	 */
	@Parameter(defaultValue = "true", required = true)
	private boolean checkDepedencyCycles;

	/**
	 * If true, all classes have to be in a module. Default: false
	 */
	@Parameter(defaultValue = "false", required = true)
	private boolean checkAllClassesInModule;

	/**
	 * If true, check that all classes respect module boundaries. Default: true
	 */
	@Parameter(defaultValue = "true", required = true)
	private boolean checkModuleBoundaries;

	/**
	 * If true, modules are typically identified in strings by their name
	 * instead of the fully qualified name of the representing class. Default:
	 * false
	 */
	@Parameter(defaultValue = "false", required = true)
	private boolean useModuleNames;

	/**
	 * If true, generate a module graph Graphviz file. Default: false
	 */
	@Parameter(defaultValue = "false", required = true)
	private boolean createModuleGraphvizFile;

	/**
	 * If true, the generated module graph includes the individual classes.
	 * Default: false
	 */
	@Parameter(defaultValue = "false", required = true)
	private boolean moduleGraphIncludesClasses = false;

	/**
	 * Set the output format of the module graph. If set, the dot command will
	 * be executed. Implies createModuleGraphvizFile. Examples: ps, png, gif,
	 * svg. Default: empty
	 */
	@Parameter(defaultValue = "", required = true)
	private String moduleGraphFormat;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final ArrayList<String> errors = new ArrayList<>();
		getLog().info("Checking Modules ...");

		final ClassParser parser = new ClassParser();
		ProjectModel project = parser.getProject();
		project.setUseModuleNames(useModuleNames);

		parser.parseDirectory(errors, outputDirectory.toPath(),
				new DirectoryParsingCallback() {

			@Override
			public void parsingFile(Path file) {
				getLog().debug("parsing " + file.toString());
			}

			@Override
			public void error(String error) {
				errors.add(error);
			}
		});

		project.resolveDependencies();

		getLog().debug("Project Details:\n" + project.details());

		if (checkDepedencyCycles) {
			getLog().info("Checking module dependencies for cycles ...");
			project.checkDependencyCycles(errors);
		}

		if (checkAllClassesInModule) {
			getLog().info("Checking if all classes are in a module ...");
			project.checkAllClassesInModule(errors);
		}

		if (checkModuleBoundaries) {
			getLog().info("Checking class dependencies ...");
			project.checkClassAccessibility(errors);
		}

		if (!targetDirectory.exists()) {
			getLog().info(
					"Target directory does not exist, skipping module graph creation");
		} else {
			if (createModuleGraphvizFile || !moduleGraphFormat.isEmpty()) {
				GraphizPrinter printer = new GraphizPrinter();
				try {
					printer.print(project, new File(targetDirectory,
							"moduleGraph.dot"), moduleGraphIncludesClasses);
				} catch (IOException e) {
					throw new RuntimeException(
							"Error while generating module graph .dot file", e);
				}
			}

			if (!moduleGraphFormat.isEmpty()) {
				Process process;
				try {
					process = new ProcessBuilder(
							moduleGraphIncludesClasses ? "sfdp" : "dot", "-T",
							moduleGraphFormat, "-o", "moduleGraph."
									+ moduleGraphFormat, "moduleGraph.dot")
							.inheritIO().directory(targetDirectory).start();
					process.waitFor();
				} catch (Exception e) {
					throw new RuntimeException(
							"Error while running the dot command to produce a module graph",
							e);
				}
			}
		}

		if (!errors.isEmpty()) {
			getLog().error("Errors while checking modules:");
			for (String s : errors) {
				getLog().error(s);
			}
			throw new MojoFailureException(
					"Error while checking module dependencies. See log for details");
		}

		getLog().info("Modules checked");
	}

}
