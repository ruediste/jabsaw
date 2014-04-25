package org.jabsaw.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;

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

	private static class Config {

		public final boolean includeJars;
		public final String excludePath;

		public Config(Properties properties) {
			String s = properties.getProperty("includeJars");
			if (s != null) {
				if ("true".equalsIgnoreCase(s)) {
					includeJars = true;
				} else if ("false".equalsIgnoreCase(s)) {
					includeJars = false;
				} else {
					throw new Error("includeJars must be true or false");
				}

			} else {
				includeJars = true;
			}

			excludePath = properties.getProperty("excludePath");

		}

	}

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
	 * Return all classes the given module depends on, including transitive
	 * dependencies, even if they are not part of a module.
	 */
	public static Class<?>[] getAllRequiredClasses(Class<?> module) {
		HashSet<Class<?>> result = new HashSet<>();
		ClassLoader classLoader = Modules.class.getClassLoader();

		// follow dependencies by classes
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
