package u24.mongodb.nuclear.segmentation;

import java.util.UUID;

import com.mongodb.BasicDBObject;

/**
 * Manage information about methods that are used in an analysis pipeline.
 * Not currently used.
 */
public class MethodMetadata {
    private String uuid;
    private String name;
    private String version;
    private String uri;
    private String inputParams;
    private String description;
    private BasicDBObject required_doc;
    private BasicDBObject user_doc;
    private BasicDBObject method_doc;

    private static final String _methodDocType = "method_instance";

    public MethodMetadata(String methodName, String methodVersion,
                          String methodURI, String methodInputParams, String methodDescription) {

        this.uuid = UUID.randomUUID().toString();
        this.name = methodName;
        this.version = methodVersion;
        this.uri = methodURI;
        this.inputParams = methodInputParams;
        this.description = methodDescription;

        required_doc = new BasicDBObject();
        required_doc.put("name", methodName);
        required_doc.put("version", methodVersion);
        required_doc.put("uri", methodURI);
        required_doc.put("parameters", methodInputParams);
        required_doc.put("description", methodDescription);

        method_doc = new BasicDBObject();
        method_doc.put("uuid", this.uuid);
        method_doc.put("type", _methodDocType);
        method_doc.put("metadata", required_doc);

        this.user_doc = null;
    }

    public void addUserDefinedMetadata(String varName, Object val) {
        if (user_doc == null)
            user_doc = new BasicDBObject(varName, val);
        else
            user_doc.put(varName, val);
        method_doc.put("userdefined", user_doc);
    }

    public BasicDBObject getMetadataDoc() {
        return method_doc;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getInputParams() {
        return inputParams;
    }

    public void setInputParams(String inputParams) {
        this.inputParams = inputParams;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
