package it.unisa.ocelot.c.compiler;

import it.unisa.ocelot.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.core.parser.tests.scanner.FileCodeReaderFactory;
import org.eclipse.core.runtime.CoreException;

public class GCC implements Compiler {

	@Override
	public String preprocess(File pInput) {
		return commandLine(new String[] {"bash",  "-c", "gcc -E \"" + pInput.getAbsolutePath() + "\" | sed '/^\\#/d'"});
	}
	
	@Override
	public String preprocess(File pInput, File pOutput) {
		return commandLine(new String[] {"gcc",  "-E", pInput.getPath()});
	}

	@Override
	public void compile(File pInput, File pOutput) {
		// TODO Auto-generated method stub
	}

	private String commandLine(String[] pCommand) {
		try {
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(pCommand);
			
			return IOUtils.toString(pr.getInputStream());
		} catch (IOException e) {
			System.err.println("Unknown error. Could not execute \"" + pCommand + "\"");
			return "";
		}
	}
	
	/**
	 * Gets the {@link IASTTranslationUnit} object for a given
	 * source file.
	 * 
	 * @param pSourceFilename a {@link String}, the name of the file.
	 * @param pIncludePaths a {@link String}{@code ...}, a list of
	 *        paths where the header files to be included must be
	 *        searched.
	 * @return a {@link IASTTranslationUnit}.
	 * @throws IOException
	 * @throws CoreException
	 */
	public static IASTTranslationUnit getTranslationUnit(String pSourceFilename, String... pIncludePaths)
	throws IOException, CoreException {
		String sourceCode = Utils.readFile(pSourceFilename);
		FileContent sourceFileContent = FileContent.create(pSourceFilename, sourceCode.toCharArray());
		IScannerInfo scannerInfo = new ScannerInfo(new HashMap<>(), pIncludePaths);
		
		return GCCLanguage.getDefault().getASTTranslationUnit(
				sourceFileContent, scannerInfo, 
				FileCodeReaderFactory.getInstance(), null, 
				ILanguage.OPTION_IS_SOURCE_UNIT, 
				new DefaultLogService());
	}

	/**
	 * Gets the {@link IASTTranslationUnit} object for a given
	 * source file (no path for #include files).
	 * 
	 * @param pSourceFilename a {@link String}, the name of the file.
	 * @return a {@link IASTTranslationUnit}.
	 * @throws IOException
	 * @throws CoreException
	 */
	public static IASTTranslationUnit getTranslationUnit(String pSourceFilename)
	throws IOException, CoreException {		
		return getTranslationUnit(pSourceFilename, new String[0]);
	}
}
