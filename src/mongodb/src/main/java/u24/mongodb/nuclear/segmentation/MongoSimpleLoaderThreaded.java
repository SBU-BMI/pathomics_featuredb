package u24.mongodb.nuclear.segmentation;

import java.io.File;
import java.io.FileWriter;

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
	
	private static InputParameters setInputParameters() {
		InputParameters inputParams = new InputParameters();
    	
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
        		return null;
        	} else { 
        		try {
        			String fileName = (new File(inputParams.inputFile)).getName();
        			inputParams.outFileWriter = new FileWriter(inputParams.outFolder + "/" + fileName + ".json");
        		} catch (Exception e) {
        			System.err.println(e.getClass().getName() + ": " + e.getMessage());
        			return null;
        		}
        	}
        }
	        
        return inputParams;
	}
	
	public static void handleFile(ProcessFile process, int numThreads) {
		try {
			InputParameters inputParams = setInputParameters();

			if (inputParams.inputList==null || inputParams.outFileWriter!=null) 
				numThreads = 1;  // if a single file or output to file, use one thread
		
			ResultsDatabase[] segDB = setupDatabaseConnections(numThreads,inputParams.dbServer);
			ProcessFileThread[] procFile = new ProcessFileThread[numThreads];

			AnalysisExecutionMetadata execMeta = new AnalysisExecutionMetadata(inputParams.execID, 
					inputParams.studyID, inputParams.batchID,  inputParams.tagID, inputParams.execTitle, 
					inputParams.execType, inputParams.execComp);

			IterateInputData iter = new IterateInputData();
			iter.setIterator(inputParams);
			
			int thread_id = 0; 
			int fi = 0;			
			while (iter.hasNext()) {
				FileParameters fileParams = new FileParameters(); 
				String[] currLine = iter.next().split(",");
				if (currLine.length!=5) {
					System.err.println("Missing parameters in input file list [studyId,caseId/imageId,fileName,shiftX,shiftY].");
					return;
				} else {
					fileParams.setSubjectId(currLine[0]);
					fileParams.setCaseId(currLine[1]);
					fileParams.setFileName(currLine[2]);
					fileParams.setShiftX(Integer.parseInt(currLine[3]));
					fileParams.setShiftY(Integer.parseInt(currLine[4]));
				}

				System.out.println("Processing[" + fi + "]: " 
						+ " Filename: " + fileParams.getFileName() 
						+ " SubjectID: " + fileParams.getSubjectId() 
						+ " CaseID: " + fileParams.getCaseId());

				int check_done = 0;
				while (check_done == 0) {
					if (procFile[thread_id] == null || !procFile[thread_id].isAlive()) {
						if (process instanceof ProcessTSVQuipFile) {
							process = new ProcessTSVQuipFile(fileParams, execMeta, inputParams, segDB[thread_id]); 
						} else if (process instanceof ProcessCSVFeaturePolygonFile) {
							process = new ProcessCSVFeaturePolygonFile(fileParams, execMeta, inputParams, segDB[thread_id]);
						} else if (process instanceof ProcessBinaryMaskFile) {
							process = new ProcessBinaryMaskFile(fileParams, execMeta, inputParams, segDB[thread_id]);
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
        String batchId  = CommandLineArguments.getBatchID();
        String tagId  = CommandLineArguments.getTagID();

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
    			handleFile(new ProcessTSVQuipFile(),numThreads);
    		} else if (CommandLineArguments.isCSV()) {
    			handleFile(new ProcessCSVFeaturePolygonFile(),numThreads);
    		} else if (CommandLineArguments.isMaskFile()) {
    			handleFile(new ProcessBinaryMaskFile(),numThreads);
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
