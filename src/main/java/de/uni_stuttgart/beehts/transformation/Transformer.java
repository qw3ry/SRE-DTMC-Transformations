package de.uni_stuttgart.beehts.transformation;

import de.uni_stuttgart.beehts.model.DTMC;
import de.uni_stuttgart.beehts.model.Delta;
import de.uni_stuttgart.beehts.model.SRE;

/**
 * This interface provides methods for transforming DTMCs and SREs one into
 * another.
 * 
 * Furthermore it generalizes the transformer classes.
 * 
 * @author Tobias Beeh
 */
public interface Transformer<From, To> {

	/**
	 * Transform any SRE into a DTMC.
	 * 
	 * @param sre
	 *            The SRE to transform.
	 * @return The resulting DTMC.
	 */
	public static DTMC toDTMC(SRE sre) {
		Transformer<SRE, DTMC> transformer = getNewTransformer(sre);
		return transformer.getTransformed();
	}

	/**
	 * Transform any DTMC into an SRE.
	 * 
	 * @param dtmc
	 *            The DTMC to transform.
	 * @return The resulting SRE.
	 */
	public static SRE toSRE(DTMC dtmc) {
		Transformer<DTMC, SRE> transformer = getNewTransformer(dtmc);
		return transformer.getTransformed();
	}

	public static Transformer<SRE, DTMC> getNewTransformer(SRE sre) {
		return new SRE2DTMCDelta(sre);
	}

	public static Transformer<DTMC, SRE> getNewTransformer(DTMC dtmc) {
		return new DTMC2SREDeltaBrz(dtmc);
	}

	/**
	 * Fully transform the model (no deltas). To be done in the constructor!
	 */
	public void transform();

	/**
	 * Get the model that is to be transformed.
	 * 
	 * @return The model to transform.
	 */
	public From getOriginal();

	/**
	 * Get the result of the transformation.
	 * 
	 * @return The result of the transformation.
	 */
	public To getTransformed();

	/**
	 * Apply a delta to the original model and use it to transform again.
	 * 
	 * @param delta
	 *            the delta to apply
	 * @return the delta that has been applied to the transformed model.
	 */
	public Delta<To> applyDelta(Delta<From> delta);

}
