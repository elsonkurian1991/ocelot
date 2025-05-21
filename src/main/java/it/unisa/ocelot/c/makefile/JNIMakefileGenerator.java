package it.unisa.ocelot.c.makefile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.util.Utils;

public abstract class JNIMakefileGenerator {
	private String filename;
	private List<String> linkLibs;
	
	public JNIMakefileGenerator(String pFilename) {
		this.filename = pFilename;
		this.linkLibs = new ArrayList<>();
		this.linkLibs.add("glib-2.0");
	}
	
	public abstract String getCCompiler();
	public abstract String getJavaHome();
	public abstract String getSystemInclude();
	
	public abstract String[] getGlib2Paths();
	public abstract String[] getJavaPaths();
	public abstract String getMoreOptions();
	
	public abstract String getCFlags();
	
	public void addLinkLibrary(String pLibrary) {
		this.linkLibs.add(pLibrary);
	}
	
	public abstract String getLibName();
	
	public void generate() throws IOException {
		/*String supportFiles= 
				" BaliseGroupMonitoringWhenLinkingInfoIsUsed_BCAL_Lib_DM_TIM_BaliseMM_LIU.c"
				+ " LinkingWindowConsistencyAndManagement_LMC_Lib_DM_TIM_BaliseMM_LMC.c"
				+ " CheckLinkingConsistency_DC_Lib_DM_TIM_BaliseMM_LMC.c"
				+ " LMC_Root_LMC_Lib_DM_TIM_BaliseMM_LMC.c"
				+ " ControlExpectationWindow_BCAL_Lib_DM_TIM_BaliseMM_LMC.c"
				+ " ObtainLocationReferenceBalise_DC_Lib_DM_TIM_BaliseMM_LMC.c"
				+ " DMI_Msg_LinkingConsistency_DC_Lib_DM_TIM_BaliseMM_DC_Balise.c"
				+ " ErrorReport_DC_Lib_DM_TIM_ErrorReporting.c"
				+ " ExpectationWindow_Calculator_BCAL_Lib_DM_TIM_BaliseMM_LMC.c"
				+ " RAMS_CrossTalkMitigation_DC_Lib_DM_TIM_BaliseMM_LMC.c"
				+ " RAMS_LinkingConsistency_DC_Lib_DM_TIM_BaliseMM_LMC.c"
				+ " LinkingConsistencyAndManagement_LMC_Lib_DM_TIM_BaliseMM_LMC.c"
				+ " UnexpectedDirection_DC_Lib_DM_TIM_BaliseMM_LMC.c"
				+ " dataBaseForKcg.c"
				+ " kcg_consts.c"
				+ " kcg_types.c"
				+ " initDataBase.c"
				+ " genericDataBase.c ";*/

		String supportFiles= getSupportFiles();
		
		//System.out.println(supportFiles);
		
		String glib2paths = "";
		for (String temp : this.getGlib2Paths())
			glib2paths += "-I"+temp+" ";
		
		String javapaths = "";
		for (String temp : this.getJavaPaths())
			javapaths += "-I"+temp+" ";
		
		String libspath = "";
		for (String temp : this.linkLibs)
			libspath += "-l"+temp+" ";
		
		String moreOptions = this.getMoreOptions();
	
		String result = "CC ="+this.getCCompiler()+" \n\n" +
		"JAVA_HOME = " + this.getJavaHome() + "\n\n" +
		"SYSTEM_INCLUDE = " + this.getSystemInclude() + "\n\n" +
		"GLIB2_INCUDE = " + glib2paths + "\n" +
		"JAVA_INCLUDE = " + javapaths + "\n\n" +
		"CFLAGS = " + this.getCFlags() + "\n\n" + 
		"INCLUDES = $(GLIB2_INCUDE) $(JAVA_INCLUDE)\n" +
		"LIBS = " + libspath + "\n" +
		"SRCS = lists.c ocelot.c EN_CBridge.c main.c "+supportFiles+" \n\n"
				+"MOREOPTS = " + moreOptions + "\n\n"+
		"MAIN = ../" + this.getLibName() + "\n\n" +
		".PHONY: clean all\n" +
		"all:\n"+
		"\t$(CC) $(CFLAGS) $(INCLUDES) -o $(MAIN) $(LFLAGS) $(SRCS) $(LIBS) $(MOREOPTS)\n"+
		//"\t$(RM) EN_CBridge.c\n"+
		"clean:\n" +
		"\t$(RM) $(MAIN)\n";
		
		Utils.writeFile(this.filename, result);
		//System.out.println(result);
	}
	

	private String getSupportFiles() throws IOException {
		List<String> fileNames = new ArrayList<>();
		ConfigManager getConfInfo=ConfigManager.getInstance();		
		String[] supportFiles= getConfInfo.getTestIncludePaths();
		for(int i=0;i<supportFiles.length;i++) {
			//System.out.println(supportFiles[i]);
			if(supportFiles[i].endsWith(".c")) {
				//System.out.println(supportFiles[i]);
				int lastIndex=supportFiles[i].lastIndexOf('/');
				fileNames.add(supportFiles[i].substring(lastIndex+1));
			}
			
		}	
		return String.join(" ", fileNames);
	}

	public abstract Process runCompiler() throws IOException;
}
