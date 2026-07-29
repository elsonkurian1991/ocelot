package it.unisa.ocelot.writer.check;

import it.unisa.ocelot.writer.TestCaseWriter;
import it.unisa.ocelot.writer.TestFrameworkFactory;
import it.unisa.ocelot.writer.TestSuiteWriter;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
public class CheckFactory implements TestFrameworkFactory {

	@Override
	public TestSuiteWriter<CheckTestCaseWriter> getTestSuiteWriterInstance() {
		return new CheckTestSuiteWriter();
	}

	@Override
	public TestCaseWriter getTestCaseWriterInstance(int id) {
		return new CheckTestCaseWriter(id);
	}

}
