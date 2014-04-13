package laf.module.pattern;

import static org.junit.Assert.*;

import org.junit.Test;

public class ClassPatternTest {

	@Test
	public void test() {
		ClassPattern pattern = new ClassPattern("", "foo.bar.Test");
		assertEquals(384, pattern.getScore());
		assertTrue(pattern.matches("foo.bar.Test"));
		assertFalse(pattern.matches("foo.bar.Tes"));

		pattern = new ClassPattern("", "foo.bar.*");
		assertEquals(264, pattern.getScore());
		assertTrue(pattern.matches("foo.bar.Test"));
		assertTrue(pattern.matches("foo.bar.Tes"));

		pattern = new ClassPattern("", "foo.bar.*!");
		assertEquals(264 * 1000, pattern.getScore());
		assertTrue(pattern.matches("foo.bar.Test"));
		assertTrue(pattern.matches("foo.bar.Tes"));

		pattern = new ClassPattern("", "foo.**Test");
		assertEquals(144, pattern.getScore());
		assertTrue(pattern.matches("foo.bar.Test"));
		assertTrue(pattern.matches("foo.HelloTest"));
		assertTrue(pattern.matches("foo.bar.HelloTest"));
		assertFalse(pattern.matches("foo.bar.HelloTes"));
		assertFalse(pattern.matches("foa.bar.HelloTest"));

	}
}
