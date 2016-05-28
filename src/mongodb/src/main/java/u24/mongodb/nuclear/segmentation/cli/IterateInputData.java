package u24.mongodb.nuclear.segmentation.cli;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;

/**
 * This iterates over the list of <subjectID,caseID,inputFile>
 */
class InputFileListIterator implements Iterator<String> {
	BufferedReader reader;

	InputFileListIterator(BufferedReader myReader) {
		reader = myReader;
	}

	@Override
	public boolean hasNext() {
		try {
			return reader.ready();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
		return false;
	}

	@Override
	public String next() {
		try {
			String currLine = reader.readLine();
			if (currLine != null) {
				return currLine;
			} else {
				return null;
			}
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
		return null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove not supported!");
	}

}

public class IterateInputData implements Iterator<String> {
	Iterator<String> iter;
	String singleFileData;
	boolean fileNotReturned;

	public IterateInputData() {
		singleFileData = null;
		iter = null;
		fileNotReturned = true;
	}
	
	public boolean setIterator(InputParameters inpParams) {
		if (inpParams.inputList!=null) { 
			setFileList(inpParams.inputList);
		} else if (inpParams.inputFile!=null) {
			setSingleFile(inpParams.subjectID,inpParams.caseID,inpParams.inputFile,inpParams.shiftX,inpParams.shiftY);
		} else {
			System.err.println("Input file or list should be defined.");
			return false;
		}
		return true;
	}

	public void setSingleFile(String subjectID, String caseID, String singleFile) {
		singleFileData = subjectID + "," + caseID + "," + singleFile;
		fileNotReturned = true;
	}
	
	public void setSingleFile(String subjectID, String caseID, String singleFile, int shiftX, int shiftY) {
		singleFileData = subjectID + "," + caseID + "," + singleFile + "," + shiftX + "," + shiftY;
		fileNotReturned = true;
	}

	public void setFileList(String inpData) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(inpData));
			iter = new InputFileListIterator(br);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean hasNext() {
		if (singleFileData!=null)
			return fileNotReturned;
		else
			return iter.hasNext();
	}

	@Override
	public String next() {
		if (singleFileData!=null) {
			fileNotReturned = false;
			return singleFileData;
		} else {
			return iter.next();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove not supported!");
	}

}
