package u24.mongodb.nuclear.segmentation;

import com.mongodb.BasicDBList;
import org.opencv.core.Point;

import java.util.ArrayList;

/**
 * Created by tdiprima on 1/19/16.
 */
public class Poinsettia {
    private double min_x;
    private double min_y;
    private double max_x;
    private double max_y;

    public double getMin_x() {
        return min_x;
    }

    public double getMin_y() {
        return min_y;
    }

    public double getMax_x() {
        return max_x;
    }

    public double getMax_y() {
        return max_y;
    }


    /**
     * Get min/max coordinates from bounding box.
     */
    public void getBoundingBox(ArrayList<Double> points) {
        min_x = points.get(0);
        min_y = points.get(1);
        max_x = min_x;
        max_y = min_y;

        for (int i = 0; i < points.size(); i += 2) {
            double temp_x = points.get(i);
            double temp_y = points.get(i + 1);
            if (min_x > temp_x)
                min_x = temp_x;
            if (min_y > temp_y)
                min_y = temp_y;
            if (max_x < temp_x)
                max_x = temp_x;
            if (max_y < temp_y)
                max_y = temp_y;
        }

    }

    public BasicDBList getPolygonPoints(ArrayList<Double> points)
    {

        BasicDBList objPointsList = new BasicDBList();
        for (int j = 0; j < points.size(); j += 2) {
            BasicDBList objPoints = new BasicDBList();
            objPoints.add(points.get(j).floatValue());
            objPoints.add(points.get(j + 1).floatValue());
            objPointsList.add(objPoints);
        }
        // Last element in the polygon list should be the same as the first element
        BasicDBList objPoints = new BasicDBList();
        objPoints.add(points.get(0).floatValue());
        objPoints.add(points.get(1).floatValue());
        objPointsList.add(objPoints);

        return objPointsList;
    }


    public void computeBoundingBox(Point[] points) {
        min_x = points[0].x;
        min_y = points[0].y;
        max_x = min_x;
        max_y = min_y;

        for (int i = 0; i < points.length; i++) {
            double temp_x = points[i].x;
            double temp_y = points[i].y;
            if (min_x > temp_x)
                min_x = temp_x;
            if (min_y > temp_y)
                min_y = temp_y;
            if (max_x < temp_x)
                max_x = temp_x;
            if (max_y < temp_y)
                max_y = temp_y;
        }

    }


    public BasicDBList getPolygonPoints(Point[] points)
    {
        BasicDBList objPointsList = new BasicDBList();
        for (int j = 0; j < points.length; j++) {
            BasicDBList objPoints = new BasicDBList();
            objPoints.add((float) points[j].x);
            objPoints.add((float) points[j].y);
            objPointsList.add(objPoints);
        }
        // Last element in the polygon list should be the same as the first element
        BasicDBList objPoints = new BasicDBList();
        objPoints.add((float) points[0].x);
        objPoints.add((float) points[0].y);
        objPointsList.add(objPoints);

        return objPointsList;
    }

}
