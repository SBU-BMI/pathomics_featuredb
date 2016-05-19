package u24.mongodb.nuclear.segmentation;

/**
 * Created by tdiprima on 1/20/16.
 */

import com.mongodb.*;

import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MongoIndex {
    static MongoClient mongoClient = null;
    static DB db = null;
    static DBCollection collection = null;

    static String dbHost = null;
    static int dbPort = 0;
    static String dbName = null;
    static String dbColl = null;

    public static void main(String[] args) {

        if (args.length != 4) {
            System.err
                    .println("Usage: host port database");
            return;
        }

        dbHost = args[0];
        dbPort = Integer.parseInt(args[1]);
        dbName = args[2];
        dbColl = args[3];

        System.out.println("Host: " + dbHost + " Port: " + dbPort
                + " DB: " + dbName + " Collection: " + dbColl);

        init();
        System.out.println("BEFORE:");
        listIndexes();
        System.out.println("CREATE INDICES:");
        createIdx();
        System.out.println("AFTER:");
        listIndexes();
        destroy();

    }


    public static void createIdx() {
        //create index on name field
        //use 1 for ascending index , -1 for descending index

        String[] idx = {"x", "y", "randval", "footprint", "scalar_features"};

        for (int i = 0; i < idx.length; i++) {
            BasicDBObject index = new BasicDBObject(idx[i], 1);
            collection.createIndex(index);
        }
        System.out.println("Indices created successfully.");
    }

    public static void features() {

        System.out.println("\nFeatures:\n");

        DBObject doc = collection.findOne();

        String properties = "properties";
        String scalar_features = "scalar_features";

        if (doc.containsField(properties)) {
            Object value = doc.get(properties);

            BasicDBObject feat = (BasicDBObject) value;

            if (feat.containsField(scalar_features)) {

                Object val = feat.get(scalar_features);

                BasicDBList dbList = (BasicDBList) val;
                BasicDBObject thing = (BasicDBObject) dbList.get(0);

                if (thing.containsField("nv")) {
                    BasicDBList dbObjects = (BasicDBList) thing.get("nv");

                    for (Object dbObject : dbObjects) {

                        System.out.println(dbObject);
                    }
                }


            }


        } else {
            System.out.println("I can't find what you're looking for.");

        }


    }

    public static void listIndexes() {
        List<DBObject> indexInfo = collection.getIndexInfo();

        System.out.println("\nIndexes:\n");
        for (DBObject temp : indexInfo) {

            Set<String> keySet = temp.keySet();
            Iterator<String> iterator = keySet.iterator();

            while (iterator.hasNext()) {

                String key = iterator.next();
                Object value = temp.get(key);

                System.out.println("\t(" + key + "," + value + ")");
                System.out.println("\t\ttypeof: " + value.getClass());
                System.out.println();

            }
        }
    }

    public static void init() {
        try {
            mongoClient = new MongoClient(new ServerAddress(dbHost, dbPort));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1); // BOUNCE //
        }

        db = mongoClient.getDB(dbHost);
        collection = db.getCollection(dbColl);
    }

    public static void destroy() {
        mongoClient.close();
    }
}