package de.uni_stuttgart.beehts.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import de.uni_stuttgart.beehts.model.SRE.SREAtomic;
import de.uni_stuttgart.beehts.model.SRE.SREKleene;
import de.uni_stuttgart.beehts.model.SRE.Traverser;
import de.uni_stuttgart.beehts.model.SRE.Type;
import de.uni_stuttgart.beehts.model.construction.SREBuilder;
import de.uni_stuttgart.beehts.util.StringHelpers;

public class SREDelta implements Delta<SRE> {

	private Map<SRE, SRE> replacements = new HashMap<>();

	public static Delta<SRE> parse(SRE sre, String s) {
		if (!isValid(s)) {
			throw new IllegalArgumentException();
		}

		SREDelta delta = new SREDelta();

		for (String line : s.split("\\R")) {
			String[] parts = line.split("\\s*>\\s*");

			SRE toReplace = getByIndex(sre, Integer.parseInt(parts[0]));
			SRE replacement = SREBuilder.parse(parts[1]);

			replacement = replacement.traverse(new Traverser() {

				@Override
				protected SRE postOrder(SRE traverseSRE, OptionalInt weight) {
					if (traverseSRE.getType() == Type.ATOMIC) {
						SREAtomic s = (SREAtomic) traverseSRE;
						if (s.getCharacter().trim().startsWith("\\")) {
							int i = Integer.parseInt(s.getCharacter().trim().substring(1));
							return getByIndex(sre, i);
						}
					}
					return traverseSRE;
				}
			}).y;

			delta.addChange(toReplace, replacement);
		}

		return delta;
	}

	private static boolean isValid(String s) {
		String sre = "[\\s\\w\\d\\\\\\:\\*\\[\\]\\.\\+\\(\\)]+";
		String line = "\\d\\s*>" + sre;
		return s.matches("(" + line + "\\R)*" + line);
	}

	public static SRE getByIndex(SRE sre, int index) {
		return sre.traverse(new Traverser() {

			SRE sre = null;
			int currIndex = 1;

			@Override
			protected void preOrder(SRE sre) {
				if (currIndex++ == index)
					this.sre = sre;
			}
		}).x.sre;
	}

	public static String printIndices(SRE sre) {
		return sre.traverse(new Traverser() {

			public StringBuilder sre = new StringBuilder(), indices = new StringBuilder();
			private int currentIndex = 1;

			@Override
			public String toString() {
				return sre.toString() + System.getProperty("line.separator") + indices.toString();
			}

			@Override
			protected void preOrder(SRE sre) {
				StringHelpers.fillToEqualLength(' ', this.sre, this.indices);
				this.indices.append(this.currentIndex++ + " ");
				switch (sre.getType()) {
				case ATOMIC:
					this.sre.append(sre.toString());
					break;
				case CAT: // fallthrough
				case KLEENE: // fallthrough
				case SUM:
					this.sre.append('(');
					break;
				default:
					throw new IllegalArgumentException();
				}
			}

			@Override
			protected void inOrder(SRE sre) {
				switch (sre.getType()) {
				case ATOMIC:
					break;
				case CAT:
					this.sre.append(" : ");
					break;
				case SUM:
					this.sre.append(" + ");
					break;
				case KLEENE: // fallthrough
				default:
					throw new IllegalArgumentException();
				}
			}

			@Override
			protected SRE postOrder(SRE sre, OptionalInt weight) {
				switch (sre.getType()) {
				case ATOMIC:
					break;
				case KLEENE:
					this.sre.append("*" + ((SREKleene) sre).getRepetitionRate());
					// fallthrough
				case CAT: // fallthrough
				case SUM:
					this.sre.append(')');
					break;
				default:
					throw new IllegalArgumentException();
				}
				if (weight.isPresent()) {
					this.sre.append("[" + weight.getAsInt() + "]");
				}
				return sre;
			}
		}).x.toString();
	}

	public void addChange(SRE original, SRE replacement) {
		replacements.put(original, replacement);
	}

	public Map<SRE, SRE> getChanges() {
		return Collections.unmodifiableMap(replacements);
	}

	@Override
	public SRE applyChanges(SRE sre) {
		return sre.traverse(new SRE.Traverser() {

			@Override
			protected SRE postOrder(SRE sre, OptionalInt weight) {
				if (replacements.containsKey(sre)) {
					return replacements.get(sre);
				} else {
					return sre;
				}
			}
		}).y;
	}
}
