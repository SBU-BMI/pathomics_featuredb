package u24.mongodb.nuclear.segmentation;

import com.mongodb.BasicDBObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Create a Study, and describe it.
 * eg. "We're doing a study on LGGs"
 */
public class AnalysisStudyMetadata {
    private String uuid;
    private String name;
    private String ownerFirstName;
    private String ownerLastName;
    private String description;
    private Date startDate;
    private Date endDate;
    private String userDefinedIdentifier;
    private BasicDBObject user_doc;

    private static final String _analysisDocType = "study_instance";


    public AnalysisStudyMetadata(String studyName, String studyId, String studyOwnerFirstName,
                                 String studyOwnerLastName, String studyStartDate,
                                 String studyEndDate, String studyDescription) {

        this.name = studyName;
        this.ownerFirstName = studyOwnerFirstName;
        this.ownerLastName = studyOwnerLastName;

        DateFormat outputFormat = new SimpleDateFormat("MM-dd-yyyy");
        DateFormat inputFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

        if (studyEndDate.isEmpty()) {
            studyEndDate = studyStartDate;
        }

        Date temp;

        try {
            temp = inputFormat.parse(studyStartDate);
            String fmt = outputFormat.format(temp);
            this.startDate = outputFormat.parse(fmt);

            temp = inputFormat.parse(studyEndDate);
            fmt = outputFormat.format(temp);
            this.endDate = outputFormat.parse(fmt);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                String fmt = outputFormat.format(new Date());
                this.startDate = outputFormat.parse(fmt);
                this.endDate = outputFormat.parse(fmt);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        this.description = studyDescription;

        /* 
        this.userDefinedIdentifier = studyOwnerFirstName
                .replaceAll("\\s+", ".").toLowerCase()
                + ":"
                + studyOwnerLastName.replaceAll("\\s+", ".").toLowerCase()
                + ":"
                + studyName.replaceAll("\\s+", ".").toLowerCase()
                + ":"
                + studyStartDate.replaceAll("\\s+", "-").toLowerCase();
                */
        
        this.userDefinedIdentifier = studyId;

        uuid = UUID.randomUUID().toString();

        user_doc = null;
    }

    /**
     * Get metadata document.
     */
    public BasicDBObject getMetadataDoc() {
        BasicDBObject required_doc = new BasicDBObject();
        required_doc.put("name", name);
        required_doc.put("owner_first_name", ownerFirstName);
        required_doc.put("owner_last_name", ownerLastName);
        required_doc.put("start_date", startDate);
        required_doc.put("end_date", endDate);
        required_doc.put("description", description);

        BasicDBObject combined_doc = new BasicDBObject("metadata", required_doc);
        if (user_doc != null)
            combined_doc.put("userdefined", user_doc);

        BasicDBObject study_doc = new BasicDBObject();
        study_doc.put("uuid", uuid);
        study_doc.put("type", _analysisDocType);
        study_doc.put("identifier", userDefinedIdentifier);
        study_doc.put("study", combined_doc);

        return study_doc;
    }

    public String getIdentifier() {
        return userDefinedIdentifier;
    }

}
