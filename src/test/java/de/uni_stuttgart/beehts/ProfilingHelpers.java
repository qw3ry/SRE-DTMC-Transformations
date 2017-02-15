package de.uni_stuttgart.beehts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.uni_stuttgart.beehts.model.Tuple;

public class ProfilingHelpers {

	private static final int progressSize = 100;

	private static void printProgress(double progress) {
		if (progress > 1)
			progress = 1;
		else if (progress < 0)
			progress = 0;
		int num = (int) (progress * progressSize);
		System.out.print("\r|");
		for (int i = 0; i < progressSize; i++) {
			System.out.print(i < num ? '=' : (i == num ? '>' : ' '));
		}
		System.out.print('|');
	}

	private static void clearProgressLine() {
		System.out.print('\r');
		for (int i = 0; i < progressSize + 2; i++) {
			System.out.print(' ');
		}
		System.out.print('\r');
	}

	public static <T, R, S> Map<Integer, Set<Tuple<S, Double>>> profile(int repetitions, int numOfModelsToTest,
			Function<T, R> measurement,
			Function<Integer, T> modelSupplier, Function<R, S> dataAboutResult) {
		Map<Integer, Set<Tuple<S, Double>>> time = new HashMap<>();
		for (int i = 0; i < repetitions; i++) {
			for (int j = 0; j < numOfModelsToTest; j++) {
				T t = modelSupplier.apply(j);

				System.gc();
				double start = System.nanoTime() / 1.e6;
				R r = measurement.apply(t);
				double duration = System.nanoTime() / 1.e6 - start;

				if (!time.containsKey(j)) {
					time.put(j, new HashSet<>());
				}
				time.get(j).add(new Tuple<>(dataAboutResult.apply(r), duration));

				ProfilingHelpers.printProgress((i + j / (double) numOfModelsToTest) / (double) repetitions);
			}
		}
		clearProgressLine();
		return time;
	}

	public static <T, R, S> Map<Integer, Set<Tuple<S, Double>>> profile(int repetitions, int startSize, int maxSize,
			Function<Integer, T> preparation, Function<T, R> measurement, Function<R, S> dataAboutResult,
			Function<Integer, Integer> sizeIncrement) {
		int numOfModelsToTest = 0;
		List<Integer> sizeMap = new ArrayList<>();
		for (int size = startSize; size <= maxSize; size = sizeIncrement.apply(size)) {
			numOfModelsToTest++;
			sizeMap.add(size);
		}

		Map<Integer, Set<Tuple<S, Double>>> result = profile(repetitions, numOfModelsToTest, measurement,
				i -> preparation.apply(sizeMap.get(i)), dataAboutResult);

		return result.keySet().stream().collect(Collectors.toMap(i -> sizeMap.get(i), i -> result.get(i)));
	}
}
