package u24.mongodb.nuclear.segmentation.parser;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;


import u24.mongodb.nuclear.segmentation.cli.FileParameters;
import u24.mongodb.nuclear.segmentation.cli.InputParameters;
import u24.mongodb.nuclear.segmentation.database.ResultsDatabase;
import u24.mongodb.nuclear.segmentation.model.AnalysisExecutionMetadata;
import u24.mongodb.nuclear.segmentation.model.Image2DMarkupGeoJSON;
import u24.mongodb.nuclear.segmentation.model.ImageExecutionMapping;
import u24.mongodb.nuclear.segmentation.model.Poinsettia;
import u24.mongodb.nuclear.segmentation.model.SimpleImageMetadata;


// Polygon simplification
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Comma Separated Values file handler.
 */
public class ProcessCSVFeaturePolygonFile implements ProcessFile {
    InputParameters inputParams;
    private String fileName;
    private String caseId;
    private String subjectId;
    private AnalysisExecutionMetadata execMeta;
    private ImageExecutionMapping imgExecMap;
    private ResultsDatabase segDB;
    private double min_x, min_y, max_x, max_y;
    private int numPointsLimit;
    private double simplifyTolerance;
    private GeometryFactory geomFactory;

    private final static Charset ENCODING = StandardCharsets.UTF_8;
    private final static int SIMPLIFY_POINTS_LIMIT = 20;
    private final static double SIMPLIFY_TOLERANCE = 0.1;

    public ProcessCSVFeaturePolygonFile() {
        this.fileName = null;
        this.subjectId = null;
        this.caseId = null;
        this.execMeta = null;
        this.numPointsLimit = SIMPLIFY_POINTS_LIMIT;
        this.simplifyTolerance = SIMPLIFY_TOLERANCE;
        this.geomFactory = null;
    }

    public ProcessCSVFeaturePolygonFile(String fileName, String subjectId, String caseId,
                                        AnalysisExecutionMetadata execMeta, InputParameters inputParams, ResultsDatabase segDB) {
        this.fileName = fileName;
        this.subjectId = subjectId;
        this.caseId = caseId;
        this.execMeta = execMeta;
        this.inputParams = inputParams;
        this.segDB = segDB;
        this.numPointsLimit = SIMPLIFY_POINTS_LIMIT;
        this.simplifyTolerance = SIMPLIFY_TOLERANCE;
        this.geomFactory = new GeometryFactory();
    }

    public ProcessCSVFeaturePolygonFile(FileParameters fileParams,
                                        InputParameters inputParams, ResultsDatabase segDB) {
        this.fileName = fileParams.getFileName();
        this.subjectId = fileParams.getSubjectId();
        this.caseId = fileParams.getCaseId();
        this.inputParams = inputParams;
        this.segDB = segDB;
        this.numPointsLimit = SIMPLIFY_POINTS_LIMIT;
        this.simplifyTolerance = SIMPLIFY_TOLERANCE;
        this.geomFactory = new GeometryFactory();

        this.execMeta = new AnalysisExecutionMetadata(inputParams);

    }

    public boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Does normalization.
     */
    ArrayList<Double> getNormalizedPoints(String values, double img_width, double img_height) {
        // get list of points and simplify polygon boundaries
        String[] points = values.split("\\[|:|\\]");
        int numPoints = (points.length - 1) / 2;
        if (inputParams.doSimplify == true && numPoints > numPointsLimit) {
            Coordinate[] coords = new Coordinate[numPoints];
            for (int i = 1, j = 0; i < points.length; i += 2, j++) {
                coords[j] = new Coordinate();
                coords[j].x = Double.parseDouble(points[i]);
                coords[j].y = Double.parseDouble(points[i + 1]);
            }
            coords[coords.length - 1] = coords[0]; // make it a closed ring

            CoordinateArraySequence coordSeq = new CoordinateArraySequence(coords);
            LinearRing polyRing = new LinearRing(coordSeq, geomFactory);
            Polygon polygon = new Polygon(polyRing, null, geomFactory);
            TopologyPreservingSimplifier tpSimp = new TopologyPreservingSimplifier(polygon);
            tpSimp.setDistanceTolerance(simplifyTolerance);
            Geometry outPoly = tpSimp.getResultGeometry();
            Coordinate[] outCoords = outPoly.getCoordinates();
            ArrayList<Double> out_points = new ArrayList<>();
            for (int i = 0; i < outCoords.length - 1; i++) {  // length-1, last element is the same as the first element
                out_points.add(outCoords[i].x / img_width);
                out_points.add(outCoords[i].y / img_height);
            }
            return out_points;
        } else {
            ArrayList<Double> out_points = new ArrayList<>();
            for (int i = 1; i < points.length; i += 2) {
                out_points.add(Double.parseDouble(points[i]) / img_width);  // x
                out_points.add(Double.parseDouble(points[i + 1]) / img_height); // y
            }
            return out_points;
        }
    }

    /**
     * Performs the processing operation.
     */
    public void processFile() {
        try {
            double mpp_x = 0.25;
            double mpp_y = 0.25;
            double image_width = 1.0;
            double image_height = 1.0;
            String cancer_type = "unknown";
            if (inputParams.doNormalize) { // normalize using image metadata from the database
                if (inputParams.getFromDB) {
                    // Query and retrieve image metadata values
                    BasicDBObject imgQuery = new BasicDBObject();
                    imgQuery.put("case_id", caseId);
                    imgQuery.put("subject_id", subjectId);

                    DBObject qryResult = segDB.getImagesCollection().findOne(imgQuery);
                    if (qryResult == null) {
                        System.err.println("ProcessCSVFeaturePolygonFile.java");
                        System.err.println("ERROR: Cannot find caseid: " + caseId);
                        return;
                    }

                    mpp_x = Double.parseDouble(qryResult.get("mpp_x").toString());
                    mpp_y = Double.parseDouble(qryResult.get("mpp_y").toString());
                    image_width = Double.parseDouble(qryResult.get("width").toString());
                    image_height = Double.parseDouble(qryResult.get("height").toString());
                    cancer_type = "no_value"; // qryResult.get("cancer_type").toString();

                    if (mpp_x < 0 || mpp_y < 0) {
                        System.err.println("ERROR: Negative mpp values: (" + mpp_x + " " + mpp_y + "). Image: " + caseId);
                        return;
                    }

                    // Check if dimensions are negative or zero
                    if (image_width <= 0.0 || image_height <= 0.0) {
                        System.err.println("ProcessCSVFeaturePolygonFile.java");
                        System.err.println("ERROR: Dimensions are negative or zero.");
                        return;
                    }
                } else {
                    image_width = inputParams.width;
                    image_height = inputParams.height;
                }
            }

            SimpleImageMetadata imgMeta = new SimpleImageMetadata();
            imgMeta.setIdentifier(caseId);
            imgMeta.setCaseid(caseId);
            imgMeta.setSubjectid(subjectId);
            imgMeta.setMpp_x(mpp_x);
            imgMeta.setMpp_y(mpp_y);
            imgMeta.setWidth(image_width);
            imgMeta.setHeight(image_height);
            imgMeta.setCancertype(cancer_type);

            // Check and register image to analysis mapping information
            imgExecMap = new ImageExecutionMapping(execMeta, imgMeta, inputParams.colorVal);
            if (!imgExecMap.checkExists(segDB)) {
                segDB.submitMetadataDocument(imgExecMap.getMetadataDoc());
            }

            // Read input CSV file
            // Extract header information
            // Last position is Polygon
            Path path = Paths.get(fileName);
            BufferedReader br = Files.newBufferedReader(path, ENCODING);
            String line = br.readLine();
            String[] header = line.split(",");
            int polygonIndex = header.length - 1;
            if (header[polygonIndex].compareTo("") == 0) { // extra comma at the end
                System.err.println("Error in input file:" + fileName + ". Extra comma at the end.");
                return;
            }
            int lineCnt = 0;
            while ((line = br.readLine()) != null) {
                // Parse the segmentation results
                String[] values = line.split(",");

                if (values.length != header.length) {
                    System.err.println("Error in input file: " + fileName + ". Missing columns (row length: "
                            + values.length + "!=" + header.length);
                    return;
                }

                double micronArea = 10.0;
                if (inputParams.doNormalize) micronArea = Float.parseFloat(values[0]) * mpp_x * mpp_y;
                if (micronArea >= inputParams.minSize && micronArea < inputParams.maxSize) { // Eliminate too small and too large nuclei
                    // Extract polygon information
                    ArrayList<Double> normPoints = getNormalizedPoints(values[polygonIndex], image_width, image_height);

                    Poinsettia pointSetter = new Poinsettia();
                    pointSetter.getBoundingBox(normPoints);

                    min_x = pointSetter.getMin_x();
                    min_y = pointSetter.getMin_y();
                    max_x = pointSetter.getMax_x();
                    max_y = pointSetter.getMax_y();

                    Image2DMarkupGeoJSON obj_2d = new Image2DMarkupGeoJSON();
                    BasicDBList objPointsList = pointSetter.getPolygonPoints(normPoints);

                    // Set markup data
                    if (objPointsList.size() > 0)
                        obj_2d.setMarkup(min_x, min_y, max_x, max_y, "Polygon", inputParams.doNormalize, objPointsList);

                    // Set scalar features
                    // Last column is Polygon data
                    // Area == NumberOfPixels is column 1
                    HashMap<String, Object> features = setFeatures(values, header, polygonIndex, mpp_x, mpp_y, obj_2d);

                    HashMap<String, HashMap<String, Object>> ns_features = new HashMap<>();
                    ns_features.put(inputParams.nameSpace, features);

                    obj_2d.setScalarFeatures(ns_features);

                    // Set provenance data
                    obj_2d.setProvenance(execMeta, imgMeta);

                    // load to segmentation results database
                    segDB.submitObjectsDocument(obj_2d.getMetadataDoc());
                }
                lineCnt++;
            }
            System.out.println("Lines processed: " + lineCnt);
            br.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public HashMap<String, Object> setFeatures(String[] values, String[] header, int colEnd,
                                               double mpp_x, double mpp_y, Image2DMarkupGeoJSON obj_2d) {
        HashMap<String, Object> features = new HashMap<>();
        if (inputParams.doNormalize) { // Dealing with QUIP CSV Format
            // Area == NumberOfPixels is column 1
            // Set footprint and NumberOfPixels
            if (isNumeric(values[0])) {
                double area = Double.parseDouble(values[0]);
                obj_2d.setFootprint(area); // Footprint
                features.put("SizeInPixels", area);
                features.put("PhysicalSize", mpp_x * mpp_y * area);
            }
            for (int i = 1; i < colEnd; i++) {
                if (isNumeric(values[i]))
                    features.put(header[i], Float.parseFloat(values[i]));
            }
        } else {
            for (int i = 0; i < colEnd; i++) {
                if (isNumeric(values[i]))
                    features.put(header[i], Float.parseFloat(values[i]));
            }
        }
        return features;
    }
}
