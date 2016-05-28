package u24.mongodb.nuclear.segmentation.cli;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class InputListArray {
	private ArrayList<FileParameters> fileList;
	
	private final static Charset ENCODING = StandardCharsets.UTF_8;
	
	public InputListArray() {
		fileList = new ArrayList<FileParameters>();
	}
	
	public boolean setListArray(InputParameters inpParams) {
		try {
			if (inpParams.isQuip) {
				if (inpParams.inputList!=null) {
					Path path = Paths.get(inpParams.inputList);
					BufferedReader br = Files.newBufferedReader(path, ENCODING);
					String line;
					while ((line = br.readLine())!=null) {
							FileParameters fileParams = new FileParameters();
							fileParams.setSubjectId("quip");
							fileParams.setCaseId("quip");
							fileParams.setFileName(line);
							fileParams.setShiftX(0);
							fileParams.setShiftY(0);
							fileList.add(fileParams);
					}
				} else if (inpParams.inputFile!=null) {
					FileParameters fileParams = new FileParameters();
					fileParams.setSubjectId("quip");
					fileParams.setCaseId("quip");
					fileParams.setFileName("quip");
					fileParams.setShiftX(0);
					fileParams.setShiftY(0);
					fileList.add(fileParams);
				}
			} else {
				if (inpParams.inputList!=null) {
					Path path = Paths.get(inpParams.inputList);
					BufferedReader br = Files.newBufferedReader(path, ENCODING);
					String line;
					while ((line = br.readLine())!=null) {
						String[] currLine = line.split(",");
						if (currLine.length!=5) {
							System.err.println("Missing parameters in input file list [studyId,caseId/imageId,fileName,shiftX,shiftY].");
							return false;
						} else {
							FileParameters fileParams = new FileParameters();
							fileParams.setSubjectId(currLine[0]);
							fileParams.setCaseId(currLine[1]);
							fileParams.setFileName(currLine[2]);
							fileParams.setShiftX(Integer.parseInt(currLine[3]));
							fileParams.setShiftY(Integer.parseInt(currLine[4]));
							fileList.add(fileParams);
						}
					} 
				} else if (inpParams.inputFile!=null) {
					FileParameters fileParams = new FileParameters();
					fileParams.setSubjectId(inpParams.subjectID);
					fileParams.setCaseId(inpParams.caseID);
					fileParams.setFileName(inpParams.inputFile);
					fileParams.setShiftX(inpParams.shiftX);
					fileParams.setShiftY(inpParams.shiftY);
					fileList.add(fileParams);
				}
			}
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
		return true;
	}
	
	public ArrayList<FileParameters> getListArray() {
		return fileList;
	}
}
