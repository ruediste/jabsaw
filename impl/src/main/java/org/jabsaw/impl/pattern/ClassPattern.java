package org.jabsaw.impl.pattern;

import java.util.Set;
import java.util.regex.Pattern;

public class ClassPattern {

	private final String originalPattern;
	private final long score;
	private final Pattern pattern;

	public ClassPattern(String pkg, String pattern) {
		originalPattern = pattern;
		if (pattern.startsWith(".")) {
			if (pkg.length() == 0) {
				pattern = pattern.substring(1);
			} else {
				pattern = pkg + pattern;
			}
		}
		score = calculateScore(pattern);
		this.pattern = createRegexpPattern(pattern);
	}

	private Pattern createRegexpPattern(String pattern) {
		// remove trailing exclamation mark
		if (pattern.endsWith("!")) {
			pattern = pattern.substring(0, pattern.length() - 1);
		}

		StringBuilder full = new StringBuilder(pattern.length());
		StringBuilder part = new StringBuilder(10);
		for (int i = 0; i < pattern.length(); i++) {
			char ch = pattern.charAt(i);
			if (ch == '*') {
				// append part
				if (part.length() > 0) {
					full.append(Pattern.quote(part.toString()));
				}
				part = new StringBuilder(10);

				// add regex expression for * or **
				if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') {
					full.append(".*");
				} else {
					full.append("[^\\.]*");
				}
			} else {
				// no *, just append the char
				part.append(ch);
			}
		}

		// append the last part
		if (part.length() > 0) {
			full.append(Pattern.quote(part.toString()));
		}

		// create the pattern
		return Pattern.compile(full.toString());
	}

	private long calculateScore(String pattern) {
		// remove trailing exclamation mark
		boolean isImportant = false;
		if (pattern.endsWith("!")) {
			pattern = pattern.substring(0, pattern.length() - 1);
			isImportant = true;
		}

		String[] parts = pattern.split("\\.");
		long result = 0;
		long factor = 2;
		for (String part : parts) {
			int baseScore;
			if ("**".equals(part)) {
				baseScore = 4;
				factor = 1;
			} else if ("*".equals(part)) {
				baseScore = 8;
				factor = 1;
			} else if (part.contains("**")) {
				baseScore = 16;
				factor = 1;
			} else if (part.contains("*")) {
				baseScore = 32;
				factor = 1;
			} else {
				baseScore = 64;
			}

			result += baseScore * factor;
		}
		if (isImportant) {
			result *= 1000;
		}
		return result;
	}

	public String getOriginalPattern() {
		return originalPattern;
	}

	public long getScore() {
		return score;
	}

	public boolean matches(String qualifiedClassName) {
		return pattern.matcher(qualifiedClassName).matches();
	}

	public static ClassPattern getBestMatch(Set<ClassPattern> patterns,
			String qualifiedName) {
		ClassPattern result = null;
		for (ClassPattern p : patterns) {
			if (p.matches(qualifiedName)) {
				if (result == null || result.score < p.score) {
					result = p;
				}
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return originalPattern;
	}
}
