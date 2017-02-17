package de.uni_stuttgart.beehts;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import de.uni_stuttgart.beehts.generator.SREGenerator;
import de.uni_stuttgart.beehts.model.DTMC;
import de.uni_stuttgart.beehts.model.Delta;
import de.uni_stuttgart.beehts.model.SRE;
import de.uni_stuttgart.beehts.model.SRE.SREAtomic;
import de.uni_stuttgart.beehts.model.SREDelta;
import de.uni_stuttgart.beehts.model.Tuple;
import de.uni_stuttgart.beehts.model.construction.DTMCParser;
import de.uni_stuttgart.beehts.model.construction.SREBuilder;
import de.uni_stuttgart.beehts.transformation.DTMC2SREBrzozowski;
import de.uni_stuttgart.beehts.transformation.DTMC2SREDeltaBrz;
import de.uni_stuttgart.beehts.transformation.SRE2DTMCDelta;
import de.uni_stuttgart.beehts.transformation.SRE2DTMCTransformer;
import de.uni_stuttgart.beehts.transformation.Transformer;

/**
 * This used to be the profiling class. As this is not suitable as a test case
 * and more profiling needed to be done this is deprecated in favour of
 * Evaluation.java.
 * 
 * @author Tobias Beeh
 */
@Deprecated
public class CaseStudy {

	public void sre2dtmc() throws IOException {
		Path path = FileSystems.getDefault().getPath("res", "CaseStudy.grammar").toAbsolutePath();
		String s = String.join("\n", Files.readAllLines(path));
		Profiler prof = measureTimeAndMemory((p) -> {
			SRE sre = SREBuilder.parse(s);
			p.addDataPoint(" Parsing SRE    ");
			Delta<SRE> deltaSRE = SREDelta.parse(sre, "2 > d:\\2");
			p.addDataPoint(" Parsing Delta  ");
			Transformer<SRE, DTMC> s2d = Transformer.getNewTransformer(sre);
			p.addDataPoint(" Transforming   ");
			s2d.applyDelta(deltaSRE);
			p.addDataPoint(" Transform Delta");
			p.printTotal();
			return s2d;
		}, 1);
		prof.printTotal();
	}

	public void countSize() throws IOException {
		Path path = FileSystems.getDefault().getPath("res", "leader4_4.tra.transformed").toAbsolutePath();
		String s = String.join("\n", Files.readAllLines(path));
		SRE sre = SREBuilder.parse(s);
		System.out.println(sre.traverse(new SRE.Traverser() {

			int numOfAtomicSREs = 0;

			@Override
			protected void inOrder(SRE sre) {
				if (sre instanceof SREAtomic) {
					numOfAtomicSREs++;
				}
			}

			@Override
			public String toString() {
				return numOfAtomicSREs + "";
			}
		}).x.numOfAtomicSREs);
	}

	public void dtmc2sre() throws IOException {
		Path path = FileSystems.getDefault().getPath("res", "leader6_6.tra").toAbsolutePath();
		String s = String.join("\n", Files.readAllLines(path));
		Profiler prof = measureTimeAndMemory((p) -> {
			DTMC dtmc = DTMCParser.parse(s);
			p.addDataPoint(" Parsing DTMC:   ");
			Transformer<DTMC, SRE> d2s = Transformer.getNewTransformer(dtmc);
			p.addDataPoint(" Transforming:   ");
			p.printTotal();
			return d2s;
		}, 1);
		prof.printTotal();
	}

	public void s2dmemory() throws IOException {
		SRE sre = SREGenerator.generateSRE(200);

		measureTimeAndMemory(p -> {
			return new SRE2DTMCTransformer(sre);
		}, 1).printTotal();

		measureTimeAndMemory(p -> {
			return new SRE2DTMCDelta(sre);
		}, 1).printTotal();
	}

	public void d2smemory() throws IOException {
		Path path = FileSystems.getDefault().getPath("res", "leader4_6.tra").toAbsolutePath();
		String s = String.join("\n", Files.readAllLines(path));
		DTMC dtmc = DTMCParser.parse(s);

		measureTimeAndMemory((p) -> {
			return new DTMC2SREBrzozowski(dtmc);
		}, 1000).printTotal();

		measureTimeAndMemory((p) -> {
			return new DTMC2SREDeltaBrz(dtmc);
		}, 1000).printTotal();
	}

	private static <T> Profiler measureTimeAndMemory(Function<Profiler, T> run, int repeatTimes) {
		System.gc();
		@SuppressWarnings("unchecked")
		T[] t = (T[]) new Object[repeatTimes];
		Profiler p = new Profiler();
		for (int i = 0; i < repeatTimes; i++) {
			t[i] = run.apply(new Profiler());
			p.addDataPoint(" Run #" + i);
		}
		return p;
	}

	private static class Profiler {

		private Tuple<Double, Double> totalAtLastDataPoint = new Tuple<>(System.nanoTime() / 1.e9,
				Runtime.getRuntime().freeMemory() / 1.e6);
		private List<Tuple<Double, Double>> dataPoints = new LinkedList<>();

		public void addDataPoint(String s) {
			double time = System.nanoTime() / 1.e9 - totalAtLastDataPoint.x;
			double mem = Runtime.getRuntime().freeMemory() / 1.e6 - totalAtLastDataPoint.y;
			dataPoints.add(new Tuple<>(time, mem));
			totalAtLastDataPoint = new Tuple<>(totalAtLastDataPoint.x + time, totalAtLastDataPoint.y + mem);
			System.out.printf(s + " %.2fs, %.2fMB\n", time, mem);
		}

		public Tuple<Double, Double> getTotal() {
			return dataPoints.stream().reduce(new Tuple<>(0., 0.),
					(t1, t2) -> new Tuple<>(t2.x + t1.x, t2.y + t1.y));
		}

		public void printTotal() {
			Tuple<Double, Double> total = getTotal();
			System.out.println("TOTAL: " + total.x + "s, " + total.y + "MB");
		}
	}

}
