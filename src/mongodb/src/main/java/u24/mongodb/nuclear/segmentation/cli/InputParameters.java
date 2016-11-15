package u24.mongodb.nuclear.segmentation.cli;

import java.io.FileWriter;

public class InputParameters {
    public String dbServer = null;
    
    public String inpType = null;
    public boolean isQuip = false;
    
    public String inputList = null;
    public String imageFile = null;
    public String inputFile = null;
    public String outFolder = null;
    
    public String caseID = null;
    public String subjectID = null;
    
    public boolean doNormalize = false;
    public boolean selfNormalize = false;
    
    public boolean getFromDB = false;
    
    public int width = 1;
    public int height = 1;
    public int shiftX = 0;
    public int shiftY = 0;
    
    public String execID = null;
    public String studyID = null;
    public String batchID = null;
    public String tagID = null;
    public String execType = null;
    public String execTitle = null;
    public String colorVal = null;
    public String execComp = null;
    public String nameSpace = null;
    
    public boolean doSimplify = false;
    public double minSize = 0.0;
    public double maxSize = 40000000000.0;
    
    public FileWriter outFileWriter = null;
    
    public InputParameters() { }
    
}
