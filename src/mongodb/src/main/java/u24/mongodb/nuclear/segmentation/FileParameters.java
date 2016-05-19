package u24.mongodb.nuclear.segmentation;

public class FileParameters {
	private String subjectId = null;
	private String caseId 	 = null;
	private String fileName  = null;
	private int shiftX = 0;
	private int shiftY = 0;
	
	public FileParameters(String subjectId, String caseId, String fileName, int shiftX, int shiftY) {
		this.subjectId = subjectId;
		this.caseId = caseId;
		this.fileName = fileName;
		this.shiftX = shiftX;
		this.shiftY = shiftY;
	}
	
	public FileParameters() {
		this.subjectId = null; 
		this.caseId = null;
		this.fileName = null;
		this.shiftX = 0;
		this.shiftY = 0;
	}

	public String getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(String subjectId) {
		this.subjectId = subjectId;
	}

	public String getCaseId() {
		return caseId;
	}

	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getShiftX() {
		return shiftX;
	}

	public void setShiftX(int shiftX) {
		this.shiftX = shiftX;
	}

	public int getShiftY() {
		return shiftY;
	}

	public void setShiftY(int shiftY) {
		this.shiftY = shiftY;
	}
	
}
