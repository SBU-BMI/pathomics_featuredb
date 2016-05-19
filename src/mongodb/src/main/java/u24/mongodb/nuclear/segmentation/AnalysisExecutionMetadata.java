package u24.mongodb.nuclear.segmentation;

import com.mongodb.BasicDBObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Within a study, you may have several Analysis Executions.
 * eg. Yi's version 2 algorithm, etc.
 * We say - Who ran it, what type, what's the date, username, and description.
 */
public class AnalysisExecutionMetadata {
    private String uuid;
    private String identifier;
    private String studyId;
    private String batchId;
    private String tagId;
    private String title;
    private String source;
    private String computation;

    private BasicDBObject user_doc;

    private Date fullDate;
    private String userName;
    private String executionDescription;

    private AnalysisStudyMetadata studyMetadata;
    private PipelineMetadata analysisPipeline;

    private static final String _executionDocType = "execution_instance";

    public AnalysisExecutionMetadata(String executionIdentifier,
    								 String studyIdentifier,
									 String batchIdentifier,
									 String tagIdentifier,
                                     String executionTitle, String source, String computation) {
        this.identifier = executionIdentifier;
        this.studyId = studyIdentifier;
		this.batchId = batchIdentifier;
		this.tagId = tagIdentifier;
        this.title = executionTitle;
        this.source = source;
        this.computation = computation;
        this.uuid = UUID.randomUUID().toString();

        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        Date date = new Date();
        String d = "";

        try {
            d = sdf.format(date);
            this.fullDate = sdf.parse(d);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.userName = System.getProperty("user.name");
        if (this.userName==null) this.userName = "nouser";
        this.executionDescription = "defaultexecution";
        this.analysisPipeline = new PipelineMetadata("dummyPipeline");

        this.user_doc = null;

        String studyName = "TCGA U24";
        String studyOwnerFirstName = userName;
        String studyOwnerLastName = "";
        String studyStartDate = d;
        String studyEndDate = d;
        String studyDescription = userName + " study " + studyStartDate;

        this.studyMetadata = new AnalysisStudyMetadata(studyName, studyId, studyOwnerFirstName, studyOwnerLastName,
                studyStartDate, studyEndDate, studyDescription);
    }

    private String getStudyIdentifierValue() {
        if (studyMetadata == null)
            return null;
        else if (studyMetadata.getIdentifier() == null)
            return null;
        else
            return studyMetadata.getIdentifier();
    }

    /**
     * Execution Identifier ultimately comes from MongoSimpleLoaderThreaded.
     */
    public String getIdentifier() {
        return this.identifier;
    }
    
    public String getStudyId() {
    	return studyId;
    }

    public String getBatchId() {
    	return batchId;
    }

    public String getTagId() {
    	return tagId;
    }

    public String getSource() {
        // ex: "computer"
        return source;
    }

    public String getComputation() {
        // ex: "segmentation"
        return computation;
    }

    public String getTitle() {
        return title;
    }

    public AnalysisStudyMetadata getStudyIdentifier() {
        return studyMetadata;
    }

    public BasicDBObject getMetadataDoc() {
        BasicDBObject required_doc = new BasicDBObject();
        required_doc.put("name", this.title);
        required_doc.put("type", this.source);
        required_doc.put("date", this.fullDate);
        required_doc.put("user_name", this.userName);
        required_doc.put("description", this.executionDescription);

        BasicDBObject combined_doc = new BasicDBObject("metadata", required_doc);
        combined_doc.append("pipeline", this.analysisPipeline.getMetadataDoc());
        if (user_doc != null)
            combined_doc.append("userdefined", user_doc);

        BasicDBObject execution_doc = new BasicDBObject();
        execution_doc.put("uuid", uuid);
        execution_doc.put("study_id", getStudyIdentifierValue());
        execution_doc.put("identifier", this.identifier);
        execution_doc.put("type", _executionDocType);
        execution_doc.append("execution", combined_doc);

        return execution_doc;
    }
}
