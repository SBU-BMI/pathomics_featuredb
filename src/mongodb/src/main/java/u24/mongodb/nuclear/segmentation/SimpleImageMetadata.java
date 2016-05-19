package u24.mongodb.nuclear.segmentation;

import java.util.UUID;

import com.mongodb.BasicDBObject;

/**
 * Used internally for maintaining the image metadata.
 */
public class SimpleImageMetadata {

    private String identifier;
    private String subjectid;
    private String caseid;
    private double mpp_x;
    private double mpp_y;
    private double width;
    private double height;
    private double objective;
    private String cancertype;

    private BasicDBObject image_doc;

    public SimpleImageMetadata() {
        String imageUUID = UUID.randomUUID().toString();
        image_doc = new BasicDBObject();
        image_doc.put("uuid", imageUUID);

        this.identifier = null;
        this.caseid = null;
        this.subjectid = null;

        this.mpp_x = -1.0;
        this.mpp_y = -1.0;
        this.width = 0.0;
        this.height = 0.0;
        this.objective = 0.0;
        this.cancertype = "undefined";
    }

    public BasicDBObject getMetadataDoc() {
        return image_doc;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
        image_doc.put("identifier", this.identifier);
    }

    public String getSubjectid() {
        return this.subjectid;
    }

    public void setSubjectid(String subjectid) {
        this.subjectid = subjectid;
        image_doc.put("subject_id", this.subjectid);
    }

    public String getCaseid() {
        return caseid;
    }

    public void setCaseid(String caseid) {
        this.caseid = caseid;
        image_doc.put("case_id", this.caseid);
    }

    public void setMpp_x(double mpp_x) {
        this.mpp_x = mpp_x;
        image_doc.put("mpp_x", this.mpp_x);
    }

    public void setMpp_y(double mpp_y) {
        this.mpp_y = mpp_y;
        image_doc.put("mpp_y", this.mpp_y);
    }

    public void setWidth(double width) {
        this.width = width;
        image_doc.put("width", this.width);
    }

    public void setHeight(double height) {
        this.height = height;
        image_doc.put("height", this.height);
    }

    public void setObjective(double objective) {
        this.objective = objective;
        image_doc.put("objective", this.objective);
    }

    public void setCancertype(String cancertype) {
        this.cancertype = cancertype;
        image_doc.put("cancer_type", this.cancertype);
    }

}
