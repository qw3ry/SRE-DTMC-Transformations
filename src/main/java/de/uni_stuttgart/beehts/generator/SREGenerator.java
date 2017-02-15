package de.uni_stuttgart.beehts.generator;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import de.uni_stuttgart.beehts.model.SRE;
import de.uni_stuttgart.beehts.model.Tuple;
import de.uni_stuttgart.beehts.model.construction.SREBuilder;

public class SREGenerator {

	private static Random random = ThreadLocalRandom.current();

	public static SRE generateSRE(int approxSize) {
		if (approxSize < 3) {
			return SREBuilder.atomic(getRandomChar());
		} else {
			switch (random.nextInt(3)) {
			case 0: // kleene
				return SREBuilder.kleene(generateSRE(approxSize - 1), Math.random());
			case 1: {// concat
				int[] weights = distribute(approxSize - 1);
				SRE[] subs = new SRE[weights.length];
				for (int i = 0; i < weights.length; i++) {
					subs[i] = generateSRE(weights[i]);
				}
				return SREBuilder.concat(subs);
			}
			case 2: {// sum
				int[] weights = distribute(approxSize - 1);
				@SuppressWarnings("unchecked")
				Tuple<SRE, Integer>[] sres = new Tuple[weights.length];
				for (int i = 0; i < weights.length; i++) {
					sres[i] = new Tuple<>(generateSRE(weights[i]), random.nextInt(100) + 1);
				}
				return SREBuilder.sum(sres);
			}
			}
		}
		throw new Error();
	}

	public static SRE generateAbnormalSRE(int approxSize) {
		if (approxSize < 3) {
			return SREBuilder.atomic(getRandomChar());
		} else {
			switch (random.nextInt(3)) {
			case 0:
				return SREBuilder.kleene(generateAbnormalSRE(approxSize - 1), Math.random());
			case 1: {
				int size = distribute(approxSize - 1).length;
				int longSub = random.nextInt(size);
				SRE[] subs = new SRE[size];
				for (int i = 0; i < size; i++) {
					subs[i] = generateAbnormalSRE(i == longSub ? approxSize - size : 1);
				}
				return SREBuilder.concat(subs);
			}
			case 2: {// sum
				int size = distribute(approxSize - 1).length;
				int longSub = random.nextInt(size);
				@SuppressWarnings("unchecked")
				Tuple<SRE, Integer>[] sres = new Tuple[size];
				for (int i = 0; i < size; i++) {
					sres[i] = new Tuple<>(generateAbnormalSRE(i == longSub ? approxSize - size : 1),
							random.nextInt(100) + 1);
				}
				return SREBuilder.sum(sres);
			}
			}
		}
		throw new Error();
	}

	private static String getRandomChar() {
		return "" + ((char) (random.nextInt(26) + 'a'));
	}

	private static int[] distribute(int weight) {
		// choose the number of distributions
		int size;
		if (weight < 3 || random.nextInt(3) < 2) {
			size = random.nextInt(3) + 2;
		} else {
			size = random.nextInt(weight / 3) + 2;
		}
		if (size > weight) {
			size = weight;
		}

		// choose the weights
		int[] retVal = new int[size];
		for (int i = 0; i < size; i++) {
			int max = weight - size + i + 1;
			int min = (i + 1 == size) ? weight : 1;
			retVal[i] = random.nextInt(max - min + 1) + min;
			weight -= retVal[i];
		}

		shuffleArray(retVal);

		return retVal;
	}

	static void shuffleArray(int[] ar) {
		for (int i = ar.length - 1; i > 0; i--) {
			int index = random.nextInt(i + 1);
			int a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}
}
