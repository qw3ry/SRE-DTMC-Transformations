package de.uni_stuttgart.beehts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.uni_stuttgart.beehts.generator.DTMCGenerator;
import de.uni_stuttgart.beehts.generator.SREGenerator;
import de.uni_stuttgart.beehts.model.*;
import de.uni_stuttgart.beehts.model.SRE.*;
import de.uni_stuttgart.beehts.model.construction.*;
import de.uni_stuttgart.beehts.transformation.Transformer;

public class Evaluation {

	public static void main(String[] args) {
		System.out.println("RANDOM MODELS");
		testRandomModels();
		System.out.println("CASESTUDIES");
		testCaseStudyModels();
	}

	private static void testCaseStudyModels() {
		String[] files = { "CaseStudy.grammar" };
		System.out.println("  SRE -> DTMC");
		for (String file : files) {
			try {
				System.out.println("    " + file);
				String model;
				model = String.join("\n",
						Files.readAllLines(FileSystems.getDefault().getPath("res", file).toAbsolutePath()));

				Map<Integer, Set<Tuple<Tuple<Integer, Integer>, Double>>> result = ProfilingHelpers.profile(10, 1,
						t -> Transformer.getNewTransformer(t).getTransformed(),
						i -> {
							SRE s = SREBuilder.parse(model);
							System.out.println(calculateSize(s));
							return s;
						}, d -> {
							try {
								Files.write(FileSystems.getDefault().getPath("res", file + ".transformed"),
										d.toString().getBytes());
							} catch (IOException e) {
							}
							return new Tuple<>(d.getNodes().size(), d.getEdges().size());
						});

				writeToCSV(result, FileSystems.getDefault().getPath("res", file + ".measurement"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		files = new String[] { "leader4_4.tra", "leader4_6.tra", "leader4_8.tra", "leader5_6.tra", "leader6_6.tra",
				"zeroconf.tra", };
		System.out.println("  DTMC -> SRE");
		for (String file : files) {
			try {
				System.out.println("    " + file);
				String model = String.join("\n",
						Files.readAllLines(FileSystems.getDefault().getPath("res", file).toAbsolutePath()));

				Map<Integer, Set<Tuple<Tuple<BigInteger, BigInteger>, Double>>> result = ProfilingHelpers.profile(10, 1,
						t -> Transformer.getNewTransformer(t).getTransformed(),
						i -> DTMCParser.parse(model), s -> {
							Tuple<BigInteger, BigInteger> size = calculateSize(s);
							try {
								if (size.x.intValueExact() < 5000) {
									Files.write(FileSystems.getDefault().getPath("res", file + ".transformed"),
											s.toString().getBytes());
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							return size;
						});

				writeToCSV(result, FileSystems.getDefault().getPath("res", file + ".measurement"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void testRandomModels() {
		System.out.println("  DTMC -> SRE");
		try {
			Map<Integer, Set<Tuple<Tuple<BigInteger, BigInteger>, Double>>> result;
			Map<Integer, Integer> sizeMap = new HashMap<>();
			System.out.println("    Sparse");
			result = ProfilingHelpers.profile(100, 100, 1000,
					size -> {
						DTMC d = DTMCGenerator.generateSparseDTMC(size);
						sizeMap.put(size, d.getEdges().size());
						return d;
					}, dtmc -> Transformer.getNewTransformer(dtmc).getTransformed(), s -> calculateSize(s),
					size -> size + 100);
			writeToCSV(result, FileSystems.getDefault().getPath("res", "d2sSparse.measurement"));
			writeToCSV(mapSizes(result, sizeMap),
					FileSystems.getDefault().getPath("res", "d2sSparseByEdges.measurement"));
			result.clear();
			sizeMap.clear();

			System.out.println("    Dense");
			result = ProfilingHelpers.profile(10, 50, 200,
					size -> {
						DTMC d = DTMCGenerator.generateDenseDTMC(size);
						sizeMap.put(size, d.getEdges().size());
						return d;
					}, dtmc -> Transformer.getNewTransformer(dtmc).getTransformed(), s -> calculateSize(s),
					size -> size + 10);
			writeToCSV(result, FileSystems.getDefault().getPath("res", "d2sDense.measurement"));
			writeToCSV(mapSizes(result, sizeMap),
					FileSystems.getDefault().getPath("res", "d2sDenseByEdges.measurement"));
			result.clear();
			sizeMap.clear();

			System.out.println("    From SRE");
			Map<Integer, Integer> sizeMapEdges = new HashMap<>();
			result = ProfilingHelpers.profile(10, 800, dtmc -> Transformer.getNewTransformer(dtmc).getTransformed(),
					i -> {
						DTMC d = Transformer.getNewTransformer(SREGenerator.generateSRE(50 + i)).getTransformed();
						sizeMap.put(i, d.getNodes().size());
						sizeMapEdges.put(i, d.getEdges().size());
						return d;
					}, s -> calculateSize(s));
			writeToCSV(processResultForSREDTMCSRETest(result, sizeMap),
					FileSystems.getDefault().getPath("res", "d2sFromSRE.measurement"));
			writeToCSV(processResultForSREDTMCSRETest(result, sizeMapEdges),
					FileSystems.getDefault().getPath("res", "d2sFromSREByEdges.measurement"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("  SRE -> DTMC");
		try {
			Map<Integer, Set<Tuple<Tuple<Integer, Integer>, Double>>> result;
			System.out.println("    Small");
			result = ProfilingHelpers.profile(1000, 500, 10000, size -> SREGenerator.generateSRE(size),
					sre -> Transformer.getNewTransformer(sre).getTransformed(),
					d -> new Tuple<>(d.getNodes().size(), d.getEdges().size()), size -> size + 500);
			writeToCSV(result, FileSystems.getDefault().getPath("res", "s2dSmall.measurement"));
			result = null;
			System.out.println("    Big");
			result = ProfilingHelpers.profile(100, 2500, 50000, size -> SREGenerator.generateSRE(size),
					sre -> Transformer.getNewTransformer(sre).getTransformed(),
					d -> new Tuple<>(d.getNodes().size(), d.getEdges().size()), size -> size + 2500);
			writeToCSV(result, FileSystems.getDefault().getPath("res", "s2dBig.measurement"));
			result = null;
			System.out.println("    Abnorm");
			result = ProfilingHelpers.profile(100, 100, 50000, size -> SREGenerator.generateAbnormalSRE(size),
					sre -> Transformer.getNewTransformer(sre).getTransformed(),
					d -> new Tuple<>(d.getNodes().size(), d.getEdges().size()), size -> (int) (size * 1.5));
			writeToCSV(result, FileSystems.getDefault().getPath("res", "s2dAbnorm.measurement"));
			result = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static <T extends Number> void writeToCSV(Map<Integer, Set<Tuple<Tuple<T, T>, Double>>> result, Path path)
			throws IOException {
		BufferedWriter w = Files.newBufferedWriter(path);
		List<Integer> keys = new ArrayList<>(result.keySet());
		keys.sort((d1, d2) -> d1 - d2);
		w.write("size,avg,s1avg,s2avg,");
		w.write(IntStream.range(0, result.get(keys.get(0)).size()).mapToObj(i -> "r" + i + ",sa" + i + ",sb" + i)
				.collect(Collectors.joining(",")) + '\n');
		for (int key : keys) {
			Set<Tuple<Tuple<T, T>, Double>> r = result.get(key);
			w.write(key + "," + r.stream().collect(Collectors.averagingDouble(d -> d.y))
					+ "," + r.stream().collect(Collectors.averagingDouble(t -> t.x.x.doubleValue()))
					+ "," + r.stream().collect(Collectors.averagingDouble(t -> t.x.y.doubleValue()))
					+ "," + r.stream().map(t -> t.y + "," + forCSV(t.x)).collect(Collectors.joining(","))
					+ '\n');
		}
		w.close();
	}

	private static <T1, T2> String forCSV(Tuple<T1, T2> t) {
		return t.x.toString() + "," + t.y.toString();
	}

	/**
	 * Calculate the size of a model
	 * 
	 * @param <T>
	 * 
	 * @param x
	 *            The SRE or DTMC to calculate the size from
	 * @return A tuple of integers containing x = number of nodes in the tree
	 *         and y = number of tokens in the string representation or x =
	 *         number of nodes and y = number of edges.
	 */
	private static <T> Tuple<BigInteger, BigInteger> calculateSize(T model) {
		try {
			if (model instanceof SRE) {
				SRE x = (SRE) model;
				Map<SRE, Tuple<BigInteger, BigInteger>> size = new HashMap<>();

				calculateSize(x, size);

				return size.get(x);
			} else if (model instanceof DTMC) {
				DTMC x = (DTMC) model;
				return new Tuple<>(BigInteger.valueOf(x.getNodes().size()), BigInteger.valueOf(x.getEdges().size()));
			} else {
				throw new IllegalArgumentException();
			}
		} catch (Throwable t) {
			return new Tuple<>(BigInteger.valueOf(-1), BigInteger.valueOf(-1));
		}
	}

	private static void calculateSize(SRE sre, Map<SRE, Tuple<BigInteger, BigInteger>> cache) {
		if (cache.containsKey(sre)) {
			return;
		}
		Tuple<BigInteger, BigInteger> size = new Tuple<>(BigInteger.valueOf(1), BigInteger.valueOf(1));
		switch (sre.getType()) {
		case ATOMIC:
			break;
		case CAT:
			for (SRE sub : ((SREConcat) sre).getSubnodes()) {
				calculateSize(sub, cache);
				Tuple<BigInteger, BigInteger> subSize = cache.get(sub);
				size.x = size.x.add(subSize.x);
				size.y = size.y.add(subSize.y).add(BigInteger.valueOf(1));
			}
			break;
		case SUM:
			for (SRE sub : ((SRESum) sre).getSubnodes()) {
				calculateSize(sub, cache);
				Tuple<BigInteger, BigInteger> subSize = cache.get(sub);
				size.x = size.x.add(subSize.x);
				size.y = size.y.add(subSize.y).add(BigInteger.valueOf(2));
			}
			break;
		case KLEENE:
			SRE sub = ((SREKleene) sre).getChild();
			calculateSize(sub, cache);
			Tuple<BigInteger, BigInteger> subSize = cache.get(sub);
			size = new Tuple<>(BigInteger.valueOf(1).add(subSize.x), BigInteger.valueOf(3).add(subSize.y));
			break;
		default:
			throw new IllegalArgumentException();
		}
		cache.put(sre, size);
	}

	private static Map<Integer, Set<Tuple<Tuple<BigInteger, BigInteger>, Double>>> processResultForSREDTMCSRETest(
			Map<Integer, Set<Tuple<Tuple<BigInteger, BigInteger>, Double>>> result, Map<Integer, Integer> sizeMap) {
		Map<Integer, Set<Tuple<Tuple<BigInteger, BigInteger>, Double>>> realSizeMap = mapSizes(result, sizeMap);

		Map<Integer, Set<Tuple<Tuple<BigInteger, BigInteger>, Double>>> chunked = new HashMap<>();
		Set<Tuple<Tuple<BigInteger, BigInteger>, Double>> currentlyBuilding = new HashSet<>();
		int totalSizeOfCurrentlyBuilding = 0;
		for (Entry<Integer, Set<Tuple<Tuple<BigInteger, BigInteger>, Double>>> e : realSizeMap.entrySet()) {
			for (Tuple<Tuple<BigInteger, BigInteger>, Double> value : e.getValue()) {
				currentlyBuilding.add(value);
				totalSizeOfCurrentlyBuilding += e.getKey();
				if (currentlyBuilding.size() >= 50) {
					chunked.put(Math.round(totalSizeOfCurrentlyBuilding / (float) currentlyBuilding.size()),
							currentlyBuilding);
					currentlyBuilding = new HashSet<>();
					totalSizeOfCurrentlyBuilding = 0;
				}
			}
		}
		if (!currentlyBuilding.isEmpty()) {
			chunked.put(Math.round(totalSizeOfCurrentlyBuilding / (float) currentlyBuilding.size()), currentlyBuilding);
		}
		return chunked;
	}

	private static Map<Integer, Set<Tuple<Tuple<BigInteger, BigInteger>, Double>>> mapSizes(
			Map<Integer, Set<Tuple<Tuple<BigInteger, BigInteger>, Double>>> result, Map<Integer, Integer> sizeMap) {
		Map<Integer, Set<Tuple<Tuple<BigInteger, BigInteger>, Double>>> realSizeMap = new HashMap<>();
		for (Integer i : result.keySet()) {
			if (!realSizeMap.containsKey(sizeMap.get(i))) {
				realSizeMap.put(sizeMap.get(i), new HashSet<>());
			}
			realSizeMap.get(sizeMap.get(i)).addAll(result.get(i));
		}
		return realSizeMap;
	}

}
