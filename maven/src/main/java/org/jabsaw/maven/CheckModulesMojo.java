package org.jabsaw.maven;

import java.io.File;
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
import org.jabsaw.impl.model.ProjectModel;

/**
 * Checks if the constraints satisfied by the modules are respected.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class CheckModulesMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project.build.outputDirectory}", property = "outputDir", required = true, readonly = true)
	private File outputDirectory;

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

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final ArrayList<String> errors = new ArrayList<>();
		getLog().info("Checking Modules ...");

		final ClassParser parser = new ClassParser();
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

		ProjectModel project = parser.getProject();
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

		getLog().info("Checking class dependencies ...");
		project.checkClassAccessibility(errors);

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
