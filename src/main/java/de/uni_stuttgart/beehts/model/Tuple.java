package de.uni_stuttgart.beehts.model;

/**
 * This class models any tuple with two elements.
 * 
 * @author Tobias Beeh
 *
 * @param <T1>
 *            The type of the first element.
 * @param <T2>
 *            The type of the second element.
 */
public class Tuple<T1, T2> {

	/**
	 * The content of the first element.
	 */
	public T1 x;
	/**
	 * The content of the second element.
	 */
	public T2 y;

	/**
	 * The constructor.
	 * 
	 * @param x
	 *            The first element of the new tuple.
	 * @param y
	 *            The second element of the new tuple.
	 */
	public Tuple(T1 x, T2 y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Tuple) {
			Tuple<?, ?> other = (Tuple<?, ?>) o;
			return (x == other.x || (x != null && x.equals(other.x)))
					&& (y == other.y || (y != null && y.equals(other.y)));
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (int) ((long) x.hashCode() + (long) y.hashCode());
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}
