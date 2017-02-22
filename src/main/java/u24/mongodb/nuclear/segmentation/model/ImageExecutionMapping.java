package u24.mongodb.nuclear.segmentation.model;

import u24.mongodb.nuclear.segmentation.database.ResultsDatabase;

import org.bson.Document;

import com.mongodb.client.FindIterable;

/**
 * Maps case ID to execution ID.
 */
public class ImageExecutionMapping {

    private Document metadataDoc;
  
    private String execId;
    private String stdyId;
    private String caseId;
    private String subjectId;
    
    public ImageExecutionMapping() { }
  
    public ImageExecutionMapping(AnalysisExecutionMetadata execMeta,
            SimpleImageMetadata imgMeta, String color) {
    	metadataDoc = new Document();
        metadataDoc.put("color", color);
        metadataDoc.put("title", execMeta.getTitle());

        Document imgmeta_doc = new Document();
        imgmeta_doc.put("subject_id", imgMeta.getSubjectid());
        imgmeta_doc.put("case_id", imgMeta.getCaseid());

        Document provenance_doc = new Document();
        provenance_doc.put("analysis_execution_id",
                execMeta.getIdentifier());
        provenance_doc.put("study_id", execMeta.getStudyId());
        provenance_doc.put("type", execMeta.getSource());

        metadataDoc.put("image", imgmeta_doc);
        metadataDoc.put("provenance", provenance_doc);
        
        execId = execMeta.getIdentifier();
        stdyId = execMeta.getStudyId();
        caseId = imgMeta.getCaseid();
        subjectId = imgMeta.getSubjectid();
    }

 	public ImageExecutionMapping(AnalysisExecutionMetadata execMeta,
            SimpleImageMetadata imgMeta, String color, Document quipMeta) {
    	metadataDoc = new Document();
        metadataDoc.put("color", color);
        metadataDoc.put("title", execMeta.getTitle());

        Document imgmeta_doc = new Document();
        imgmeta_doc.put("subject_id", imgMeta.getSubjectid());
        imgmeta_doc.put("case_id", imgMeta.getCaseid());

        Document provenance_doc = new Document();
        provenance_doc.put("analysis_execution_id",
                execMeta.getIdentifier());
        provenance_doc.put("study_id", execMeta.getStudyId());
        provenance_doc.put("type", execMeta.getSource());
		provenance_doc.put("algorithm_params",quipMeta);

        metadataDoc.put("image", imgmeta_doc);
        metadataDoc.put("provenance", provenance_doc);
        
        execId = execMeta.getIdentifier();
        stdyId = execMeta.getStudyId();
        caseId = imgMeta.getCaseid();
        subjectId = imgMeta.getSubjectid();
    }

    /**
     * Query the 'metadata' collection.
     */
    public boolean checkExists(ResultsDatabase db, String executionIdentifier, String studyId, String imageCaseID) {
        Document imgQuery = new Document();
        imgQuery.put("image.case_id", imageCaseID);
        imgQuery.put("provenance.analysis_execution_id",
                executionIdentifier);
        imgQuery.put("provenance.study_id", studyId);
        Document cursor = db.submitAnalysisExecutionMappingQuery(imgQuery);

        return   cursor!=null;  
   }
    
    public boolean checkExists(ResultsDatabase db) {
    	Document imgQuery = new Document();
        imgQuery.put("image.case_id", caseId);
        imgQuery.put("image.subject_id", subjectId);
        imgQuery.put("provenance.analysis_execution_id",execId);
        imgQuery.put("provenance.study_id", stdyId);
        Document cursor = db.submitAnalysisExecutionMappingQuery(imgQuery);

        return cursor!=null;	
    }
    
    /**
     * GET metadataDoc.
     */
    public Document getMetadataDoc() {
        return metadataDoc;
    }

    /**
     * SET metadataDoc.
     */
    public void setMetadataDoc(AnalysisExecutionMetadata execMeta,
                               SimpleImageMetadata imgMeta, String color) {
        metadataDoc = new Document();
        metadataDoc.put("color", color);
        metadataDoc.put("title", execMeta.getTitle());

        Document imgmeta_doc = new Document();
        imgmeta_doc.put("subject_id", imgMeta.getSubjectid());
        imgmeta_doc.put("case_id", imgMeta.getCaseid());

        Document provenance_doc = new Document();
        provenance_doc.put("analysis_execution_id",
                execMeta.getIdentifier());
        provenance_doc.put("study_id", execMeta.getStudyId());
        provenance_doc.put("type", execMeta.getSource());

        metadataDoc.put("image", imgmeta_doc);
        metadataDoc.put("provenance", provenance_doc);
    }
    
    public void setQuipMetadataDoc(Document quipMeta) {
    	metadataDoc.append("provenance", quipMeta);
    }
}
