package u24.mongodb.nuclear.segmentation.parser;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import org.opencv.core.Point;

import u24.masktopoly.MaskToPoly;
import u24.masktopoly.PolygonData;
import u24.mongodb.nuclear.segmentation.cli.FileParameters;
import u24.mongodb.nuclear.segmentation.cli.InputParameters;
import u24.mongodb.nuclear.segmentation.database.ResultsDatabase;
import u24.mongodb.nuclear.segmentation.model.AnalysisExecutionMetadata;
import u24.mongodb.nuclear.segmentation.model.Image2DMarkupGeoJSON;
import u24.mongodb.nuclear.segmentation.model.ImageExecutionMapping;
import u24.mongodb.nuclear.segmentation.model.Poinsettia;
import u24.mongodb.nuclear.segmentation.model.SimpleImageMetadata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class ProcessQuipMaskFile implements ProcessFile {

	private String fileName;
	private String subjectId;
	private String caseId;
	private InputParameters inputParams;
	private AnalysisExecutionMetadata execMeta;
	private int shiftX, shiftY;
	private ResultsDatabase segDB = null;
	private MaskToPoly maskToPoly;
	private ImageExecutionMapping imgExecMap;
	private double min_x, min_y, max_x, max_y;
	private DBObject quipMeta;
	private boolean doNormalize;
	
	private BufferedWriter bufferedWriter;
	
	private final static Charset ENCODING = StandardCharsets.UTF_8;

	public ProcessQuipMaskFile() { }

	public ProcessQuipMaskFile(String fileName,
			String subjectId, String caseId, 
			AnalysisExecutionMetadata execMeta,
			InputParameters inputParams,
			int shiftX, int shiftY, ResultsDatabase segDB) {
		this.fileName = fileName;
		this.subjectId = subjectId;
		this.caseId = caseId;
		this.execMeta = execMeta;
		this.inputParams = inputParams;
		this.shiftX = shiftX;
		this.shiftY = shiftY;
		this.segDB = segDB;
		this.maskToPoly = new MaskToPoly();
		
		this.bufferedWriter = null;
		if (inputParams.outFileWriter!=null) 
			this.bufferedWriter = new BufferedWriter(inputParams.outFileWriter);
		
		this.doNormalize = true;
	}
	
	public ProcessQuipMaskFile(FileParameters fileParams,
			InputParameters inputParams,
			ResultsDatabase segDB) {
		this.fileName = fileParams.getFileName();
		this.subjectId = fileParams.getSubjectId();
		this.caseId = fileParams.getCaseId();
		this.shiftX = fileParams.getShiftX();
		this.shiftY = fileParams.getShiftY();
		this.inputParams = inputParams;
		this.segDB = segDB;
		this.maskToPoly = new MaskToPoly();
		
		this.bufferedWriter = null;
		if (inputParams.outFileWriter!=null) 
			this.bufferedWriter = new BufferedWriter(inputParams.outFileWriter);
		
		this.doNormalize = inputParams.doNormalize;
	}

	void shiftPoints(Point[] points) {
		for (int i = 0; i < points.length; i++) {
			points[i].x = (points[i].x + shiftX);
			points[i].y = (points[i].y + shiftY);
		}
	}

	void normalizePoints(Point[] points, double img_width, double img_height) {
		for (int i = 0; i < points.length; i++) {
			points[i].x = (points[i].x) / img_width;
			points[i].y = (points[i].y) / img_height;
		}
	}

	/**
	 *
	 */
	 public void processFile() {
		try {
			// Read the json file for analysis metadata
			Path path = Paths.get(fileName);
			BufferedReader br = Files.newBufferedReader(path, ENCODING);
			if (br==null) {
				System.err.println("ERROR: Cannot read file: " + fileName);
				return;
			}
			String line = br.readLine();
			quipMeta = (DBObject) JSON.parse(line);
			br.close();
			
			caseId    = quipMeta.get("case_id").toString();
			subjectId = quipMeta.get("subject_id").toString();
			String execId = quipMeta.get("analysis_id").toString();
			String execTitle = quipMeta.get("analysis_desc").toString();
			
			execMeta = new AnalysisExecutionMetadata(execId, inputParams.studyID, inputParams.batchID,  
					inputParams.tagID, execTitle, inputParams.execType, inputParams.execComp);
			
			double mpp_x = Double.parseDouble(quipMeta.get("mpp").toString());
			double mpp_y = mpp_x;
			double image_width  = Double.parseDouble(quipMeta.get("image_width").toString()); 
			double image_height = Double.parseDouble(quipMeta.get("image_height").toString()); 
			String cancer_type = "unknown";
			shiftX = Integer.parseInt(quipMeta.get("patch_minx").toString());
			shiftY = Integer.parseInt(quipMeta.get("patch_miny").toString());
			
			String maskFilePrefix  = quipMeta.get("out_file_prefix").toString();
			String maskFilePathTmp = (new File(fileName)).getAbsolutePath();
			String maskFilePath    = maskFilePathTmp.substring(0,maskFilePathTmp.lastIndexOf(File.separator));
			String maskFile = maskFilePath + "/" + maskFilePrefix + "-seg.png";
			
			// Extract polygons from the mask file
			maskToPoly.readMask(maskFile);
			maskToPoly.extractPolygons();
			if (inputParams.selfNormalize) {
				image_width  = maskToPoly.getImgWidth();
				image_height = maskToPoly.getImgHeight();
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
			imgExecMap = new ImageExecutionMapping(execMeta, imgMeta, inputParams.colorVal,quipMeta);
			if (bufferedWriter==null) { // output to db 
				if (!imgExecMap.checkExists(segDB)) 
					segDB.submitMetadataDocument(imgExecMap.getMetadataDoc());
			}
			
			// Check and register additional analysis provenance data
			BasicDBObject provQuery = new BasicDBObject();
			provQuery.put("analysis_execution_id", execId);
			DBObject qryResult = segDB.getProvenanceCollection().findOne(provQuery);
			if (qryResult == null) {
				quipMeta.put("analysis_execution_id", execId);
				segDB.submitProvenanceDocument((BasicDBObject)quipMeta);
			}
			
			if (doNormalize==false) {
				image_width  = 1.0;
				image_height = 1.0;
			}

			List<PolygonData> polygons = maskToPoly.getPolygons();
			PolygonData polygon;
			Point[] points;
			for (int i = 0; i < polygons.size(); i++) {
				polygon = polygons.get(i);
				points = polygon.points;
				shiftPoints(points);
				if (inputParams.doNormalize)
					normalizePoints(points,image_width,image_height);

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
							min_y, max_x, max_y, "Polygon", inputParams.doNormalize,
							objPointsList);
				}

				// Set footprint
				obj_2d.setFootprint(polygon.area); 

				// Set quantitative features
				HashMap<String, Object> features = new HashMap<>();
				features.put("Area", polygon.area);
				HashMap<String, HashMap<String, Object>> ns_features = new HashMap<>();
				// add namespace
				ns_features.put(inputParams.nameSpace, features);
				obj_2d.setScalarFeatures(ns_features);

				// Set provenance data
				obj_2d.setProvenance(execMeta, imgMeta);

				// load to segmentation results database or write to file
				if (bufferedWriter!=null)
					bufferedWriter.write(obj_2d.getMetadataDoc().toString() + "\n");
				else 
					segDB.submitObjectsDocument(obj_2d.getMetadataDoc());

			}
			System.out.println("Processed: " + polygons.size() + " polygons.");
			if (bufferedWriter != null) 
                bufferedWriter.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	 }
}
