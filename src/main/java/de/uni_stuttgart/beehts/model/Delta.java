package de.uni_stuttgart.beehts.model;

/**
 * This Interface provides some operations common to deltas of the models used.
 * 
 * @author Tobias Beeh
 *
 * @param <T>
 *            The underlying model.
 */
public interface Delta<T> {

	public T applyChanges(T model);

}
