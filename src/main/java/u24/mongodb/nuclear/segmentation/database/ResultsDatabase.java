package u24.mongodb.nuclear.segmentation.database;

import java.util.Date;
import java.util.Random;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

 
/**
 * Submit operations.
 */
public class ResultsDatabase {

    private String resultsDatabase = "u24_results";

    private Random rand;

    private MongoClient mongoClient;
    private MongoDatabase db;
    private MongoCollection<Document> collObjects;
    private MongoCollection<Document> collMetadata;
    private MongoCollection<Document> collImages;
    private MongoCollection<Document> collProvenance;

	static Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	static { root.setLevel(Level.INFO); }

    /**
     * Constructor.
     * Open connections.
     */
    public ResultsDatabase(String dbURI) {
        try {
            MongoClientURI mongoURI = new MongoClientURI(dbURI);
            mongoClient = new MongoClient(mongoURI);
            resultsDatabase = mongoURI.getDatabase();
            db = mongoClient.getDatabase(resultsDatabase);	
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
            db = mongoClient.getDatabase(resultsDatabase);
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
        String provenanceCollection = "provenance";
        collProvenance = db.getCollection(provenanceCollection);
    }
    
    public MongoDatabase getDB() {
    	return db;
    }

    public MongoCollection<Document> getImagesCollection() {
        return collImages;
    }
    
    public MongoCollection<Document> getProvenanceCollection() {
    	return collProvenance;
    }

	public MongoCollection<Document> getObjectsCollection() {
        return collObjects;
    }         

    public Document submitAnalysisExecutionMappingQuery(Document query) {
        return collMetadata.find(query).first();
    }

    public ObjectId submitObjectsDocument(Document doc) {
        doc.append("submit_date", new Date());
        doc.append("randval", rand.nextFloat());
        collObjects.insertOne(doc);

        return (ObjectId) doc.get("_id");
    }

    public ObjectId submitMetadataDocument(Document doc) {
        doc.append("submit_date", new Date());
        doc.append("randval", rand.nextFloat());
        collMetadata.insertOne(doc);

        return (ObjectId) doc.get("_id");
    }
    
    public ObjectId submitProvenanceDocument(Document doc) {
    	 doc.append("submit_date", new Date());
         doc.append("randval", rand.nextFloat());
         collProvenance.insertOne(doc);

         return (ObjectId) doc.get("_id");
    }

}
