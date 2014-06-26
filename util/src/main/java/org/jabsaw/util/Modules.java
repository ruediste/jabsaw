package org.jabsaw.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.jabsaw.impl.ClassParser;
import org.jabsaw.impl.model.*;
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

	private static class Config {

		public final boolean includeJars;
		public final boolean useModuleNames;
		public final String excludePath;

		public Config(Properties properties) {
			includeJars = parseBoolean(properties, "includeJars", true);
			useModuleNames = parseBoolean(properties, "useModuleNames", false);
			excludePath = properties.getProperty("excludePath");

		}

		private boolean parseBoolean(Properties properties,
				String propertyName, boolean defaultValue) throws Error {
			boolean value;
			String s = properties.getProperty(propertyName);
			if (s != null) {
				if ("true".equalsIgnoreCase(s)) {
					value = defaultValue;
				} else if ("false".equalsIgnoreCase(s)) {
					value = false;
				} else {
					throw new Error(propertyName + " must be true or false");
				}

			} else {
				value = defaultValue;
			}
			return value;
		}

	}

	/**
	 * Return the {@link ProjectModel} of the current classpath.
	 */
	public static ProjectModel getProjectModel() {
		synchronized (Modules.class) {
			if (Modules.projectModel == null) {
				ClassLoader classLoader = Modules.class.getClassLoader();
				Config config = new Config(new Properties());
				try (InputStream in = classLoader
						.getResourceAsStream("jabsaw.properties")) {
					Properties properties = new Properties();
					if (in != null) {
						properties.load(in);
					} else {
						Modules.logger.info("jabsaw.properties not found");
					}
					config = new Config(properties);
					Modules.logger.info("loaded jabsaw.properties");
				} catch (IOException e1) {
					Modules.logger.error("error loading jabsaw.properties");
				}

				try {
					// load all classes on the classpath
					ImmutableList<ClassInfo> classes = FluentIterable
							.from(ClassPath.from(classLoader).getResources())
							.filter(ClassInfo.class).toList();
					ClassParser parser = new ClassParser();
					parser.getProject()
					.setUseModuleNames(config.useModuleNames);
					for (ClassInfo info : classes) {
						if (!config.includeJars) {
							if ("jar".equals(info.url().getProtocol())) {
								continue;
							}
						}
						if (config.excludePath != null
								&& info.url().getPath()
								.contains(config.excludePath)) {
							continue;
						}
						Modules.logger.trace("parsing" + info.url());

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
	 * Get the {@link ModuleModel} defined by the given representing class
	 */
	public static ModuleModel getModuleModel(Class<?> module) {
		return Modules.getProjectModel().getModule(module.getName());
	}

	/**
	 * Return all classes the given module depends on, including transitive
	 * dependencies, even if they are not part of a module.
	 */
	public static Class<?>[] getAllRequiredClasses(Class<?>... modules) {
		HashSet<Class<?>> result = new HashSet<>();
		ClassLoader classLoader = Modules.class.getClassLoader();

		// follow dependencies by classes
		for (Class<?> module : modules) {
			for (ClassModel info : Modules.getModuleModel(module)
					.getAllClassDependencies()) {
				try {
					result.add(classLoader.loadClass(info.getQualifiedName()));
				} catch (ClassNotFoundException e) {
					Modules.logger
							.error("Error loading class "
									+ info.getQualifiedName(), e);
				}

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
		for (ClassModel info : Modules.getModuleModel(module)
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

	/**
	 * Check if all classes belong to a module. All errors are added to the
	 * provided error list.
	 */
	public static void checkAllClassesInModule(List<String> errors) {
		Modules.getProjectModel().checkAllClassesInModule(errors);
	}

	/**
	 * Check if all classes respect the accessibility boundaries defined by the
	 * modules. All errors are added to the provided error list.
	 */
	public static void checkClassAccessibility(List<String> errors) {
		Modules.getProjectModel().checkClassAccessibility(errors);
	}

	/**
	 * Check if there are any cycles in the dependencies of the modules. All
	 * errors are added to the provided error list.
	 */
	public static void checkDependencyCycles(List<String> errors) {
		Modules.getProjectModel().checkDependencyCycles(errors);
	}
}
