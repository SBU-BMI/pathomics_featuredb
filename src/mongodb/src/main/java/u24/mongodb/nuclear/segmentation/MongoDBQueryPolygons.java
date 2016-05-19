package u24.mongodb.nuclear.segmentation;

import com.mongodb.*;
import com.mongodb.util.JSON;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class MongoDBQueryPolygons {

    public static void main(String args[]) {

        if (args.length != 3) {
            System.err
                    .println("Usage: <mongodb://host:port/database> <query> <output file>");
            return;
        }

        String[] tokens = args[0].split("://|:|/");
        String dbHost = tokens[1];
        int dbPort = Integer.parseInt(tokens[2]);
        String dbName = tokens[3];

        String queryString = args[1];
        String outFile = args[2];

        System.out.println("Host: " + dbHost + " Port: " + dbPort + " Query: " + queryString);

        try {

            MongoClient mongoClient = new MongoClient(dbHost, dbPort);
            DB segDb = mongoClient.getDB(dbName);
            DBCollection segColl = segDb.getCollection("objects");

            DBObject query = (DBObject) JSON.parse(queryString);

            DBCursor cursor = segColl.find(query);
            System.out.println("Cursor size: " + cursor.count());

            if (cursor.count() > 0) {
                cursor.batchSize(1000);
                String header = "case_id\texecution_id\tpolygon\n";

                BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
                writer.write(header);
                while (cursor.hasNext()) {
                    DBObject obj = cursor.next();
                    // System.out.println("Here: " + obj.toString());

                    DBObject provenance = (DBObject) obj.get("provenance");

                    DBObject img = (DBObject) provenance.get("image");
                    String caseId = img.get("case_id").toString();

                    DBObject execution = (DBObject) provenance.get("analysis");
                    String executionId = execution.get("execution_id").toString();

                    // System.out.println("Case: " + caseId + " " + executionId);
                    DBObject geometry = (DBObject) obj.get("geometry");

                    writer.write(caseId + "\t" + executionId + "\t");

                    BasicDBList objPolygons = (BasicDBList) geometry.get("coordinates");
                    for (Object poly : objPolygons) {
                        BasicDBList pntList = (BasicDBList) poly;
                        boolean once = true;
                        for (Object pnt : pntList) {
                            BasicDBList p = (BasicDBList) pnt;
                            // System.out.println("Point: [" + p.get(0).toString() + ":" + p.get(1).toString() +"]");
                            if (once) {
                                writer.write(p + "\n");
                                once = false;
                            } else {
                                writer.write("\t\t" + p + "\n");
                            }


                        }
                    }
                }
                writer.close();
            } else {
                System.out.println("No results.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
