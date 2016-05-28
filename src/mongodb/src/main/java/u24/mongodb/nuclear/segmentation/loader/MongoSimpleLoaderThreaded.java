package u24.mongodb.nuclear.segmentation.loader;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import u24.mongodb.nuclear.segmentation.cli.CommandLineArguments;
import u24.mongodb.nuclear.segmentation.cli.FileParameters;
import u24.mongodb.nuclear.segmentation.cli.InputListArray;
import u24.mongodb.nuclear.segmentation.cli.InputParameters;
import u24.mongodb.nuclear.segmentation.cli.IterateInputData;
import u24.mongodb.nuclear.segmentation.database.ResultsDatabase;
import u24.mongodb.nuclear.segmentation.model.AnalysisExecutionMetadata;
import u24.mongodb.nuclear.segmentation.parser.ProcessAperioXMLFile;
import u24.mongodb.nuclear.segmentation.parser.ProcessBinaryMaskFile;
import u24.mongodb.nuclear.segmentation.parser.ProcessCSVFeaturePolygonFile;
import u24.mongodb.nuclear.segmentation.parser.ProcessFile;
import u24.mongodb.nuclear.segmentation.parser.ProcessQuipCSVFile;
import u24.mongodb.nuclear.segmentation.parser.ProcessQuipMaskFile;
import u24.mongodb.nuclear.segmentation.parser.ProcessTSVFeaturePolygonFile;

class ProcessFileThread implements Runnable {
    private Thread myThread;
    private ProcessFile inpFile;

    public ProcessFileThread(ProcessFile inpFile) {
        this.myThread = new Thread(this, "myThread");
        this.inpFile = inpFile;
        myThread.start();
    }

    public void run() {
        inpFile.processFile();
    }

    public boolean isAlive() {
        return this.myThread.isAlive();
    }
}

public class MongoSimpleLoaderThreaded {
	
	private static InputParameters inputParams;
	
	private static ResultsDatabase[] setupDatabaseConnections(int numThreads, String segdbServer) {
		ResultsDatabase[] segDB = new ResultsDatabase[numThreads];
		if (segdbServer!=null) {
        	for (int i = 0; i < numThreads; i++) 
            	segDB[i] = new ResultsDatabase(segdbServer);
		} else {
        	for (int i = 0; i < numThreads; i++) 
				segDB[i] = null;
		}
        return segDB;
	}
	
	private static boolean setInputParameters() {
		inputParams = new InputParameters();
    	
    	inputParams.dbServer  	= CommandLineArguments.getDBServer();
      
    	inputParams.inputList	= CommandLineArguments.getInpList();
        inputParams.imageFile	= CommandLineArguments.getInpImage();
        inputParams.inputFile   = CommandLineArguments.getInpFile();
        inputParams.outFolder   = CommandLineArguments.getOutFoldername();
      
        inputParams.execTitle 	= CommandLineArguments.getExecutionTitle();
        inputParams.execID    	= CommandLineArguments.getExecutionID();
        inputParams.execType  	= CommandLineArguments.getExecutionType();
        inputParams.execComp  	= CommandLineArguments.getExecutionComputation();
        inputParams.colorVal 	= CommandLineArguments.getColor();
        inputParams.studyID   	= CommandLineArguments.getStudyID();
        inputParams.batchID   	= CommandLineArguments.getBatchID();
        inputParams.tagID 		= CommandLineArguments.getTagID();
        
        inputParams.caseID		= CommandLineArguments.getCaseID();
        inputParams.subjectID	= CommandLineArguments.getSubjectID();
        
        inputParams.isQuip = CommandLineArguments.isQuip();
        
        inputParams.doNormalize   = CommandLineArguments.isNormalize();
        inputParams.selfNormalize = CommandLineArguments.isSelfNormalize();
        inputParams.getFromDB 	  = CommandLineArguments.isGetFromDB();
      
        inputParams.width		= CommandLineArguments.getWidth();
	    inputParams.height		= CommandLineArguments.getHeight();
        inputParams.shiftX		= CommandLineArguments.getShiftX();
	    inputParams.shiftY		= CommandLineArguments.getShiftY();
	 
        inputParams.doSimplify  = CommandLineArguments.isSimplify();
        inputParams.minSize		= CommandLineArguments.getMinSize();
        inputParams.maxSize		= CommandLineArguments.getMaxSize();
        
        inputParams.nameSpace 	= CommandLineArguments.getNamespace();
        
        // Handle writing to output file instead of database
        inputParams.outFileWriter = null;
        if (inputParams.outFolder!=null) {
        	if (inputParams.inputFile==null) {
        		System.err.println("Error: input file name is missing.");
        		return false;
        	} else { 
        		try {
        			String fileName = (new File(inputParams.inputFile)).getName();
        			inputParams.outFileWriter = new FileWriter(inputParams.outFolder + "/" + fileName + ".json");
        		} catch (Exception e) {
        			System.err.println(e.getClass().getName() + ": " + e.getMessage());
        			return false;
        		}
        	}
        }
	        
        return true;
	}
	
	public static void handleFile(ProcessFile process, int numThreads) {
		try {
			if (!setInputParameters()) 
				return;

			if (inputParams.inputList==null || inputParams.outFileWriter!=null) 
				numThreads = 1;  // if a single file or output to file, use one thread
		
			ResultsDatabase[] segDB = setupDatabaseConnections(numThreads,inputParams.dbServer);
			ProcessFileThread[] procFile = new ProcessFileThread[numThreads];

			IterateInputData iter = new IterateInputData();
			iter.setIterator(inputParams);
			
			InputListArray fileList = new InputListArray();
			if (fileList.setListArray(inputParams)==false) 
				return; 
			ArrayList<FileParameters> fileArray = fileList.getListArray();
			
			int thread_id = 0; 
			int fi = 0;			
			while (fi<fileArray.size()) {
				FileParameters fileParams = fileArray.get(fi);
				if (inputParams.isQuip==false) {
					System.out.println("Processing[" + fi + "]: " 
							+ " Filename: " + fileParams.getFileName() 
							+ " SubjectID: " + fileParams.getSubjectId() 
							+ " CaseID: " + fileParams.getCaseId());
				} else {
					System.out.println("Processing[" + fi + "]: " 
							+ " Filename: " + fileParams.getFileName());
				}

				int check_done = 0;
				while (check_done == 0) {
					if (procFile[thread_id] == null || !procFile[thread_id].isAlive()) {
						if (process instanceof ProcessTSVFeaturePolygonFile) {
							process = new ProcessTSVFeaturePolygonFile(fileParams, inputParams, segDB[thread_id]); 
						} else if (process instanceof ProcessCSVFeaturePolygonFile) {
							process = new ProcessCSVFeaturePolygonFile(fileParams, inputParams, segDB[thread_id]);
						} else if (process instanceof ProcessBinaryMaskFile) {
							process = new ProcessBinaryMaskFile(fileParams, inputParams, segDB[thread_id]);
						} else if (process instanceof ProcessQuipMaskFile) {
							process = new ProcessQuipMaskFile(fileParams, inputParams, segDB[thread_id]);
						} else if (process instanceof ProcessQuipCSVFile) {
							process = new ProcessQuipCSVFile(fileParams, inputParams, segDB[thread_id]);
						}
						procFile[thread_id] = new ProcessFileThread(process);
						check_done = 1;
					}
					thread_id = (thread_id + 1) % numThreads;
					Thread.sleep(500);
				}
				fi++;
			}

			// Finishing Threads
			loop(procFile,numThreads);
			
			if (inputParams.outFileWriter!=null)
				inputParams.outFileWriter.close();
			
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}
    
	public static void handleAperioXMLFile(int numThreads) {
		String outFolder = CommandLineArguments.getOutFoldername();
		if (outFolder == null) outFolder = "./";

		String inpFile = CommandLineArguments.getInpFile();
		if (inpFile == null) {
			System.err.println("Need an input file.");
			return;
		}

		String caseID = CommandLineArguments.getCaseID();
		if (caseID == null) caseID = "NO-CASE-ID";

		boolean normalize = CommandLineArguments.isNormalize();
		int img_width = CommandLineArguments.getWidth();
		int img_height = CommandLineArguments.getHeight();
		if (normalize && (img_width == -1 || img_height == -1)) {
			System.err
					.println("Please enter valid image width and height (>0) values.");
			return;
		}
		int shiftX = CommandLineArguments.getShiftX();
		int shiftY = CommandLineArguments.getShiftY();

		String execId      = CommandLineArguments.getExecutionID();
		String execType    = CommandLineArguments.getExecutionType();
		String computation = "markup";
		String execName    = CommandLineArguments.getExecutionTitle();
		String colorVal    = CommandLineArguments.getColor();
		String studyId     = CommandLineArguments.getStudyID();
        String batchId     = CommandLineArguments.getBatchID();
        String tagId       = CommandLineArguments.getTagID();

		numThreads = 1;
		try {
			ProcessFileThread[] procFile = new ProcessFileThread[numThreads];
			AnalysisExecutionMetadata executionMetadata = new AnalysisExecutionMetadata(
					execId, studyId, batchId, tagId, execName, execType, computation);

			int thread_id = 0;
			int fi = 0;
			System.out.println("Processing [" + fi + "]: " + inpFile);
			String fileName = (new File(inpFile)).getName();
			FileWriter outFileWriter = new FileWriter(outFolder + "/" + fileName
					+ ".json");

			int check_done = 0;
			while (check_done == 0) {
				if (procFile[thread_id] == null
						|| !procFile[thread_id].isAlive()) {
					ProcessAperioXMLFile aperioXMLFile;
					aperioXMLFile = new ProcessAperioXMLFile(inpFile,
							executionMetadata, img_width, img_height, shiftX,
							shiftY, normalize, caseID, outFileWriter);

					aperioXMLFile.setColor(colorVal);
					aperioXMLFile.setCaseID(caseID);
					aperioXMLFile.setSubjectID(caseID);

					procFile[thread_id] = new ProcessFileThread(aperioXMLFile);
					check_done = 1;
				}
			}

			// Finishing Threads
			loop(procFile,numThreads);
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}

    public static void loop(ProcessFileThread[] procFile, int numThreads) {
        System.out.println("Finishing threads.");

        int[] check_thread = new int[numThreads];

        for (int i = 0; i < numThreads; i++)
            check_thread[i] = 0;

        int all_done = 0;
        int thread_id = 0;

        while (all_done < numThreads) {
            if (procFile[thread_id] == null && check_thread[thread_id] == 0) {
                check_thread[thread_id] = 1;
                all_done++;
            }
            if (procFile[thread_id] != null && check_thread[thread_id] == 0
                    && !procFile[thread_id].isAlive()) {
                check_thread[thread_id] = 1;
                all_done++;
            }
            thread_id = (thread_id + 1) % numThreads;
            try {
                Thread.sleep(500);
            } catch (java.lang.InterruptedException insomnia) {
                insomnia.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {

    	CommandLineArguments.initCommandLineOptions();
    	System.out.println("Parsing the command line arguments\n");
    	if (!CommandLineArguments.parseCommandLineArgs(args)) {
    		CommandLineArguments.printUsage();
    		return;
    	}

    	int numCores   = Runtime.getRuntime().availableProcessors();
    	int numThreads = (numCores * 6)/10;
    	if (numThreads==0) numThreads = 1;

    	try {
    		if (CommandLineArguments.isTSV()) {
    			handleFile(new ProcessTSVFeaturePolygonFile(),numThreads);
    		} else if (CommandLineArguments.isCSV()) {
    			if (CommandLineArguments.isQuip()) {
    	   			handleFile(new ProcessQuipCSVFile(),numThreads);
    			} else {
    				handleFile(new ProcessCSVFeaturePolygonFile(),numThreads);
    			}
    		} else if (CommandLineArguments.isMaskFile()) {
    			if (CommandLineArguments.isQuip()) {
    				handleFile(new ProcessQuipMaskFile(),numThreads);
    			} else {
    				handleFile(new ProcessBinaryMaskFile(),numThreads);
    			}
    		} else if (CommandLineArguments.isAperio()) {
    			handleAperioXMLFile(numThreads);
    		} else {
    			System.err.println("Unknown input type.");
    		}
    	} catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }
}
