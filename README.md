JabSaw
======

Lightweigth compile time module system, aimed at package dependency organization. 

Motivation
----------
Keeping track of coupling is very helpful in reaching a sound software design. While
this can be achieved by having a plan and being disciplined in adhering to it, tool
support proves to be very helpful in detecting deviations from it.

The most used tool in the java ecosystem is to use separate compilation units 
(Eclipse projects, Maven artifacts, OSGi modules, ...). Unfortunately, this is practical for rather coarse
grained units only.

Then there are various tools like [Dependency Finder](http://depfind.sourceforge.net/) and
[Class Cycle](http://classycle.sourceforge.net/index.html). They are mainly aimed at analysing
dependencies after the software has been written. There are possibilities to express dependency
rules, but they are described in a separate text file.

Especially when writing infrastructure code (frameworks) expressing dependencies at a package level is desireable.
This is the problem JabSaw is aimed at.

JabSaw is designed specifically to express static package level dependencies. It does not contain a version scheme
and neither has runtime components (like OSGi) nor a build/dependency resolution system (like Maven).

Defining Modules
----------------
Modules are defined by annotating a class with the `org.jabsaw.Module` annotation. This class is called the representing
class of the module. Representing classes are used instead of annotating the package in a `package-info.java` since this allows
to easily express relationships between modules. Example:

	import org.jabsaw.Module;
  
	@Module(imported = ApiModule.class )
	public class ImplModule{}
  
This defines the ImplModule which depends on the ApiModule. We suggest naming the class `<module name>Module` and not using it
for anything else. Then we put our package-level documentation in the javadoc of the module.

By default, all classes in the package of the module representing class are included in the module. This can be changed using
class name patterns or by directly including and excluding classes. See the javadoc for details.  

Classes of a modules may only use classes which are accessible from the module. The following classes are accessible:
* classes which are not in any module
* classes of the module itself
* classes of imported modules

As shown above, imports are defined by listing them in the `import` element of the `@Module` annotation. Multiple
modules are imported like this:

	@Module(imported = {ModuleA.class, ModuleB.class, ...}) 

In addition the the import mechanism there is also an export mechanism. If all clients of a module A will need module B, since 
the classes of module B are used in the interface of module A, module A should export module B. This makes the classes of module B
accessible to all modules which import module A. This mechanism is recursive. In the example above, if B exports a module C, clients of
A will also be able to access C.


Installation
------------
We are not yet on Maven Central, but installation is easy:
	
	git clone git@github.com:ruediste1/jabsaw.git
	cd jabsaw
	mvn install
	
This will install the artifacts into the local Maven repository.

### Accessing the API
The api can be found in `jabsaw/api/target/jabsaw-api-<version>.jar`. When using Maven, add the following to your `pom.xml`:

	...
	<dependencies>
		...
		<dependency>
			<groupId>org.jabsaw</groupId>
			<artifactId>jabsaw-api</artifactId>
			<version>1.0-SNAPSHOT</version>
		</dependency>
		...
	</dependencies>
	...
	
### Checking with the Maven Plugin
JabSaw comes with a Maven plugin to check module dependencies. Add

	...
	<build>
		<plugins>
		...
			<plugin>
				<groupId>org.jabsaw</groupId>
				<artifactId>jabsaw-maven-plugin</artifactId>
				<version>1.0-SNAPSHOT</version>
				<configuration>
					...
				</configuration>
				<executions>
					<execution>
						<phase>process-classes</phase>
						<goals>
							<goal>check</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
			...
		</plugins>
	</build>
	...

to your pom.xml. The documentation to the configuration parameters can be accessed via:

	mvn jabsaw:help -Ddetail=true -Dgoal=check
 
Currently, the following paramerters are available:

	checkAllClassesInModule
	  If true, all classes have to be in a module. 
	  Default: false
	
	checkDepedencyCycles
	  If true, the modules are checked for dependency cycles. 
	  Default: true.
	
	checkModuleBoundaries
	  If true, check that all classes respect module boundaries. 
	  Default: true
	
	createModuleGraphvizFile
	  If true, generate a module graph Graphviz file. 
	  Default: false
	
	moduleGraphFormat
	  Set the output format of the module graph. If set, the dot command will be
	  executed. Implies createModuleGraphvizFile. Examples: ps, png, gif, svg.
	  Default: empty
	
	moduleGraphIncludesClasses
	  If true, the generated module graph includes the individual classes.
	  Default: false

By setting the moduleGraphFormat parameter, a graph of the module dependencies is created in the `target/` directory. 
This requires graphviz to be installed (the `dot` program). Under Ubuntu simply type

	sudo apt-get install graphviz


### Checking with the Command Line Interface
If you are not using Maven, you can use the command line interface. All dependencies are packed within the `jabsaw/cli/target/jabsaw-cli-<version>.jar` file.

	java -jar jabsaw-cli-1.0-SNAPSHOT.jar .

When no parameters are given, a help screen will be shown.

### Using Unit Test Interface
JabSaw is accessible from within unit tests. Add the util artifact to the `pom.xml`:

	<dependency>
		<groupId>org.jabsaw</groupId>
		<artifactId>jabsaw-util</artifactId>
		<version>1.0-SNAPSHOT</version>
		<scope>test</scope>
	</dependency>
	
This makes the `org.jabasaw.util.Modules` class accessible, which provides certain
module related methods. When the first method is called, the modules are read form the
test classpath. This can be configured by adding a `jabsaw.properties` file to the 
root test classpath. When using main, put the file under `src/test/resources/jabsaw.properties`.
The following properties are supported:

	includeJars = true|false
	  When set to true, jar files on the classpath will be parsed
	  Default: true
	excludePath = ...
	  When defined, any resource on the classpath containing the 
	  provided string will not be scanned.
	  
Example file working well with maven:

	includeJars = false
	excludePath = /target/test-classes/
	
To perform the various checks, use the `checkXXX()` methods. To integrate with [Arquillian](http://arquillian.org/),
the `getAllRequiredClasses()` and `getClasses()` methods come in very handy. For example, the following will include all
required classes of the `UrlMappingModule`:

	ShrinkWrap
		.create(WebArchive.class)
		.addClasses(Modules.getAllRequiredClasses(UrlMappingModule.class));

License
-------
[Apache License, Version 2.0](LICENSE.txt)

Versioning
----------
We use a single major/minor versioning. Updates of the minor version do not result
in breaking changes, updates of the major version can.


### Creating Releases
During development, the version is always set to the next version with the -SNAPSHOT suffix.
A release can be perfomed with

	mvn release:clean release:prepare
	
by answering the prompts for versions and tags, followed by
	
	mvn release:perform

Finally, update the `settings.xml` file to contain your credentials and put the release 
to the central repository by

	mvn nexus-staging:release
