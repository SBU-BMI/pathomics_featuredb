package u24.mongodb.nuclear.segmentation;

import java.util.ArrayList;
import java.util.UUID;

import com.mongodb.BasicDBObject;

/**
 * Describe the analysis pipeline.
 * Not currently used.
 */
public class PipelineMetadata {
    private String uuid;
    private String name;
    private String version;
    private String uri;
    private String description;
    private BasicDBObject required_doc;
    private BasicDBObject pipeline_doc;
    private static final String _pipelineDocType = "pipeline_instance";

    // Variable assigned but not used:
    // private BasicDBObject user_doc;


    public PipelineMetadata(String pipelineName, String pipelineVersion,
                            String pipelineURI, String pipelineDescription,
                            ArrayList<MethodMetadata> pipelineMethods) {

        this.uuid = UUID.randomUUID().toString();
        this.name = pipelineName;
        this.version = pipelineVersion;
        this.uri = pipelineURI;
        this.description = pipelineDescription;

        ArrayList<BasicDBObject> method_docs = new ArrayList<>();
        for (int i = 0; i < pipelineMethods.size(); i++)
            method_docs.add(pipelineMethods.get(i).getMetadataDoc());

        required_doc = new BasicDBObject();
        required_doc.put("name", pipelineName);
        required_doc.put("version", pipelineVersion);
        required_doc.put("uri", pipelineURI);
        required_doc.put("description", pipelineDescription);

        pipeline_doc = new BasicDBObject();
        pipeline_doc.put("uuid", uuid);
        pipeline_doc.put("type", _pipelineDocType);
        pipeline_doc.put("metadata", required_doc);
        pipeline_doc.put("methods", method_docs);

        // this.user_doc = null;
    }

    public PipelineMetadata(String pipelineName) {

        this.uuid = UUID.randomUUID().toString();
        this.name = pipelineName;
        this.version = "dummyVersion";
        this.uri = "dummyURI";
        this.description = "dummyDescription";

        MethodMetadata pipelineMethod = new MethodMetadata("dummyName",
                "dummyVersion", "dummyURI", "dummyInputParams",
                "dummyDescription");
        ArrayList<BasicDBObject> method_docs = new ArrayList<>();
        method_docs.add(pipelineMethod.getMetadataDoc());

        required_doc = new BasicDBObject();
        required_doc.put("name", this.name);
        required_doc.put("version", this.version);
        required_doc.put("uri", this.uri);
        required_doc.put("description", this.description);

        pipeline_doc = new BasicDBObject();
        pipeline_doc.put("uuid", this.uuid);
        pipeline_doc.put("type", _pipelineDocType);
        pipeline_doc.put("metadata", required_doc);
        pipeline_doc.put("methods", method_docs);

        // this.user_doc = null;
    }

    public BasicDBObject getMetadataDoc() {
        return pipeline_doc;
    }

}
