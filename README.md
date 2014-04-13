JabSaw
======

Lightweigth compile time module system, aimed at package dependency organization. 

Getting Started
---------------
We are not yet on Maven Central, but installation is easy:
	
	git clone git@github.com:ruediste1/jabsaw.git
	cd jabsaw
	mvn install
	
JabSaw currently comes with a maven plugin. Simply add

	...
	<build>
		<plugins>
		...
			<plugin>
				<groupId>org.jabsaw</groupId>
				<artifactId>jabsaw-maven-plugin</artifactId>
				<version>1.0-SNAPSHOT</version>
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

to your pom.xml. Then define your module by creating an empty class named after your module, and annotating it 
with the @org.jabsaw.Module annotation. This annotation is used to define imported and exported modules. By
default, all classes in the package of the module class are included in the module. This can be customized using
inclusion and exclusion filters. Example:

	package com.test.impl;
	import com.test.api.ApiModule;
	import org.jabsaw.Module;
  
	@Module(exported = ApiModule.class)
	public class ImplModule{}
  
This creates the ImplModule, which depends on the ApiModule, and exports it to it's clients. During the build,
the plugin will check that the ImplModule will not depend on any class in another module than the ApiModule. For details see the JavaDoc of the Module annotation.
