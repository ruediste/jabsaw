package org.jabsaw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabsaw.impl.ClassParser;
import org.jabsaw.impl.ClassParser.DirectoryParsingCallback;
import org.jabsaw.impl.GraphizPrinter;
import org.jabsaw.impl.model.ProjectModel;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Main {

	@Option(name = "-cc", usage = "check for dependency cycles. default: true")
	private boolean checkDepedencyCycles = true;

	@Option(name = "-cm", usage = "check that all classes are in a module. default: false")
	private boolean checkAllClassesInModule = false;

	@Option(name = "-cb", usage = "check that all classes respect the module boundaries. default: true")
	private boolean checkClassAccessibility = true;

	@Option(name = "-graphiz", usage = "If true, generate a module graph Graphviz file. Default: false")
	private boolean createModuleGraphvizFile = false;

	@Option(name = "-n", usage = "If true, modules are typically identified in strings by their name instead of the fully qualified name of the representing class. Default: false")
	private boolean useModuleNames = false;

	@Option(name = "-graphvizClasses", usage = "If true, the generated module graph includes the individual classes. Default: false")
	private boolean moduleGraphIncludesClasses = false;

	@Option(name = "-graphvizFormat", usage = "Set the output format of the module graph. If set, the dot command will be executed. Implies -graphviz. Examples: ps, png, gif, svg. Default: empty")
	private String moduleGraphFormat = "";

	@Option(name = "-v", usage = "verbose output. default: false")
	private boolean verbose = false;

	@Argument
	List<File> inputDirectories = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		new Main().doMain(args);
	}

	public void doMain(String[] args) throws IOException {
		parseCmdLine(args);

		final ArrayList<String> errors = new ArrayList<>();
		System.out.println("Checking Modules ...");

		final ClassParser parser = new ClassParser();
		ProjectModel project = parser.getProject();
		project.setUseModuleNames(useModuleNames);

		DirectoryParsingCallback callback = new DirectoryParsingCallback() {

			@Override
			public void parsingFile(Path file) {
				if (verbose) {
					System.out.println("parsing " + file.toString());
				}
			}

			@Override
			public void error(String error) {
				errors.add(error);
			}
		};

		for (File f : inputDirectories) {
			parser.parseDirectory(errors, f.toPath(), callback);
		}

		project.resolveDependencies();

		if (verbose) {
			System.out.println("Project Details:\n" + project.details());
		}

		if (checkDepedencyCycles) {
			System.out.println("Checking module dependencies for cycles ...");
			project.checkDependencyCycles(errors);
		}

		if (checkAllClassesInModule) {
			System.out.println("Checking if all classes are in a module ...");
			project.checkAllClassesInModule(errors);
		}

		if (checkClassAccessibility) {
			System.out.println("Checking class dependencies ...");
			project.checkClassAccessibility(errors);
		}

		if (!errors.isEmpty()) {
			System.err.println("Errors while checking modules:");
			for (String s : errors) {
				System.err.println(s);
			}
			System.exit(1);
		}

		if (createModuleGraphvizFile) {
			GraphizPrinter printer = new GraphizPrinter();
			try {
				printer.print(project, new File("moduleGraph.dot"),
						moduleGraphIncludesClasses);
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
				.inheritIO().start();
				process.waitFor();
			} catch (Exception e) {
				throw new RuntimeException(
						"Error while running the dot command to produce a module graph",
						e);
			}
		}

		System.out.println("Modules checked");

	}

	private void parseCmdLine(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);

		// if you have a wider console, you could increase the value;
		// here 80 is also the default
		parser.setUsageWidth(80);

		try {
			// parse the arguments.
			parser.parseArgument(args);

			// you can parse additional arguments if you want.
			// parser.parseArgument("more","args");

			if (inputDirectories.isEmpty()) {
				throw new CmdLineException(parser,
						"No input direcotries are given");
			}

		} catch (CmdLineException e) {
			// if there's a problem in the command line,
			// you'll get this exception. this will report
			// an error message.
			System.err.println(e.getMessage());
			System.err
			.println("java -jar ... org.jabsaw.Main [options...] dirs...");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();

			System.exit(1);
		}

		// handle option implies
		if (!moduleGraphFormat.isEmpty()) {
			createModuleGraphvizFile = true;
		}
	}
}
