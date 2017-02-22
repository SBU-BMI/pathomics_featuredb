package u24.mongodb.nuclear.segmentation.parser;

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

/**
 * Comma Separated Values file handler.
 */
public class ProcessCSVRadiologyFeatureFile implements ProcessFile {
	InputParameters inputParams;	
	private String fileName;
	private String caseId;
	private String subjectId;
	private AnalysisExecutionMetadata execMeta;
	private ImageExecutionMapping imgExecMap;
	private ResultsDatabase segDB;
	private double min_x, min_y, max_x, max_y;
	
	private final static Charset ENCODING = StandardCharsets.UTF_8;
	
	public ProcessCSVRadiologyFeatureFile() {
		this.fileName = null;
		this.subjectId = null;		
		this.caseId = null;
		this.execMeta = null;
	}

	public ProcessCSVRadiologyFeatureFile(String fileName, String subjectId, String caseId,
			AnalysisExecutionMetadata execMeta, InputParameters inputParams, ResultsDatabase segDB) {
		this.fileName = fileName;
		this.subjectId = subjectId;		
		this.caseId = caseId;
		this.execMeta = execMeta;
		this.inputParams = inputParams;
		this.segDB = segDB;
	}
	
	public ProcessCSVRadiologyFeatureFile(FileParameters fileParams, 
			InputParameters inputParams, ResultsDatabase segDB) {
		this.fileName = fileParams.getFileName();
		this.subjectId = fileParams.getSubjectId();		
		this.caseId = fileParams.getCaseId();
		this.inputParams = inputParams;
		this.segDB = segDB;

		this.execMeta = new AnalysisExecutionMetadata(inputParams.execID, 
				inputParams.studyID, inputParams.batchID,  inputParams.tagID, inputParams.execTitle, 
				inputParams.execType, inputParams.execComp);
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
	 * Performs the processing operation.
	 */
	public void processFile() {
		try {
			
			// Dummy polygon
			ArrayList<Double> normPoints = new ArrayList<>();
			normPoints.add(0.0); // x
			normPoints.add(0.0); // y
			normPoints.add(0.5);
			normPoints.add(0.0);
			normPoints.add(0.5);
			normPoints.add(0.5);
			normPoints.add(0.0); // x
			normPoints.add(0.0); // y
				
			Poinsettia pointSetter = new Poinsettia();
			pointSetter.getBoundingBox(normPoints);

			min_x = pointSetter.getMin_x();
			min_y = pointSetter.getMin_y();
			max_x = pointSetter.getMax_x();
			max_y = pointSetter.getMax_y();
			ArrayList<ArrayList<Float>> objPointsList = pointSetter.getPolygonPoints(normPoints);
			double area = (max_y-min_y)*(max_x-min_x);
			// End dummy polygon 
			
			// Read input CSV file
			// Extract header information
			Path path = Paths.get(fileName);
			BufferedReader br = Files.newBufferedReader(path, ENCODING);
			String line = br.readLine();
			String[] header  = line.split(",");
			int lineCnt = 0;
			int subjectIdx =0, caseIdx = 0; 
			// subject ID and case ID (image ID) location in the header 
			for (int ii=0;ii<header.length;ii++) {
				if (header[ii].equalsIgnoreCase("tciaPatient")==true) 
					subjectIdx = ii;
				if (header[ii].equalsIgnoreCase("img_name")==true)
					caseIdx = ii;
			}
			while ((line = br.readLine()) != null) {
				// Parse the segmentation results
				String[] values = line.split(",");

				if (values.length != header.length) {
					System.err.println("Error in input file: " + fileName + ". Missing columns (row length: "
							+ values.length + "!=" + header.length);
					return;
				}
				
				subjectId = values[subjectIdx];
				caseId    = values[caseIdx];

				SimpleImageMetadata imgMeta = new SimpleImageMetadata();
				imgMeta.setIdentifier(caseId);
				imgMeta.setCaseid(caseId);
				imgMeta.setSubjectid(subjectId);

				// Check and register image to analysis mapping information
				imgExecMap = new ImageExecutionMapping(execMeta, imgMeta, inputParams.colorVal);
				if (!imgExecMap.checkExists(segDB))
					segDB.submitMetadataDocument(imgExecMap.getMetadataDoc());
				
				Image2DMarkupGeoJSON obj_2d = new Image2DMarkupGeoJSON();
				
				obj_2d.setMarkup(min_x, min_y, max_x, max_y, "Polygon", true, objPointsList);
				obj_2d.setObjectType("ROI");
				obj_2d.setFootprint(area);

				// Set scalar features
				HashMap<String, Object> features = setFeatures(values, header, obj_2d);
				HashMap<String, HashMap<String, Object>> ns_features = new HashMap<>();
				ns_features.put(inputParams.nameSpace, features);
				obj_2d.setScalarFeatures(ns_features);

				// Set provenance data
				obj_2d.setProvenance(execMeta, imgMeta);

				// load to segmentation results database
				segDB.submitObjectsDocument(obj_2d.getMetadataDoc());
				lineCnt++;
			}
			System.out.println("Lines processed: " + lineCnt);
			br.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}

	public HashMap<String, Object> setFeatures(String[] values, String[] header, Image2DMarkupGeoJSON obj_2d) {
		HashMap<String, Object> features = new HashMap<>();

		for (int ii=0;ii<header.length;ii++) {
			if (!(header[ii].equalsIgnoreCase("tciaPatient")==true 	|| 
					header[ii].equalsIgnoreCase("img_name")==true	||
					header[ii].equalsIgnoreCase("bratsFilename")==true)) {	
				if (isNumeric(values[ii])) 
					features.put(header[ii], Double.parseDouble(values[ii]));
				else
					features.put(header[ii], values[ii]);
			}
		}
		return features;
	}
}
