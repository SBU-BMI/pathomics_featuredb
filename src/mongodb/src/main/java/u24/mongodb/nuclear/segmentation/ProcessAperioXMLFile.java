package u24.mongodb.nuclear.segmentation;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.opencv.core.Point;
import u24.masktopoly.PolygonData;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

public class ProcessAperioXMLFile implements ProcessFile {

    private String fileName;
    private AnalysisExecutionMetadata executionMetadata;
    private String colorVal;
    private int shift_x, shift_y;
    private String caseID;
    private String subjectID;
    private ResultsDatabase inpDB;
    private int img_width, img_height;
    private boolean getFromDB;
    private boolean normalize;
    private ResultsDatabase outDB;
    private FileWriter outFileWriter;
    private BufferedWriter bufferedWriter;
    private ImageExecutionMapping imgExecMap;

    SimpleImageMetadata imgMeta;
    private double min_x, min_y, max_x, max_y;

    public ProcessAperioXMLFile(String fileName,
                           AnalysisExecutionMetadata executionMetadata,
                           int img_width, int img_height,
                           int shift_x, int shift_y,
                           boolean normalize,
                           String caseID,
                           ResultsDatabase outDB) {
        this.fileName = fileName;
        this.executionMetadata = executionMetadata;
        this.colorVal = "yellow";
        this.normalize  = normalize;
        this.img_width  = img_width;
        this.img_height = img_height;
        this.shift_x = shift_x;
        this.shift_y = shift_y;
        this.caseID = caseID;
        this.subjectID = caseID;
        this.outDB = outDB;
        this.outFileWriter = null;
        this.bufferedWriter = null;

        this.inpDB = null;
        this.getFromDB = false;
        this.imgExecMap = new ImageExecutionMapping();
        this.imgMeta = new SimpleImageMetadata();
    }

    public ProcessAperioXMLFile(String fileName,
                           AnalysisExecutionMetadata executionMetadata,
                           int img_width, int img_height,
                           int shift_x, int shift_y,
                           boolean normalize,
                           String caseID,
                           FileWriter outFileWriter) {
        this.fileName = fileName;
        this.executionMetadata = executionMetadata;
        this.colorVal = "yellow";
        this.normalize = normalize;
        this.img_width  = img_width;
        this.img_height = img_height;
        this.shift_x = shift_x;
        this.shift_y = shift_y;
        this.caseID = caseID;
        this.subjectID = caseID;
        this.outDB = null;
        this.outFileWriter = outFileWriter;
        this.bufferedWriter = new BufferedWriter(outFileWriter);

        this.inpDB = null;
        this.getFromDB = false;
        this.imgExecMap = new ImageExecutionMapping();
        this.imgMeta = new SimpleImageMetadata();
    }

    public void setCaseID(String caseID) {
        this.caseID = caseID;
    }

    public void setSubjectID(String subjectID) {
        this.subjectID = subjectID;
    }

    public void setColor(String colorVal) {
        this.colorVal = colorVal;
    }

    public void setImgMetaFromDB(ResultsDatabase inpDB) {
        this.getFromDB = true;
        this.inpDB = inpDB;
    }

    public void doNormalization() {
        this.normalize = true;
    }

    void shiftPoints(Point[] points) {
    	for (int i = 0; i < points.length; i++) {
            points[i].x = (points[i].x + shift_x);
            points[i].y = (points[i].y + shift_y);
        }
    }
    
    void normalizePoints(Point[] points) {
        for (int i = 0; i < points.length; i++) {
            points[i].x = (points[i].x) / img_width;
            points[i].y = (points[i].y) / img_height;
        }
    }

    /**
     * Query and retrieve image metadata values from images collection.
     */
    boolean setImageMetadata() {
        if (getFromDB) {
            // Query and retrieve image metadata values
            BasicDBObject imgQuery = new BasicDBObject();
            imgQuery.put("case_id", caseID);
            DBObject qryResult = inpDB.getImagesCollection().findOne(imgQuery);
            if (qryResult == null) {
                System.err.println("ERROR: Cannot find case_id: " + caseID);
                return false;
            }

            img_width = (int) (Double.parseDouble(qryResult.get("width")
                    .toString()));
            img_height = (int) (Double.parseDouble(qryResult.get("height")
                    .toString()));

            if (qryResult.get("subject_id") != null) {
                subjectID = qryResult.get("subject_id").toString();
            }


            // Check if dimensions are negative or zero
            if (img_width <= 0 || img_height <= 0) {
                System.err.println("ERROR: Image dimensions are wrong: ("
                        + img_width + "x" + img_height + ")");
                return false;
            }
        }
        imgMeta.setIdentifier(caseID);
        imgMeta.setCaseid(caseID);
        imgMeta.setSubjectid(subjectID);
        imgMeta.setWidth((double) img_width);
        imgMeta.setHeight((double) img_height);

        return true;
    }
    
    PolygonData parseRegion(Element nNode) {
    	PolygonData polygon = new PolygonData();
		if (nNode.getAttribute("Area")==null) {
			System.err.println("No Area attribute.");
			return null;
		} 
    	polygon.area = Double.parseDouble(nNode.getAttribute("Area"));
		NodeList pList = nNode.getElementsByTagName("Vertex");
		polygon.points = new Point[pList.getLength()];
		for (int i=0;i<pList.getLength();i++) {
			Element el = (Element) pList.item(i);
		
			polygon.points[i] = new Point();	
			polygon.points[i].x = Double.parseDouble(el.getAttribute("X"));
			polygon.points[i].y = Double.parseDouble(el.getAttribute("Y"));
			
		}
		return polygon;
    }
    
    /**
     *
     */
    public void processFile() {
        try {
            if (!setImageMetadata()) {
                return;
            }

            File fXmlFile = new File(fileName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("Region");
			for (int i=0;i<nList.getLength();i++) {
				Element nNode = (Element) nList.item(i);
				
				PolygonData polygon = parseRegion(nNode);
				if (polygon==null) return;
				Point[] points = polygon.points;
				shiftPoints(points);
				if (normalize)
					normalizePoints(points);
				
                Poinsettia pointSetter = new Poinsettia();
                pointSetter.computeBoundingBox(points);
                min_x = pointSetter.getMin_x();
                min_y = pointSetter.getMin_y();
                max_x = pointSetter.getMax_x();
                max_y = pointSetter.getMax_y();

                Image2DMarkupGeoJSON obj_2d = new Image2DMarkupGeoJSON();

                BasicDBList objPointsList = pointSetter.getPolygonPoints(points);

                // Check markup data
                if (objPointsList.size() > 0) {
                    // Set markup data
                    obj_2d.setMarkup(min_x,
                            min_y, max_x, max_y, "Polygon", normalize,
                            objPointsList);
                }

				// Set footprint
				obj_2d.setFootprint(polygon.area); 

                // Set quantitative features
                HashMap<String, Object> features = new HashMap<>();
                features.put("Area", polygon.area);
                HashMap<String, HashMap<String, Object>> ns_features = new HashMap<>();
                // add namespace
                String namespace = "http://u24.bmi.stonybrook.edu/v1";
                ns_features.put(namespace, features);
                obj_2d.setScalarFeatures(ns_features);

                // Set provenance data
                obj_2d.setProvenance(executionMetadata, imgMeta);

                if (outDB != null) {
                    // load to segmentation results database
                    outDB.submitObjectsDocument(obj_2d.getMetadataDoc());
                } else if (bufferedWriter != null) {
                    // Write segmentation results to file in JSON format
                    bufferedWriter.write(obj_2d.getMetadataDoc().toString() + "\n");
                }
			}

            if (outDB != null) {
                // Check and register image to analysis mapping information
                imgExecMap.setMetadataDoc(executionMetadata, imgMeta, colorVal);
                if (!imgExecMap.checkExists(outDB, executionMetadata.getIdentifier(), executionMetadata.getStudyId(), imgMeta.getCaseid())) {
                    outDB.submitMetadataDocument(imgExecMap.getMetadataDoc());
                }

            }        
            if (bufferedWriter != null) {
                bufferedWriter.close();
                outFileWriter.close();
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
