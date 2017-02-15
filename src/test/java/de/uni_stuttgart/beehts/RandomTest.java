package de.uni_stuttgart.beehts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.Test;

import de.uni_stuttgart.beehts.generator.DTMCGenerator;
import de.uni_stuttgart.beehts.generator.SREGenerator;
import de.uni_stuttgart.beehts.model.DTMC;
import de.uni_stuttgart.beehts.model.Tuple;
import de.uni_stuttgart.beehts.transformation.Transformer;

public class RandomTest {

	@Test
	public void randomSREtoDTMCSmall() {
		System.out.println("SRE -> DTMC (small)");
		profile(100, 500, 10000, size -> SREGenerator.generateSRE(size), sre -> Transformer.getNewTransformer(sre),
				size -> size + 500);
	}

	@Test
	public void randomSREtoDTMCBig() {
		System.out.println("SRE -> DTMC (big)");
		profile(10, 2500, 50000, size -> SREGenerator.generateSRE(size), sre -> Transformer.getNewTransformer(sre),
				size -> size + 2500);
	}

	@Test
	public void abnormSREtoDTMC() {
		System.out.println("SRE -> DTMC (abnorm)");
		profile(10, 2, 50000, size -> SREGenerator.generateAbnormalSRE(size), sre -> Transformer.getNewTransformer(sre),
				size -> (int) (size * 1.5));
	}

	@Test
	public void randomSparseDMTCtoSRE() {
		System.out.println("DTMC -> SRE (sparse)");
		profile(10, 100, 1000, size -> DTMCGenerator.generateSparseDTMC(size),
				dtmc -> Transformer.getNewTransformer(dtmc),
				size -> size + 100);
	}

	@Test
	public void randomDenseDMTCtoSRE() {
		System.out.println("DTMC -> SRE (dense)");
		profile(10, 100, 1000, size -> DTMCGenerator.generateDenseDTMC(size),
				dtmc -> Transformer.getNewTransformer(dtmc),
				size -> size + 100);
	}

	@Test
	public void fromSREDMTCtoSRE() {
		System.out.println("DTMC -> SRE (obtained from transformation)");
		Map<Integer, DTMC> models = new HashMap<>();
		int minSize = 100;
		int maxSize = 1000;
		for (int i = minSize; i < maxSize; i += 10) {
			DTMC dtmc = Transformer.getNewTransformer(SREGenerator.generateSRE(i)).getTransformed();
			models.put(dtmc.getNodes().size(), dtmc);
		}
		int min = models.keySet().stream().mapToInt(i -> i).min().getAsInt();
		int max = models.keySet().stream().mapToInt(i -> i).max().getAsInt();
		List<Integer> keys = new ArrayList<>(models.keySet());
		keys.sort((i, j) -> i - j);
		profile(1, min, max, size -> models.get(size).clone(), dtmc -> Transformer.getNewTransformer(dtmc),
				size -> size == max ? size + 1 : keys.get(keys.indexOf(size) + 1));
	}

	private static <T, R> void profile(int repetitions, int startSize, int maxSize, Function<Integer, T> preparation,
			Function<T, R> measurement, Function<Integer, Integer> sizeIncrement) {
		Map<Integer, Set<Tuple<Integer, Double>>> time = ProfilingHelpers.profile(repetitions,
				startSize, maxSize, preparation,
				measurement, t -> -1, sizeIncrement);

		System.out.println("SIZE | TIME (ms)");
		for (int key : time.keySet()) {
			System.out.println(key + "; " + time.get(key).stream().mapToDouble(d -> d.y).average().getAsDouble());
		}
		System.out.println();
	}
}
