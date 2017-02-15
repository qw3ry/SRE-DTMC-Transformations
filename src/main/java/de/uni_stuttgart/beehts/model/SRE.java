package de.uni_stuttgart.beehts.model;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.uni_stuttgart.beehts.util.MathHelpers;

/**
 * This class models Stochastic Regular Expressions (SREs). This is a recursive
 * model.
 * 
 * @author Tobias Beeh
 */
public abstract class SRE {

	/**
	 * This enum represents the possibly operations you can do with SREs. As
	 * this model is recursive, it consists of other SREs, that are put together
	 * by an operation. Hence you can speak of a top-level type of an SRE.
	 * 
	 * @author Tobias Beeh
	 */
	public enum Type {
		SUM, CAT, KLEENE, ATOMIC,
	}

	/**
	 * Calculate the probability for a given String.
	 * 
	 * @param string
	 *            The string to calculate the probability for.
	 * @return The probability
	 * @throws UnsupportedOperationException
	 *             in the current implementation state, this will always be
	 *             thrown.
	 */
	public abstract double getProbability(String string);

	/**
	 * Get the {@link Type} of the SRE.
	 * 
	 * @return the Type of this SRE.
	 */
	public abstract Type getType();

	public abstract SRE clone();

	/**
	 * Simplify the SRE by removing unneeded nesting.
	 * 
	 * To do this, the SRE is checked against some common patterns like
	 * <code>a[x] + (b[y] + c[z])</code> which can be simplified to a sum
	 * without nesting (but different weights)
	 * 
	 * @return the simplified SRE
	 */
	public abstract SRE simplify();

	/**
	 * Check for equality of the SRE (instead of object equality)
	 * 
	 * @param other
	 *            the "other" sre
	 * @return true if this and other are equivalent regarding their syntax.
	 */
	public abstract boolean deepEquals(SRE other);

	/**
	 * Traverse the tree calling the methods of a traverser for each node.
	 * 
	 * @param <T>
	 * 
	 * @param t
	 *            the traverser to use
	 */
	public final <T extends Traverser> Tuple<T, SRE> traverse(T t) {
		return traverse(t, OptionalInt.empty());
	}

	protected abstract <T extends Traverser> Tuple<T, SRE> traverse(T t, OptionalInt weight);

	/**
	 * Abstract class to visit each node of a SRE
	 * 
	 * @author Tobias Beeh
	 */
	public static abstract class Traverser {

		/**
		 * Called before traversing the child node(s). Called once for each leaf
		 * node.
		 * 
		 * @param sre
		 *            the current node
		 */
		protected void preOrder(SRE sre) {
		}

		/**
		 * Called once for a leaf node. For other nodes called between the
		 * traversal of child nodes. May be called 0 to n times for a certain
		 * node.
		 * 
		 * @param sre
		 *            the current node
		 */
		protected void inOrder(SRE sre) {
		}

		/**
		 * Called after all child node(s) are traversed. Called once for each
		 * leaf node.
		 * 
		 * @param sre
		 *            the current node
		 * @param weight
		 *            the weight of this node if the parent is a sum. Otherwise
		 *            no value is present.
		 * @return an updated value for the node (if you don't want to update,
		 *         return the sre itself)
		 */
		protected SRE postOrder(SRE sre, OptionalInt weight) {
			return sre;
		}
	}

	/**
	 * This class models a SRE, which is atomic.
	 * 
	 * @author Tobias Beeh
	 */
	public static class SREAtomic extends SRE {

		private String c;

		/**
		 * Constructor.
		 *
		 * @param c
		 *            The character to recognize. An empty string recognizes the
		 *            empty String. Must not be null.
		 */
		public SREAtomic(String c) {
			if (c == null) {
				throw new IllegalArgumentException();
			}
			this.c = c;
		}

		/**
		 * Factory method for an epsilon SRE
		 * 
		 * @return a newly constructed epsilon SRE
		 */
		public static SREAtomic EPSILON() {
			return new SREAtomic("");
		}

		/**
		 * get the transition character.
		 * 
		 * @return the transition character.
		 */
		public String getCharacter() {
			return c;
		}

		@Override
		public String toString() {
			return getCharacter();
		}

		@Override
		public boolean deepEquals(SRE other) {
			if (other == null) {
				return false;
			} else if (this.getClass().isAssignableFrom(other.getClass())) {
				String oc = ((SREAtomic) other).getCharacter();
				return Objects.equals(oc, this.getCharacter());
			} else {
				return false;
			}
		}

		@Override
		public Type getType() {
			return Type.ATOMIC;
		}

		@Override
		public SRE simplify() {
			// an atomic SRE cannot be further simplified
			return this;
		}

		@Override
		public <T extends Traverser> Tuple<T, SRE> traverse(T t, OptionalInt weight) {
			t.preOrder(this);
			t.inOrder(this);
			return new Tuple<T, SRE>(t, t.postOrder(this, weight));
		}

		@Override
		public SRE clone() {
			return new SREAtomic(getCharacter());
		}

		@Override
		public double getProbability(String string) {
			return getCharacter().equals(string) ? 1 : 0;
		}
	}

	/**
	 * This class models a SRE, which consists of a concatenation of several
	 * other SREs.
	 * 
	 * @author Tobias Beeh
	 */
	public static class SREConcat extends SRE {

		private SRE[] subnodes;

		/**
		 * Constructor
		 * 
		 * @param subnode
		 *            The SREs to concatenate. Need to be at least 2.
		 */
		public SREConcat(SRE... subnode) {
			if (subnode.length < 1)
				throw new IllegalArgumentException();
			subnodes = subnode;
		}

		/**
		 * Get all the concatenated SREs.
		 * 
		 * @return a list of SREs.
		 */
		public SRE[] getSubnodes() {
			return subnodes;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(subnodes[0].toString());
			for (int i = 1; i < subnodes.length; i++) {
				builder.append(" : ");
				builder.append(subnodes[i].toString());
			}
			return "(" + builder.toString() + ")";
		}

		@Override
		public boolean deepEquals(SRE o) {
			if (o == null) {
				return false;
			} else if (this.getClass().isAssignableFrom(o.getClass())) {
				SREConcat other = (SREConcat) o;
				return Arrays.equals(other.getSubnodes(), this.getSubnodes());
			} else {
				return false;
			}
		}

		@Override
		public Type getType() {
			return Type.CAT;
		}

		/**
		 * Implementation of {@link SRE#simplify()}.<br>
		 * 
		 * Patterns, that are simplified:<br>
		 * 
		 * - a : (b : c) ==> a : b : c <br>
		 * - a : epsilon : b ==> a : b <br>
		 */
		@Override
		public SRE simplify() {
			List<SRE> sreList = new LinkedList<>();
			for (SRE sre : this.subnodes) {
				if (sre.getType() == SRE.Type.ATOMIC) {
					// filter epsilon expressions
					String character = ((SREAtomic) sre).getCharacter();
					if (character.isEmpty()) {
						continue;
					}
				} else if (sre.getType() == SRE.Type.CAT) {
					// remove unnecessary deep nesting
					for (SRE subnode : ((SREConcat) sre).getSubnodes()) {
						sreList.add(subnode);
					}
					continue;
				}
				sreList.add(sre);
			}
			if (sreList.isEmpty()) {
				return SREAtomic.EPSILON();
			} else if (sreList.size() == 1) {
				return sreList.get(0);
			} else {
				return new SREConcat(sreList.toArray(new SRE[sreList.size()]));
			}
		}

		@Override
		public <T extends Traverser> Tuple<T, SRE> traverse(T t, OptionalInt weight) {
			t.preOrder(this);
			for (int i = 0; i < getSubnodes().length; i++) {
				getSubnodes()[i] = getSubnodes()[i].traverse(t).y;
				if (i != getSubnodes().length - 1) {
					t.inOrder(this);
				}
			}
			return new Tuple<T, SRE>(t, t.postOrder(this, weight));
		}

		@Override
		public SRE clone() {
			SRE[] sres = new SRE[subnodes.length];
			for (int i = 0; i < subnodes.length; i++) {
				sres[i] = subnodes[i].clone();
			}
			return new SREConcat(sres);
		}

		@Override
		public double getProbability(String string) {
			int[] sepIdx = new int[subnodes.length];
			for (int i = 0; i < sepIdx.length; i++) {
				sepIdx[i] = 0;
			}
			// for all possible separator combinations
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * This class models a SRE, which consists of a sum of several other SREs
	 * (that is, a choice).
	 * 
	 * @author Tobias Beeh
	 */
	public static class SRESum extends SRE {

		private SRE[] subnodes;
		private int[] rates;

		/**
		 * Constructor
		 * 
		 * @param sres
		 *            The sub SREs, each coupled with the rate that it gets
		 *            chosen. Note that each rate needs to be nonnegative.
		 */
		@SafeVarargs
		public SRESum(Tuple<SRE, Integer>... sres) {
			/*
			 * if (sres.length < 2) { throw new IllegalArgumentException(); }
			 */
			subnodes = new SRE[sres.length];
			rates = new int[sres.length];
			for (int i = 0; i < sres.length; i++) {
				if (sres[i].y < 0)
					throw new IllegalArgumentException();
				subnodes[i] = sres[i].x;
				rates[i] = sres[i].y;
			}
		}

		/**
		 * For internal use by {@link #simplify()} only.
		 */
		private SRESum(List<SRE> sres, List<Integer> rates) {
			assert (sres.size() > 1 && sres.size() == rates.size());
			this.subnodes = new SRE[sres.size()];
			this.rates = new int[sres.size()];
			for (int i = 0; i < sres.size(); i++) {
				if (rates.get(i) < 0)
					throw new IllegalArgumentException();
				subnodes[i] = sres.get(i);
				this.rates[i] = rates.get(i);
			}
		}

		/**
		 * For internal use only.
		 */
		private SRESum() {
		}

		/**
		 * Get all possible choices.
		 * 
		 * @return An array of SREs.
		 */
		public SRE[] getSubnodes() {
			return subnodes;
		}

		/**
		 * Get the rates for the choices. The indices of the SREs from
		 * {@link #getSubnodes()} will match the indices of the returned array.
		 * 
		 * @return An array of nonnegative integers.
		 */
		public int[] getRates() {
			return rates;
		}

		@Override
		public String toString() {
			return '(' + IntStream.range(0, subnodes.length)
					.mapToObj(i -> subnodes[i].toString() + '[' + rates[i] + ']').collect(Collectors.joining(" + "))
					+ ')';
		}

		@Override
		public boolean deepEquals(SRE o) {
			if (o == null) {
				return false;
			} else if (this.getClass().isAssignableFrom(o.getClass())) {
				SRESum other = (SRESum) o;
				return Arrays.equals(other.getSubnodes(), this.getSubnodes())
						&& Arrays.equals(other.getRates(), this.getRates());
			} else {
				return false;
			}
		}

		@Override
		public Type getType() {
			return Type.SUM;
		}

		/**
		 * Simplifies <code>a + a + b</code> to <code> a + b</code>.<br>
		 * This used to simplify <code>a + (b + c)</code> to
		 * <code>a + b + c</code> as well, but this was removed due to integer
		 * overflow issues.<br>
		 * Be aware of the bad runtime of this function (at least O(n²)!).
		 */
		@Override
		public SRE simplify() {
			List<SRE> sreList = new LinkedList<>();
			List<Integer> rateList = new LinkedList<>();

			for (int i = 0; i < subnodes.length; i++) {
				sreList.add(subnodes[i]);
				rateList.add(rates[i]);
			}

			for (int i = 0; i < sreList.size(); i++) {
				int j = i + 1;
				while (j < sreList.size()) {
					if (sreList.get(i).deepEquals(sreList.get(j))) {
						sreList.remove(j);
						rateList.set(i, rateList.get(i) + rateList.remove(j));
					} else {
						j++;
					}
				}
			}

			if (sreList.size() < 2) {
				if (sreList.isEmpty()) {
					throw new AssertionError("This should never happen. Ever.");
				} else {
					return sreList.get(0);
				}
			}

			return new SRESum(sreList, rateList);
		}

		@Override
		public <T extends Traverser> Tuple<T, SRE> traverse(T t, OptionalInt weight) {
			t.preOrder(this);
			for (int i = 0; i < getSubnodes().length; i++) {
				getSubnodes()[i] = getSubnodes()[i].traverse(t, OptionalInt.of(getRates()[i])).y;
				if (i != getSubnodes().length - 1) {
					t.inOrder(this);
				}
			}
			return new Tuple<T, SRE>(t, t.postOrder(this, weight));
		}

		@Override
		public SRE clone() {
			SRE[] sres = new SRE[subnodes.length];
			int[] rates = new int[subnodes.length];
			for (int i = 0; i < subnodes.length; i++) {
				sres[i] = this.subnodes[i].clone();
				rates[i] = this.rates[i];
			}
			SRESum retVal = new SRESum();
			retVal.rates = rates;
			retVal.subnodes = sres;
			return retVal;
		}

		@Override
		public double getProbability(String string) {
			int rateSum = Arrays.stream(rates).sum();
			double p = 0;
			for (int i = 0; i < getSubnodes().length; i++) {
				p += getSubnodes()[i].getProbability(string) * getRates()[i] / rateSum;
			}
			return p;
		}
	}

	/**
	 * This class models a SRE, which consists of a Kleene-iteration over
	 * another SRE.
	 * 
	 * @author Tobias Beeh
	 */
	public static class SREKleene extends SRE {

		private SRE sre;
		private double repetitionRate;

		/**
		 * Constructor.
		 * 
		 * @param sre
		 *            The SRE to iterate
		 * @param rate
		 *            the probability of iteration.
		 */
		public SREKleene(SRE sre, double rate) {
			if (sre == null) {
				throw new IllegalArgumentException();
			}
			this.sre = sre;
			this.repetitionRate = rate;
		}

		/**
		 * Get the iterated SRE.
		 * 
		 * @return An SRE.
		 */
		public SRE getChild() {
			return sre;
		}

		/**
		 * Get the probability of another repetition.
		 * 
		 * @return The repetition rate.
		 */
		public double getRepetitionRate() {
			return repetitionRate;
		}

		@Override
		public String toString() {
			return "(" + sre.toString() + "*" + repetitionRate + ")";
		}

		@Override
		public boolean deepEquals(SRE other) {
			if (other == null) {
				return false;
			} else if (this.getClass().isAssignableFrom(other.getClass())) {
				SREKleene otherConverted = (SREKleene) other;
				return otherConverted.getChild().deepEquals(this.getChild())
						&& MathHelpers.equals(otherConverted.getRepetitionRate(), this.getRepetitionRate());
			} else {
				return false;
			}
		}

		@Override
		public Type getType() {
			return Type.KLEENE;
		}

		/**
		 * Implementation of {@link SRE#simplify()}.<br>
		 * Simplifies <code>(a*)*<code> to <code>a*</code>.
		 */
		@Override
		public SRE simplify() {
			if (this.repetitionRate == 0.0) {
				return SREAtomic.EPSILON();
			} else if (this.sre.getType() == Type.KLEENE) {
				// in a nested kleene (e.g. {a^*p1}^*p2), the probability for
				// the empty string can be calculated by
				// <code>sum_{i=1}^inf{(1-p2)*(p1*p2)^i}</code>
				// which is equal to
				// <code>frac{1-p2}{1-p1*p2}</code>.
				// Therefore if a^p is equal to {a^*p1}^*p2 then
				// p = 1 - frac{1-p2}{1-p1*p2}
				SREKleene sub = (SREKleene) this.sre;
				double p = 1 - (1 - this.repetitionRate) / (1 - this.repetitionRate * sub.getRepetitionRate());
				return new SREKleene(sub.getChild(), p);
			} else if (this.sre.getType() == Type.ATOMIC) {
				String c = ((SREAtomic) this.sre).c;
				if (c == null || c.isEmpty()) {
					return SREAtomic.EPSILON();
				}
			}
			return new SREKleene(this.sre, this.repetitionRate);
		}

		@Override
		public <T extends Traverser> Tuple<T, SRE> traverse(T t, OptionalInt weight) {
			t.preOrder(this);
			this.sre = getChild().traverse(t).y;
			return new Tuple<T, SRE>(t, t.postOrder(this, weight));
		}

		@Override
		public SRE clone() {
			return new SREKleene(getChild().clone(), repetitionRate);
		}

		@Override
		public double getProbability(String string) {
			throw new UnsupportedOperationException();
		}
	}
}
