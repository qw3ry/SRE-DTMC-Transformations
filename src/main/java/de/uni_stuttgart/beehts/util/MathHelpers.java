package de.uni_stuttgart.beehts.util;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Class containing some static Math functions which are not provided by the
 * java standard library
 * 
 * @author Tobias Beeh
 */
public class MathHelpers {

	private static int gcd(int x, int y) {
		return (y == 0) ? x : gcd(y, x % y);
	}

	/**
	 * Calculate the greatest commom divisor of all numbers in the int stream
	 * 
	 * @param numbers
	 *            a int stream with some numbers
	 * @return the greatest common divisor of the numbers in the int stream
	 */
	public static int gcd(IntStream numbers) {
		return numbers.reduce(0, (x, y) -> gcd(x, y));
	}

	/**
	 * Calculate the greatest commom divisor of some integers
	 * 
	 * @param numbers
	 *            as many integers as you like
	 * @return the greatest common divisor of the given numbers
	 */
	public static int gcd(int... numbers) {
		return gcd(Arrays.stream(numbers));
	}

	/**
	 * Calculate the least common multiple of all numbers in the int stream
	 * 
	 * @param numbers
	 *            a int stream with some numbers
	 * @return the least common multiple of the numbers in the int stream
	 */
	public static int lcm(IntStream numbers) {
		return numbers.reduce(1, (x, y) -> x * (y / gcd(x, y)));
	}

	/**
	 * Calculate the least common multiple of some integer
	 * 
	 * @param numbers
	 *            as many integers as you like
	 * @return the least common multiple of the given numbers
	 */
	public static int lcm(int... numbers) {
		return lcm(Arrays.stream(numbers));
	}

	/**
	 * Check two doubles for equality. This is done with a relative epsilon
	 * check. Note that a == b && b == c does not imply a == c!
	 * 
	 * @param d1
	 *            the first double
	 * @param d2
	 *            the second double
	 * @return true if d1 is approx. equal to d2
	 */
	public static boolean equals(double d1, double d2) {
		return Math.abs(d1 - d2) < Math.ulp(d1) + Math.ulp(d2);
	}

	/**
	 * Check two floats for equality. This is done with a relative epsilon
	 * check. Note that a == b && b == c does not imply a == c!
	 * 
	 * @param d1
	 *            the first float
	 * @param d2
	 *            the second float
	 * @return true if d1 is approx. equal to d2
	 */
	public static boolean equals(float d1, float d2) {
		return Math.abs(d1 - d2) < Math.ulp(d1) + Math.ulp(d2);
	}

}
