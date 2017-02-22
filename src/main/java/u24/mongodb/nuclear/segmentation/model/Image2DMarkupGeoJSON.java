package u24.mongodb.nuclear.segmentation.model;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bson.Document;
import com.mongodb.util.JSON;

import java.util.ArrayList;

/**
 * The document creation process.
 * Database is GeoJSON compliant.
 */
public class Image2DMarkupGeoJSON {

    private Document objDoc;
    private Document propDoc;
    private Document featuresDoc;
    private Document provenanceDoc;
    private static final String VERSION_NUMBER = "1.3";
    private static final String TYPE = "type";

    public Image2DMarkupGeoJSON() {
        objDoc = new Document();
        propDoc = new Document();
        provenanceDoc = new Document();
        featuresDoc = new Document();

        // add self parent id
        objDoc.put(TYPE, "Feature");
        objDoc.put("parent_id", "self");
        objDoc.put("randval", new Random().nextFloat());
        objDoc.append("creation_date", new Date());
        objDoc.put("object_type", "nucleus");

    }

    public void setObjectType(String objType) {
        objDoc.put("object_type", objType);
    }

    /**
     * Create the document subsection that corresponds to the segmentation result.
     * Creates the section of the JSON that contains the segmentation data, polygon information, bounding box, location,
     * and coordinates.
     * <p>
     * Footprint = Area. Required for caMicroscope.
     */
    public int setMarkup(double min_x,
                         double min_y, double max_x, double max_y, String geomType,
                         boolean coordinatesNormalized, 
                         ArrayList<ArrayList<Float>> objPointsList) {

        try {
        	double mid_x = (min_x+max_x)/2;
        	double mid_y = (min_y+max_y)/2;
            objDoc.put("x", mid_x);
            objDoc.put("y", mid_y);
            objDoc.put("normalized", Boolean.toString(coordinatesNormalized));

            // Add bounding box
            ArrayList<Double> bboxList = new ArrayList<Double>();
            bboxList.add(min_x);
            bboxList.add(min_y);
            bboxList.add(max_x);
            bboxList.add(max_y);
            objDoc.put("bbox", bboxList);

            // Add geometry object
            Document geomDoc = new Document();
            // objType = "Polygon", etc.
            geomDoc.put(TYPE, geomType);

            // Add arrays of arrays of points
            ArrayList<ArrayList<ArrayList<Float>>> objList = new ArrayList<ArrayList<ArrayList<Float>>>();
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
            ArrayList<Document> scalar_features_list = new ArrayList<Document>();

            for (Map.Entry<String, HashMap<String, Object>> nsf : ns_features.entrySet()) {

                String ns = nsf.getKey();
                HashMap<String, Object> features = nsf.getValue();

                ArrayList<Document> nvList = new ArrayList<Document>();

                for (Map.Entry<String, Object> entry : features.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (!key.equalsIgnoreCase("namespace")) {
                        nvList.add(new Document().append("name", key).append("value", value));
                    }

                }

                featuresDoc = new Document();
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
            Document imgDoc = new Document();
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
            Document analysisDoc = new Document();

            analysisDoc.put("execution_id", executionMetadata.getIdentifier());
            analysisDoc.put("study_id", executionMetadata.getStudyId());
            analysisDoc.put("source", executionMetadata.getSource());
            analysisDoc.put("computation", executionMetadata.getComputation());

            String json = null;
            try {
                if (executionMetadata.getAlgorithmParameters() != null) {
                    json = executionMetadata.getAlgorithmParameters();

                    Document dbObject = (Document) JSON.parse(json);
                    analysisDoc.put("algorithmParams", dbObject);

                }
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                System.err.println("json: " + json);
            }

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
        ArrayList<Document> nvList = new ArrayList<Document>();

        for (Map.Entry<String, String> entry : annotations.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            nvList.add(new Document().append("predicate", key).append("object", value));

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
    public Document getMetadataDoc() {
        return objDoc;
    }
}
