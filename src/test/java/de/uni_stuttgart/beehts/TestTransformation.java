package de.uni_stuttgart.beehts;

import org.junit.Test;

import de.uni_stuttgart.beehts.model.*;
import de.uni_stuttgart.beehts.model.construction.*;
import de.uni_stuttgart.beehts.transformation.Transformer;

public class TestTransformation {

	@Test
	public void testSRE2DTMC() {
		SRE sre = SREBuilder.parse("((a:b)*0.2)[1]+c[2]");
		Delta<SRE> deltaSRE = SREDelta.parse(sre, "2 > d:\\2");
		Transformer<SRE, DTMC> s2d = Transformer.getNewTransformer(sre);
		s2d.applyDelta(deltaSRE);
	}

	@Test
	public void testDTMC2SRE() {
		DTMC dtmc = DTMCParser.parse("0 \n 1 \n 0 1 0.23 a \n 0 1 0.77 b");
		Transformer<DTMC, SRE> d2s = Transformer.getNewTransformer(dtmc);
		DTMCDeltaResticted deltaDTMC = new DTMCDeltaResticted();
		deltaDTMC.addChange(dtmc.getEdges().stream().filter(e -> e.character.equals("a")).reduce(null, (e1, e2) -> e2),
				DTMCParser.parse("0 \n 1 \n 0 1 0.23 a \n 0 1 0.77 b"));
		
		System.out.println(d2s.getTransformed());
		
		d2s.applyDelta(deltaDTMC);
		
		System.out.println(d2s.getTransformed());
	}
}
