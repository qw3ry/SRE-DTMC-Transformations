package de.uni_stuttgart.beehts;

import org.junit.Test;

import de.uni_stuttgart.beehts.model.DTMC;
import de.uni_stuttgart.beehts.model.DTMCDelta;
import de.uni_stuttgart.beehts.model.Delta;
import de.uni_stuttgart.beehts.model.SRE;
import de.uni_stuttgart.beehts.model.SREDelta;
import de.uni_stuttgart.beehts.model.construction.DTMCParser;
import de.uni_stuttgart.beehts.model.construction.SREBuilder;

public class TestModels {

	@Test
	public void parseSRE() {
		SREBuilder.parse("((a:b)*0.2)[1]+c[2]");
	}

	@Test
	public void parseDTMC() {
		// first syntax type
		DTMCParser.parse("I: 0; F: 1;");
		DTMCParser.parse("I: 0; F: 1; 0 --> 1 (\"a\":1.0)");

		// second syntax type
		DTMCParser.parse("0 \n 1 2 \n");
		DTMCParser.parse("0 \n 1 2 \n 0 1 0.23 a \n 0 2 0.77 b \n 1 2 1 c");
	}

	@Test
	public void applyDelta() {
		DTMC dtmc = DTMCParser.parse("0 \n 1 2 \n 0 1 0.23 a \n 0 2 0.77 b \n 1 2 1 c");
		Delta<DTMC> deltaDTMC = DTMCDelta.parse("0 2 b > 0 2 0.5 b\n+0 0 0.27 d", dtmc);
		dtmc = deltaDTMC.applyChanges(dtmc);

		SRE sre = SREBuilder.parse("((a:b)*0.2)[1]+c[2]");
		Delta<SRE> deltaSRE = SREDelta.parse(sre, "2 > (d[1] + e[3]):\\2");
		sre = deltaSRE.applyChanges(sre);
	}
}
