package de.uni_stuttgart.beehts.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CollectionHelpers {

	public static <T> Set<T> setOf(@SuppressWarnings("unchecked") T... content) {
		return new HashSet<>(Arrays.asList(content));
	}
}
