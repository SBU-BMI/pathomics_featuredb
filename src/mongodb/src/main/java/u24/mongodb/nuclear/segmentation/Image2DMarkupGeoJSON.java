package u24.mongodb.nuclear.segmentation;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The document creation process.
 * Database is GeoJSON compliant.
 */
public class Image2DMarkupGeoJSON {

    private BasicDBObject objDoc;
    private BasicDBObject propDoc;
    private BasicDBObject featuresDoc;
    private BasicDBObject provenanceDoc;
    private static final String VERSION_NUMBER = "1.3";
    private static final String TYPE = "type";
   
    public Image2DMarkupGeoJSON() {
        objDoc = new BasicDBObject();
        propDoc = new BasicDBObject();
        provenanceDoc = new BasicDBObject();
        featuresDoc = new BasicDBObject();

        // add self parent id
        objDoc.put(TYPE, "Feature");
        objDoc.put("parent_id", "self");
        objDoc.put("randval", new Random().nextFloat());
        objDoc.append("creation_date", new Date());
        objDoc.put("object_type", "nucleus");

    }

    /**
     * Create the document subsection that corresponds to the segmentation result.
     * Creates the section of the JSON that contains the segmentation data, polygon information, bounding box, location,
     * and coordinates.
     * 
     * Footprint = Area. Required for caMicroscope.
     */
    public int setMarkup(double min_x,
                         double min_y, double max_x, double max_y, String objType,
                         boolean coordinatesNormalized, BasicDBList objPointsList) {

        try {
            objDoc.put("x", min_x);
            objDoc.put("y", min_y);
            objDoc.put("normalized", Boolean.toString(coordinatesNormalized));

            // Add bounding box
            BasicDBList bboxList = new BasicDBList();
            bboxList.add(min_x);
            bboxList.add(min_y);
            bboxList.add(max_x);
            bboxList.add(max_y);
            objDoc.put("bbox", bboxList);

            // Add geometry object
            BasicDBObject geomDoc = new BasicDBObject();
            // objType = "Polygon", etc.
            geomDoc.put(TYPE, objType);

            // Add arrays of arrays of points
            BasicDBList objList = new BasicDBList();
            objList.add(objPointsList);
            geomDoc.put("coordinates", objList);

            // Add geometry object to the main document
            objDoc.put("geometry", geomDoc);
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return 0;
    }


    /**
     * Set Scalar Features.
     */
    public int setScalarFeatures(HashMap<String, HashMap<String, Object>> ns_features) {

        try {
            BasicDBList scalar_features_list = new BasicDBList();

            for (Map.Entry<String, HashMap<String, Object>> nsf : ns_features.entrySet()) {

                String ns = nsf.getKey();
                HashMap<String, Object> features = nsf.getValue();

                BasicDBList nvList = new BasicDBList();

                for (Map.Entry<String, Object> entry : features.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (!key.equalsIgnoreCase("namespace")) {
                        nvList.add(new BasicDBObject().append("name", key).append("value", value));
                    }

                }

                featuresDoc = new BasicDBObject();
                featuresDoc.put("ns", ns);
                featuresDoc.put("nv", nvList);
                scalar_features_list.add(featuresDoc);

            }
            propDoc.put("scalar_features", scalar_features_list);

            objDoc.put("properties", propDoc);
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return 0;
    }


    /**
     * Set Image Metadata.
     */
    public int setImageMetadata(SimpleImageMetadata imgMeta) {
        try {
            BasicDBObject imgDoc = new BasicDBObject();
            imgDoc.put("case_id", imgMeta.getCaseid());
            imgDoc.put("subject_id", imgMeta.getSubjectid());
            provenanceDoc.put("image", imgDoc);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    /**
     * Set Analysis Metadata.
     */
    public int setAnalysisMetadata(AnalysisExecutionMetadata executionMetadata) {

        try {
            BasicDBObject analysisDoc = new BasicDBObject();

            analysisDoc.put("execution_id", executionMetadata.getIdentifier());
            analysisDoc.put("study_id", executionMetadata.getStudyId());

            // eg. "computer"
            analysisDoc.put("source", executionMetadata.getSource());

            // eg. "segmentation"
            analysisDoc.put("computation", executionMetadata.getComputation());

            provenanceDoc.put("analysis", analysisDoc);
            provenanceDoc.put("data_loader", VERSION_NUMBER);
			provenanceDoc.put("batch_id", executionMetadata.getBatchId());
			provenanceDoc.put("tag_id", executionMetadata.getTagId());

            objDoc.put("provenance", provenanceDoc);
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return 0;

    }

    /**
     * Set provenance.
     */
    public int setProvenance(AnalysisExecutionMetadata executionMetadata, SimpleImageMetadata imgMeta) {
        setImageMetadata(imgMeta);
        setAnalysisMetadata(executionMetadata);

        return 0;
    }

    /**
     * TBA.
     * We are not *currently* generating, but we *will* generate.
     */
    public void setAnnotations(HashMap<String, String> annotations) {
        BasicDBList nvList = new BasicDBList();

        for (Map.Entry<String, String> entry : annotations.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            nvList.add(new BasicDBObject().append("predicate", key).append("object", value));

        }

        objDoc.append("annotations", nvList);
    }


    /**
     * TBA.
     */
    public void setPixels() {

    }

    /**
     * Set footprint.
     */
    public void setFootprint(double area) {
        try {
            objDoc.put("footprint", area);

        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    /**
     * Get metadata document.
     */
    public BasicDBObject getMetadataDoc() {
        return objDoc;
    }
}
