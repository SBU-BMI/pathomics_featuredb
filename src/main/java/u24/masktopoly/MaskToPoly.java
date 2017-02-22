package u24.masktopoly;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class MaskToPoly {
	private Mat inputImg;
	private boolean normalize;
	private List<MatOfPoint>  contours;
	private List<PolygonData> out_poly;
	private static double AREA_THRESHOLD = 4.0;

	static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

	public MaskToPoly() {
		inputImg  = null;
		contours  = null;
		out_poly  = null;
		normalize = false;
	}
	
	public MaskToPoly(boolean normalize) {
		inputImg = null;
		contours = null;
		out_poly = null;
		this.normalize = normalize;
	}
	
	public void doNormalization() {
		this.normalize = true;
	}

	public int readMask(String inpFile) 
	{
		inputImg = Imgcodecs.imread(inpFile, CvType.CV_8UC1);
		return inputImg.empty()?1:0; 
	}
	
	public int getImgWidth() {
		return (int) (inputImg.size().width);
	}
	
	public int getImgHeight() {
		return (int) (inputImg.size().height);
	}

	public int extractContours()
	{
		// Find contours
		double width  = inputImg.size().width;
		double height = inputImg.size().height;
		Mat temp = Mat.zeros(new Size(width+2,height+2), inputImg.type());
		Core.copyMakeBorder(inputImg, temp, 1, 1, 1, 1, Core.BORDER_CONSTANT);

		contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		out_poly = new ArrayList<PolygonData>();
		for (int i = 0; i < contours.size(); i++) {
			Point[] poly_points = contours.get(i).toArray();
			if (poly_points.length>2) {
				// Find average point 
				double mid_x = 0;
				double mid_y = 0;
				for (int ptc = 0; ptc < poly_points.length; ++ptc) {
					mid_x += poly_points[ptc].x;
					mid_y += poly_points[ptc].y;
				} 
				mid_x = (mid_x/poly_points.length);
				mid_y = (mid_y/poly_points.length);

				// Find area of polygon
				Mat contour = new Mat(1,poly_points.length, CvType.CV_32FC2);
				for (int ptc=0;ptc<poly_points.length;ptc++) 
					contour.put(0,ptc,poly_points[ptc].x,poly_points[ptc].y);
				double poly_area = Imgproc.contourArea(contour);

				if (poly_area>AREA_THRESHOLD) { // if area is greater than threshold, write polygon out
					PolygonData poly_data = new PolygonData();
					poly_data.mid_x  = mid_x;
					poly_data.mid_y  = mid_y;
					poly_data.area   = poly_area;
					poly_data.points = poly_points; 
					out_poly.add(poly_data);
				}
			}
		}

		return 0;
	}

	public int extractPolygons()
	{
		// Find contours
		double width  = inputImg.size().width;
		double height = inputImg.size().height;
		Mat temp = Mat.zeros(new Size(width+2,height+2), inputImg.type());
		Core.copyMakeBorder(inputImg, temp, 1, 1, 1, 1, Core.BORDER_CONSTANT);

		contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		// Approximate contours to polygons
		out_poly = new ArrayList<PolygonData>();
		for (int i = 0; i < contours.size(); i++) {
			MatOfPoint2f approxCurve = new MatOfPoint2f();
			MatOfPoint2f inpCurve = new MatOfPoint2f(contours.get(i).toArray());
		   	Imgproc.approxPolyDP(inpCurve, approxCurve, 2, true);
			MatOfPoint outCurve = new MatOfPoint(approxCurve.toArray());

			if (outCurve.toArray().length>2) {
				Point[] poly_points = outCurve.toArray();

				// Find average point of polygon 
				double mid_x = 0;
				double mid_y = 0;
				for (int ptc = 0; ptc < poly_points.length; ++ptc) {
					mid_x += poly_points[ptc].x;
					mid_y += poly_points[ptc].y;
				} 
				mid_x = (mid_x/poly_points.length);
				mid_y = (mid_y/poly_points.length);

				// Find area of polygon
				Mat contour = new Mat(1,poly_points.length, CvType.CV_32FC2);
				for (int ptc=0;ptc<poly_points.length;ptc++) 
					contour.put(0,ptc,poly_points[ptc].x,poly_points[ptc].y);
				double poly_area = Imgproc.contourArea(contour);

				if (poly_area>AREA_THRESHOLD) { // if area is greater than threshold, write polygon out
					PolygonData poly_data = new PolygonData();
					poly_data.mid_x  = mid_x;
					poly_data.mid_y  = mid_y;
					poly_data.area   = poly_area;
					poly_data.points = poly_points; 
					out_poly.add(poly_data);
				}
			}
		}

		return 0;
	}
	
	public void normalizePoints() {
		if (normalize) {
			PolygonData polygon;
			Point[] points;
			double width  = inputImg.size().width;
			double height = inputImg.size().height;
			for (int plg = 0; plg < out_poly.size(); plg++) {
				polygon = out_poly.get(plg);
				points = polygon.points;
				for (int ptx = 0; ptx < points.length; ptx++) {
					points[ptx].x = points[ptx].x/width;
					points[ptx].y = points[ptx].y/height;
				}
			}
		}
	}

	public int editBoundaries()
	{
		return 1;
	}

	public int getPolygonCount()
	{
		return out_poly.size();
	}

	public List<PolygonData> getPolygons()
	{
		return out_poly;
	}

	public int writePolygons(String outFile, int shiftX, int shiftY)
	{
		if (out_poly.size()<=0) { return 1; }

		try {
            		FileWriter fileWriter = new FileWriter(outFile,true);
            		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter); 

			bufferedWriter.write("PolygonNo\tX\tY\tArea\tBoundaries");
			bufferedWriter.newLine();	

			for (int idx = 0; idx < out_poly.size(); idx++) {
				PolygonData pd = out_poly.get(idx);
				double mid_x = pd.mid_x+shiftX;
				double mid_y = pd.mid_y+shiftY;
				bufferedWriter.write(idx + "\t" + mid_x + "\t" + mid_y + "\t" + pd.area + "\t");
				for (int ptc = 0; ptc < pd.points.length; ++ptc) {
					double x = pd.points[ptc].x + shiftX;
					double y = pd.points[ptc].y + shiftY;
					bufferedWriter.write(x + "," + y + ";");
				}
				bufferedWriter.newLine();
			}	
            		bufferedWriter.close();
        	} catch(IOException ex) {
            		System.out.println( "Error writing to file '" + outFile + "'");
			return 1;
        	}
		return 0;
	}

	public static void main(String argv[]) {
		if (argv.length!=4) {
			System.out.println("Usage: <mask file> <shift_x> <shift_y> <output file>");
			return;
		}

		String inpFile = argv[0];
		int shift_x = Integer.parseInt(argv[1]);
		int shift_y = Integer.parseInt(argv[2]);
		String outFile = argv[3];

		MaskToPoly m2p = new MaskToPoly();
		m2p.readMask(inpFile);
		m2p.extractPolygons();
		m2p.writePolygons(outFile,shift_x,shift_y);
	
	}

}

