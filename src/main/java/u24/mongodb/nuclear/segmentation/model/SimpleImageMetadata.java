package u24.mongodb.nuclear.segmentation.model;

import java.util.UUID;
import org.bson.Document;
import u24.mongodb.nuclear.segmentation.database.ResultsDatabase;

/**
 * Used internally for maintaining the image metadata.
 */
public class SimpleImageMetadata {

	private String identifier;
	private String subject_id;
	private String case_id;
	private double mpp_x;
	private double mpp_y;
	private double image_width;
	private double image_height;
	private String cancer_type;

	private Document image_doc;

	public SimpleImageMetadata() {
		String imageUUID = UUID.randomUUID().toString();
		image_doc = new Document();
		image_doc.put("uuid", imageUUID);

		this.identifier = null;
		this.case_id = null;
		this.subject_id = null;

		this.mpp_x = -1.0;
		this.mpp_y = -1.0;
		this.image_width = 0.0;
		this.image_height = 0.0;
		this.cancer_type = "undefined";
	}

	public int setFromDB(String caseId, String subjectId, ResultsDatabase segDB) {
		// Query and retrieve image metadata values
		Document imgQuery = new Document();
		imgQuery.put("case_id", caseId);
		imgQuery.put("subject_id", subjectId);

		this.case_id = caseId;
		this.subject_id = subjectId;

		Document qryResult = segDB.getImagesCollection().find(imgQuery).first();
		if (qryResult==null) return 1;

		if (qryResult.get("mpp_x")!=null) 
			this.mpp_x = Double.parseDouble(qryResult.get("mpp_x").toString());
		else {
			System.err.println("Cannot find mpp_x in image metadata.");
			return 1;
		}
		if (qryResult.get("mpp_y")!=null)
			this.mpp_y = Double.parseDouble(qryResult.get("mpp_y").toString());
		else {
			System.err.println("Cannot find mpp_y in image metadata.");
			return 1;
		}
		if (qryResult.get("width")!=null)
			this.image_width = Double.parseDouble(qryResult.get("width").toString());
		else {
			System.err.println("Cannot find the width field in image metadata.");
			return 1;
		}
		if (qryResult.get("height")!=null)
			this.image_height = Double.parseDouble(qryResult.get("height").toString());
		else {
			System.err.println("Cannot find the height field in image metadata.");
			return 1;
		}
		if (qryResult.get("cancer_type")!=null)
			this.cancer_type = qryResult.get("cancer_type").toString(); 	
		return 0;
	}

	public Document getMetadataDoc() {
		return image_doc;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
		image_doc.put("identifier", this.identifier);
	}

	public String getSubjectid() {
		return this.subject_id;
	}

	public void setSubjectid(String subjectid) {
		this.subject_id = subjectid;
		image_doc.put("subject_id", this.subject_id);
	}

	public String getCaseid() {
		return case_id;
	}

	public void setCaseid(String caseid) {
		this.case_id = caseid;
		image_doc.put("case_id", this.case_id);
	}

	public void setMpp_x(double mpp_x) {
		this.mpp_x = mpp_x;
		image_doc.put("mpp_x", this.mpp_x);
	}
	
	public double getMpp_x() {
		return this.mpp_x;
	}

	public void setMpp_y(double mpp_y) {
		this.mpp_y = mpp_y;
		image_doc.put("mpp_y", this.mpp_y);
	}
	
	public double getMpp_y() {
		return this.mpp_y;
	}

	public void setWidth(double width) {
		this.image_width = width;
		image_doc.put("width", this.image_width);
	}
	
	public double getWidth() {
		return this.image_width;
	}

	public void setHeight(double height) {
		this.image_height = height;
		image_doc.put("height", this.image_height);
	}
	
	public double getHeight() {
		return this.image_height;
	}

	public void setCancertype(String cancertype) {
		this.cancer_type = cancertype;
		image_doc.put("cancer_type", this.cancer_type);
	}
	
	public String getCancertype() {
		return this.cancer_type;
	}

}
