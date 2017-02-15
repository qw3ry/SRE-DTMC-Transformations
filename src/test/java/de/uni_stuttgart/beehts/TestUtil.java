package de.uni_stuttgart.beehts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.uni_stuttgart.beehts.util.MathHelpers;

public class TestUtil {

	@Test
	public void testMath() {
		assertEquals(2, MathHelpers.gcd(6, 10));
		assertEquals(18, MathHelpers.lcm(6, 9));
	}
}
