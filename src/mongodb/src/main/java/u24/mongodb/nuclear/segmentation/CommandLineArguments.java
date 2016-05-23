package u24.mongodb.nuclear.segmentation;

import org.apache.commons.cli.*;

/**
 * Parses command line arguments.
 * Uses Apache Commons CLI.
 */
public class CommandLineArguments {
    private static Options     allOpts;
    private static OptionGroup normOptGrp;
    private static CommandLine cmdLine;

    private static String dbServer 	= null;
    private static int    dbPort 	= 27017;
    private static String dbHost 	= "localhost";
    private static String dbName 	= null;
    private static String dbUser    = null;
    private static String dbPasswd  = null;

    private static String  inpType    = null;
    private static boolean isQuip     = false;
    private static boolean isCSV      = false;
    private static boolean isAperio   = false;
    private static boolean isMask     = false;
    private static boolean isMaskTile = false;
    private static boolean isTSV      = false;
    
    private static String inpList   = null;
    private static String imgFile   = null;
    private static String inpFile   = null;
    private static String outFolder = null;

    private static String  caseID    = null;
    private static String  subjectID = null;
    
    private static boolean doNormalize     = false;
    private static boolean doSelfNormalize = false;
    private static boolean getFromDB       = false;
    
    private static int width 	= 1;
    private static int height 	= 1;
    private static int shiftX 	= 0;
    private static int shiftY 	= 0;
    
    private static String execID 	= null;
    private static String studyID 	= "main";
	private static String batchID 	= "b0";
	private static String tagID 	= "t0";
    private static String execType 	= "computer";
    private static String execTitle = null;
    private static String execColor = "yellow";
    private static String execComp 	= "segmentation";
    private static String nameSpace = "http://u24.bmi.stonybrook.edu/v1";
    
    private static boolean doSimplify = false; 
    private static double  minSize    = 0.0;
    private static double  maxSize    = 40000000000.0; // large number 

	private static void defineDBOptions() {
		Option dbport = Option.builder()
				.longOpt("dbport")
				.desc("Database port.")
				.hasArg()
				.argName("dbport")
				.build();
		Option dbhost = Option.builder()
				.longOpt("dbhost")
				.desc("Database host.")
				.hasArg()
				.argName("dbhost")
				.build();
		Option dbname = Option.builder()
				.longOpt("dbname")
				.desc("Database name (required, if --dest is db).")
				.hasArg()
				.argName("dbname")
				.build();
		
		Option dbuser = Option.builder()
				.longOpt("dbuser")
				.desc("Database user.")
				.hasArg()
				.argName("dbuser")
				.build();
		Option dbpasswd = Option.builder()
				.longOpt("dbpasswd")
				.desc("Database password.")
				.hasArg()
				.argName("dbpasswd")
				.build();
		
		allOpts.addOption(dbhost);
		allOpts.addOption(dbport);
		allOpts.addOption(dbname);
		allOpts.addOption(dbuser);
		allOpts.addOption(dbpasswd);
	}

	/**
	 * Input options.
	 */
	private static void defineInputTypeOptions() {
		Option inpType = Option
				.builder()
				.longOpt("inptype")
				.desc("Input type: mask (binary mask (0/1) format), csv (QUIP csv format), tsv (QUIP tab-separated value files), aperio (Aperio XML markup.)")
				.hasArg()
				.argName("mask|csv|tsv|aperio")
				.required(true)
				.build();
		allOpts.addOption(inpType);
		
		Option isQUIP = Option
				.builder()
				.longOpt("quip")
				.desc("QUIP analysis file collection")
				.build();
		allOpts.addOption(isQUIP);

		OptionGroup inpOptGrp = new OptionGroup();
		Option inpFile = Option.builder()
				.longOpt("inpfile")
				.desc("Input file.")
				.hasArg()
				.argName("filename")
				.build();
		Option inpList = Option
				.builder()
				.longOpt("inplist")
				.desc("File containing a list of masks with QUIP filename format or QUIP CSV/TSV files.")
				.hasArg()
				.argName("filename")
				.build();
		inpOptGrp.addOption(inpFile);
		inpOptGrp.addOption(inpList);
		allOpts.addOptionGroup(inpOptGrp);
	}

	/**
	 * Shift and normalization options for input segmentations.
	 */
	private static void defineShiftNormalizationOptions() {
		Option shift = Option.builder()
				.longOpt("shift")
				.desc("Shift in X and Y dimensions for a mask.")
				.numberOfArgs(2)
				.argName("x,y")
				.valueSeparator(',')
				.build();
		Option normalize = Option
				.builder()
				.longOpt("norm")
				.desc("Normalize polygon coordinates to [0,1] using width (w) and height (h).")
				.numberOfArgs(2)
				.argName("w,h")
				.valueSeparator(',')
				.build();
		Option normbyself = Option
				.builder()
				.longOpt("self")
				.desc("Normalize polygon coordinates to [0,1] using image metadata in input file(s).")
				.build();

		allOpts.addOption(shift);
		normOptGrp.addOption(normalize);
		normOptGrp.addOption(normbyself);
	}

	/**
	 * Options to simplify and filter polygons in CSV files.
	 */
	private static void defineSimplifyFilterOptions() {
		Option simplify = Option.builder()
				.longOpt("simplify")
				.desc("Simplify polygons in CSV files using the JTS library.")
				.build();
		Option areafilter = Option.builder()
				.longOpt("sizefilter")
				.desc("Filter polygons based on polygon area.")
				.numberOfArgs(2)
				.argName("minSize,maxSize")
				.valueSeparator(',')
				.build();
		allOpts.addOption(simplify);
		allOpts.addOption(areafilter);
	}

	/**
	 * Image source options.
	 */
	private static void defineImageSourceOptions() {
		Option caseID = Option.builder()
				.longOpt("cid")
				.desc("Case ID (Image ID)")
				.hasArg()
				.argName("caseid")
				.build();
		Option subjectID = Option.builder().
				longOpt("sid")
				.desc("Subject ID")
				.hasArg()
				.argName("subjectid")
				.build();
		Option imgFile = Option.builder()
				.longOpt("img")
				.desc("Image file from which mask files were generated.")
				.hasArg()
				.argName("filename")
				.build();
		Option getFromDB = Option.builder()
				.longOpt("fromdb")
				.desc("Get image metadata from FeatureDB.")
				.build();
		
		allOpts.addOption(caseID);
		allOpts.addOption(subjectID);
		normOptGrp.addOption(imgFile);
		normOptGrp.addOption(getFromDB);
	}

	/**
	 * Analysis provenance.
	 */
	private static void defineAnalysisProvenanceOptions() {

		Option algoID = Option.builder()
				.longOpt("eid")
				.desc("Analysis execution id.")
				.hasArg()
				.argName("execid")
				.build();

		Option studyID = Option.builder()
				.longOpt("studyid")
				.desc("Study id (default: main).")
				.hasArg()
				.argName("studyid")
				.build();

		Option batchID = Option.builder()
				.longOpt("batchid")
				.desc("Batch id.")
				.hasArg()
				.argName("batchid")
				.build();

		Option tagID = Option.builder()
				.longOpt("tagid")
				.desc("Tag id.")
				.hasArg()
				.argName("tagid")
				.build();

		Option algoType = Option.builder()
				.longOpt("etype")
				.desc("Analysis type: human|computer.")
				.hasArg()
				.argName("type")
				.build();

		Option algoTitle = Option
				.builder()
				.longOpt("etitle")
				.desc("Analysis title for FeatureDB storage and visualization.")
				.hasArg()
				.argName("title")
				.build();
		
		Option nmSpace = Option.builder()
				.longOpt("namespace")
				.desc("Namespace for feature attribute names.")
				.hasArg()
				.argName("namespace")
				.build();

		Option color = Option.builder()
				.longOpt("ecolor")
				.desc("Color of segmentations for visualization.")
				.hasArg()
				.argName("color")
				.build();

		Option algoComp = Option.builder()
				.longOpt("ecomp")
				.desc("Analysis computation type (e.g., segmentation).")
				.hasArg()
				.argName("computation")
				.build();

		allOpts.addOption(algoID);
		allOpts.addOption(studyID);
		allOpts.addOption(batchID);
		allOpts.addOption(tagID);
		allOpts.addOption(algoType);
		allOpts.addOption(algoTitle);
		allOpts.addOption(nmSpace);
		allOpts.addOption(color);
		allOpts.addOption(algoComp);
	}

	/**
	 * Destination.
	 */
	private static void defineDestinationOptions() {

		Option dest = Option
				.builder()
				.longOpt("dest")
				.desc("Output: JSON file (works with aperio and mask single file options only) or FeatureDB database.")
				.hasArg()
				.argName("file|db")
				.build();
		Option outFile = Option.builder()
				.longOpt("outfolder")
				.desc("Folder where output file will be written.")
				.hasArg()
				.argName("folder")
				.build();
		allOpts.addOption(dest);
		allOpts.addOption(outFile);
	}

	public static void initCommandLineOptions() {
		allOpts = new Options();
		normOptGrp = new OptionGroup();

		defineDBOptions();
		defineInputTypeOptions();

		defineImageSourceOptions();
		defineShiftNormalizationOptions();
		allOpts.addOptionGroup(normOptGrp);

		defineSimplifyFilterOptions();

		defineAnalysisProvenanceOptions();
		defineDestinationOptions();
	}

	public static void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(100);
		System.out
				.println("Program to process Binary Mask, CSV, TSV, or Aperio XML files and load to FeatureDB.\n");
		formatter.printHelp(" ", " ", allOpts, " ", true);
	}

	/**
	 * Get input values.
	 */
	private static boolean parseInputTypeOptions() {

		inpType = cmdLine.getOptionValue("inptype");
		
		// legacy mapping of mask file variations
		if (inpType.equals("maskfile") || inpType.equals("mask") || inpType.equals("masktile")) {
			inpType="mask";
			isMaskTile = true;
			isMask = true;
		}
		if (inpType.equals("csv")) isCSV = true;
		if (inpType.equals("tsv")) isTSV = true;
		
		if (inpType.equals("aperio")) {
			if (cmdLine.hasOption("inpfile")) {
				inpFile = cmdLine.getOptionValue("inpfile");
			} else {
				System.err.println("ERROR: aperio option" + "requires --inpfile parameter.");
			}
			isAperio = true;
		} else if (inpType.equals("csv") 
				|| inpType.equals("tsv") 
				|| inpType.equals("mask")) {
			if (cmdLine.hasOption("inplist")) {
				inpList = cmdLine.getOptionValue("inplist");
			} else if (cmdLine.hasOption("inpfile")) {
				inpFile = cmdLine.getOptionValue("inpfile");
			} else {
				System.err.println("ERROR: --inplist or --inpfile parameter is required.");
				return false;
			}
		} else {
			System.err.println("ERROR: Unknown value: (" + inpType + ") for inptype.");
			return false;
		}
		
		if (cmdLine.hasOption("quip")) isQuip = true;
		
		return true;
	}

	private static boolean parseDatabaseOptions() {

		if (cmdLine.hasOption("dbhost"))
			dbHost = cmdLine.getOptionValue("dbhost");
		if (cmdLine.hasOption("dbport"))
			dbPort = Integer.parseInt(cmdLine.getOptionValue("dbport"));
		if (cmdLine.hasOption("dbname")) {
			dbName = cmdLine.getOptionValue("dbname");
		} else {
			System.err.println("No database name is provided.");
			return false;
		}
		
		if (cmdLine.hasOption("dbuser"))
			dbUser = cmdLine.getOptionValue("dbuser");
		if (cmdLine.hasOption("dbpasswd"))
			dbPasswd = cmdLine.getOptionValue("dbpasswd");
		
		if (dbUser!=null)
			dbServer = "mongodb://" + dbUser + ":" + dbPasswd + "@" + dbHost + ":" + dbPort + "/" + dbName;
		else
			dbServer = "mongodb://" + dbHost + ":" + dbPort + "/" + dbName;
		
		return true;
	}

	/**
	 * Check destination and get values.
	 */
	private static boolean parseDestinationOptions() {

		if (!cmdLine.hasOption("dest")) 
			return parseDatabaseOptions();

		String destVal = cmdLine.getOptionValue("dest");
		if (destVal.equals("db")) {
			return parseDatabaseOptions();
		} else if (destVal.equals("file")) {
			if (!cmdLine.hasOption("outfolder")) {
				System.err.println("Destination is file, but no foldername given.");
				return false;
			} else {
				outFolder = cmdLine.getOptionValue("outfolder");
				return true;
			}
		} else {
			System.err.println("Unknown destination option.");
			return false;
		}
	}

	/**
	 * Get source image values.
	 */
	private static boolean parseImageSourceOptions() {

		if (cmdLine.hasOption("img")) {
			imgFile = cmdLine.getOptionValue("img");
			doNormalize = true;
		}
		if (cmdLine.hasOption("cid")) {
			caseID = cmdLine.getOptionValue("cid");
		} else {
			caseID = "undefined";
		}
		if (cmdLine.hasOption("sid")) {
			subjectID = cmdLine.getOptionValue("sid");
		} else {
			subjectID = "undefined";
		}

		// Get image metadata from FeatureDB
		if (cmdLine.hasOption("fromdb")) {
			getFromDB   = true;
			doNormalize = true;
		}
		return true;
	}

	/**
	 * Get shift and normalization values.
	 */
	private static boolean parseShiftNormalizationOptions() {

		if (cmdLine.hasOption("shift")) {
			shiftX = Integer.parseInt(cmdLine.getOptionValues("shift")[0]);
			shiftY = Integer.parseInt(cmdLine.getOptionValues("shift")[1]);
		}
		if (cmdLine.hasOption("norm")) {
			doNormalize = true;
			width  = Integer.parseInt(cmdLine.getOptionValues("norm")[0]);
			height = Integer.parseInt(cmdLine.getOptionValues("norm")[1]);
		}
		if (cmdLine.hasOption("self")) {
			doNormalize     = true;
			doSelfNormalize = true;
		}
		return true;
	}

	private static boolean parseSimplifyFilterOptions() {
		if (cmdLine.hasOption("sizefilter")) {
			minSize = Double.parseDouble(cmdLine.getOptionValues("sizefilter")[0]);
			maxSize = Double.parseDouble(cmdLine.getOptionValues("sizefilter")[1]);
		}
		if (cmdLine.hasOption("simplify")) {
			doSimplify = true;
		}
		return true;
	}

	/**
	 * Get analysis values.
	 */
	private static boolean parseAnalysisProvenanceOptions() {

		if (!cmdLine.hasOption("quip")) {
			if (!cmdLine.hasOption("eid")) {
				System.err.println("ERROR: execution id <eid> is not defined.");
				return false;
			} else {
				execID 	 = cmdLine.getOptionValue("eid");
			}
		}
		studyID  = cmdLine.getOptionValue("studyid");

		if (cmdLine.hasOption("etype"))
			execType = cmdLine.getOptionValue("etype");
		if (cmdLine.hasOption("batchid"))
			batchID  = cmdLine.getOptionValue("batchid");
		if (cmdLine.hasOption("tagid"))
			tagID  = cmdLine.getOptionValue("tagid");
		if (cmdLine.hasOption("ecolor"))
			execColor  = cmdLine.getOptionValue("ecolor");
		if (cmdLine.hasOption("ecomp"))
			execComp  = cmdLine.getOptionValue("ecomp");
		if (cmdLine.hasOption("namespace"))
			nameSpace  = cmdLine.getOptionValue("namespace");

		execTitle = cmdLine.hasOption("etitle") ? cmdLine.getOptionValue("etitle") : "Algorithm: " + execID;

		return true;
	}

	public static boolean parseCommandLineArgs(String args[]) {
		CommandLineParser parser = new DefaultParser();
		try {
			cmdLine = parser.parse(allOpts, args);
		} catch (ParseException exp) {
			System.err.println("ERROR: " + exp.getMessage());
			return false;
		}
		
		boolean parseRet = parseInputTypeOptions();
		parseRet &= parseDestinationOptions();
		parseRet &= parseImageSourceOptions();
		parseRet &= parseShiftNormalizationOptions();
		parseRet &= parseSimplifyFilterOptions();
		parseRet &= parseAnalysisProvenanceOptions();

		return parseRet; 
	}

	// Getters for DB
	public static String getDBServer() {
		return dbServer;
	}

	public static String getDBHost() {
		return dbHost;
	}

	public static int getDBPort() {
		return dbPort;
	}

	public static String getDBName() {
		return dbName;
	}
	
	public static String getDBUser() {
		return dbUser;
	}
	
	public static String getDBPasswd() {
		return dbPasswd;
	}

	// Getters for input type
	public static boolean isMaskFile() {
		return isMask;
	}

	public static boolean isMaskTile() {
		return isMaskTile;
	}

	public static boolean isTSV() {
		return isTSV;
	}

	public static boolean isCSV() {
		return isCSV;
	}

	public static boolean isAperio() {
		return isAperio;
	}
	
	public static boolean isQuip() {
		return isQuip;
	}

	public static String getInpList() {
		return inpList;
	}

	public static String getInpFile() {
		return inpFile;
	}

	public static String getOutFoldername() {
		return outFolder;
	}

	public static String getCaseID() {
		return caseID;
	}

	public static String getSubjectID() {
		return subjectID;
	}

	public static boolean isNormalize() {
		return doNormalize;
	}

	public static boolean isSelfNormalize() {
		return doSelfNormalize;
	}

	public static int getShiftX() {
		return shiftX;
	}

	public static int getShiftY() {
		return shiftY;
	}

	public static int getWidth() {
		return width;
	}

	public static int getHeight() {
		return height;
	}

	public static boolean isSimplify() {
		return doSimplify;
	}

	public static double getMinSize() {
		return minSize;
	}

	public static double getMaxSize() {
		return maxSize;
	}

	public static boolean isGetFromDB() {
		return getFromDB;
	}

	public static String getInpImage() {
		return imgFile;
	}

	public static String getExecutionID() {
		return execID;
	}
	
	public static String getExecutionType() {
		return execType;
	}

	public static String getExecutionTitle() {
		return execTitle;
	}
	
	public static String getExecutionComputation() {
		return execComp;
	}

	public static String getStudyID() {
		return studyID;
	}

	public static String getBatchID() {
		return batchID;
	}

	public static String getTagID() {
		return tagID;
	}

	public static String getNamespace() {
		return nameSpace;
	}

	public static String getColor() {
		return execColor;
	}
	
}
