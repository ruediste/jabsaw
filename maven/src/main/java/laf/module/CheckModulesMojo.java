package laf.module;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import laf.module.model.ProjectModel;

import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class CheckModulesMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project.build.outputDirectory}", property = "outputDir", required = true, readonly = true)
	private File outputDirectory;

	@Parameter(defaultValue = "true", required = true)
	private boolean checkDepedencyCycles;

	@Parameter(defaultValue = "false", required = true)
	private boolean checkAllClassesInModule;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final ArrayList<String> errors = new ArrayList<>();
		getLog().info("Checking Modules ...");

		final ClassParser parser = new ClassParser();
		try {
			Files.walkFileTree(outputDirectory.toPath(),
					new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile()
							&& file.getFileName().toString()
							.endsWith(".class")) {
						getLog().debug("parsing " + file.toString());
						try {
									parser.parse(file.toFile());
						} catch (Throwable t) {
							getLog().error(
											"Error while parsing " + file, t);
							errors.add("Error whil parsing " + file);
						}
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file,
						IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir,
						IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Error while reading input files", e);
		}

		ProjectModel project = parser.getProject();
		project.resolveDependencies();
		project.calculateTransitiveClosures();

		getLog().debug("Project Details:\n" + project.details());

		if (checkDepedencyCycles) {
			getLog().info("Checking module dependencies for cycles ...");
			project.checkDependencyCycles(errors);
		}

		if (checkAllClassesInModule) {
			getLog().info("Checking if all classes are in a module ...");
			project.checkAllClassesInModule(errors);
		}

		project.checkClasses(errors);
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
