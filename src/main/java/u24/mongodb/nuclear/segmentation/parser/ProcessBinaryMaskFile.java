package u24.mongodb.nuclear.segmentation.parser;

import org.bson.Document;
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

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProcessBinaryMaskFile implements ProcessFile {

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
	
	private BufferedWriter bufferedWriter;

	public ProcessBinaryMaskFile() { }

	public ProcessBinaryMaskFile(String fileName,
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
	}
	
	public ProcessBinaryMaskFile(FileParameters fileParams,
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
		
		this.execMeta = new AnalysisExecutionMetadata(inputParams.execID, 
				inputParams.studyID, inputParams.batchID,  inputParams.tagID, inputParams.execTitle, 
				inputParams.execType, inputParams.execComp);
		
		this.bufferedWriter = null;
		if (inputParams.outFileWriter!=null) 
			this.bufferedWriter = new BufferedWriter(inputParams.outFileWriter);
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
			double mpp_x = 0.25;
			double mpp_y = 0.25;
			double image_width = 1.0; 
			double image_height = 1.0;
			String cancer_type = "unknown";
			
			SimpleImageMetadata imgMeta = new SimpleImageMetadata();
			imgMeta.setIdentifier(caseId);
			imgMeta.setCaseid(caseId);
			imgMeta.setSubjectid(subjectId);
			imgMeta.setMpp_x(mpp_x);
			imgMeta.setMpp_y(mpp_y);
			imgMeta.setWidth(image_width);
			imgMeta.setHeight(image_height);
			imgMeta.setCancertype(cancer_type);
			
			if (inputParams.doNormalize && (inputParams.selfNormalize==false)) { // normalize using image metadata from the database
				if (inputParams.getFromDB) {
					imgMeta.setFromDB(caseId, subjectId, segDB);

					mpp_x = imgMeta.getMpp_x();
					mpp_y = imgMeta.getMpp_y();
					image_width = imgMeta.getWidth();
					image_height = imgMeta.getHeight();
				} else {
					image_width = inputParams.width;
					image_height = inputParams.height;
					imgMeta.setWidth(image_width);
					imgMeta.setHeight(image_height);
				}
			} 

			if (mpp_x < 0 || mpp_y < 0) {
				System.err.println("ERROR: Negative mpp values: (" + mpp_x + " " + mpp_y + "). Image: " + caseId);
				return;
			}

			// Check if dimensions are negative or zero
			if (image_width <= 0.0 || image_height <= 0.0) {
				System.err.println("ProcessBinaryMaskFile.java");
				System.err.println("ERROR: Dimensions are negative or zero.");
				return;
			}

			// Extract polygons from the mask file
			maskToPoly.readMask(fileName);
			maskToPoly.extractPolygons();
			if (inputParams.selfNormalize) {
				image_width  = maskToPoly.getImgWidth();
				image_height = maskToPoly.getImgHeight();
			}

			// Check and register image to analysis mapping information
			imgExecMap = new ImageExecutionMapping(execMeta, imgMeta, inputParams.colorVal);
			if (bufferedWriter==null) { // output to db 
				if (!imgExecMap.checkExists(segDB)) 
					segDB.submitMetadataDocument(imgExecMap.getMetadataDoc());
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

				ArrayList<ArrayList<Float>> objPointsList = pointSetter.getPolygonPoints(points);

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
