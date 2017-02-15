package de.uni_stuttgart.beehts.model.construction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.uni_stuttgart.beehts.model.SRE;
import de.uni_stuttgart.beehts.model.Tuple;
import de.uni_stuttgart.beehts.model.SRE.*;

/**
 * This class helps constructing SREs.
 * 
 * @author Tobias Beeh
 */
public class SREBuilder {

	/**
	 * Constructs an atomic SRE.
	 * 
	 * @param c
	 *            The transition character.
	 * @return The constructed SRE.
	 */
	public static SRE atomic(String c) {
		return new SREAtomic(c);
	}

	/**
	 * Constructs a concatenation of multiple SREs. If the given SREs contain a
	 * concatenation, the recursive structure may be flattened by this method.
	 * There is no guarantee that the returned SRE is of type {@link SREConcat}.
	 * 
	 * @param sres
	 *            The SREs to concatenate.
	 * @return The constructed SRE.
	 */
	public static SRE concat(SRE... sres) {
		return new SREConcat(sres).simplify();
	}

	/**
	 * Constructs a choice of SREs (so called "sum"). If the given SREs contain
	 * a sum by themselves, the recursive structure may be flattened by this
	 * method.
	 * 
	 * @param sres
	 *            Tuples of SREs and their rates.
	 * @return The construced SRE.
	 */
	@SafeVarargs
	public static SRE sum(Tuple<SRE, Integer>... sres) {
		return new SRESum(sres).simplify();
	}

	/**
	 * Constructs an iteration over another SRE. If the given SRE is already an
	 * iteration, the recursive structure may be flattened by this method.
	 * 
	 * @param sre
	 *            The SRE to iterate over.
	 * @param rate
	 *            The probability of the iterations.
	 * @return The constructed SRE.
	 */
	public static SRE kleene(SRE sre, double rate) {
		return new SREKleene(sre, rate).simplify();
	}

	/**
	 * Constructs an SRE from the String representation of one. This uses the
	 * {@link SREParser}.
	 * 
	 * @param sre
	 *            A string representation of an SRE.
	 * @return An SRE.
	 */
	public static SRE parse(String sre) {
		return SREParser.parse(sre);
	}

	public static SRE fromFile(Path file) throws IOException {
		if (file.toFile().isDirectory())
			throw new IllegalArgumentException();
		return parse(String.join("\n", Files.readAllLines(file)));
	}
}
