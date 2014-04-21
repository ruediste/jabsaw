package org.jabsaw.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import org.jabsaw.impl.ClassParser;
import org.jabsaw.impl.model.ClassModel;
import org.jabsaw.impl.model.ProjectModel;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

/**
 * Static utility methods.
 */
public class Modules {
	private final static Logger logger = LoggerFactory.getLogger(Modules.class);

	private static ProjectModel projectModel;

	public static ProjectModel getProjectModel() {
		synchronized (Modules.class) {
			if (Modules.projectModel == null) {
				try {
					// load all classes on the classpath
					ClassLoader classLoader = Modules.class.getClassLoader();
					ImmutableList<ClassInfo> classes = FluentIterable
							.from(ClassPath.from(classLoader).getResources())
							.filter(ClassInfo.class).toList();
					ClassParser parser = new ClassParser();
					for (ClassInfo info : classes) {
						try (InputStream is = classLoader
								.getResourceAsStream(info.getResourceName())) {
							ClassReader reader = new ClassReader(is);
							parser.parse(reader);
						} catch (Throwable t) {
							Modules.logger.error(
									"Error while reading " + info.getName(), t);
						}
					}
					parser.getProject().resolveDependencies();
					Modules.projectModel = parser.getProject();
				} catch (IOException e) {
					throw new RuntimeException(
							"error while reading classes on classpath", e);
				}
			}
		}
		return Modules.projectModel;
	}

	/**
	 * Return the names of all classes the given module depends on, including
	 * transitive dependencies, even if they are not part of a module.
	 */
	public static Class<?>[] getAllRequiredClasses(Class<?> module) {
		HashSet<Class<?>> result = new HashSet<>();
		ClassLoader classLoader = Modules.class.getClassLoader();
		for (ClassModel info : Modules.getProjectModel()
				.getModule(module.getName()).getAllClassDependencies()) {
			try {
				result.add(classLoader.loadClass(info.getQualifiedName()));
			} catch (ClassNotFoundException e) {
				Modules.logger.error(
						"Error loading class " + info.getQualifiedName(), e);
			}
		}
		return result.toArray(new Class<?>[] {});
	}

	/**
	 * Return the names of all classes in the given module.
	 */
	public static Class<?>[] getClasses(Class<?> module) {
		HashSet<Class<?>> result = new HashSet<>();
		ClassLoader classLoader = Modules.class.getClassLoader();
		ProjectModel tmp = Modules.getProjectModel();
		for (ClassModel info : tmp.getModule(module.getName())
				.getAllClassDependencies()) {
			try {
				result.add(classLoader.loadClass(info.getQualifiedName()));
			} catch (ClassNotFoundException e) {
				Modules.logger.error(
						"Error loading class " + info.getQualifiedName(), e);
			}
		}
		return result.toArray(new Class<?>[] {});
	}
}
