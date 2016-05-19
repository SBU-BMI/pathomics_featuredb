package u24.mongodb.nuclear.segmentation;

import java.util.Date;
import java.util.Random;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.MongoClientURI;

/**
 * Submit operations.
 */
public class ResultsDatabase {

    private String resultsDatabase = "u24_results";

    private Random rand;

    private MongoClient mongoClient;
    private DB db;
    private DBCollection collObjects;
    private DBCollection collMetadata;
    private DBCollection collImages;

    /**
     * Constructor.
     * Open connections.
     */
    public ResultsDatabase(String dbURI) {
        try {
            MongoClientURI mongoURI = new MongoClientURI(dbURI);
            mongoClient = new MongoClient(mongoURI);
            resultsDatabase = mongoURI.getDatabase();
            db = mongoClient.getDB(resultsDatabase);	
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        rand = new Random();
        initCollections();
    }
    
    /**
     * Constructor.
     * Open connections.
     */
    public ResultsDatabase(String dbHost, int dbPort, String resultDbName) {
        try {
            mongoClient = new MongoClient(dbHost, dbPort);
            if (resultDbName != null)
                resultsDatabase = resultDbName;
            db = mongoClient.getDB(resultsDatabase);
            mongoClient.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        rand = new Random();
        initCollections();
    }
    
    /**
     * Access specific collections.
     */
    private void initCollections() {
        String objectsCollection = "objects";
        collObjects = db.getCollection(objectsCollection);
        String metadataCollection = "metadata";
        collMetadata = db.getCollection(metadataCollection);
        String imagesCollection = "images";
        collImages = db.getCollection(imagesCollection);
    }
    
    public DB getDB() {
    	return db;
    }

    public DBCollection getImagesCollection() {
        return collImages;
    }

    public DBCursor submitAnalysisExecutionMappingQuery(BasicDBObject query) {
        return collMetadata.find(query);
    }

    public ObjectId submitObjectsDocument(BasicDBObject doc) {
        doc.append("submit_date", new Date());
        doc.append("randval", rand.nextFloat());
        collObjects.insert(doc);

        return (ObjectId) doc.get("_id");
    }

    public ObjectId submitMetadataDocument(BasicDBObject doc) {
        doc.append("submit_date", new Date());
        doc.append("randval", rand.nextFloat());
        collMetadata.insert(doc);

        return (ObjectId) doc.get("_id");
    }

}
