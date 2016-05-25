#! /usr/bin/env python

import os.path
import glob
import sys, getopt
import socket

def readCSV(fileName):
    allContent = [] 
    numFeatures = 0
    print fileName
    f = open(fileName,'r')
    readContent = f.read().split('\n')
    print len(readContent)
    numFeatures = len(readContent[0].split(','))
    f.close()   
    return (readContent,numFeatures)

def initFeatures():
    imaging_list = [ ["SizeInPixels", "size"], 
                      ["PrincipalMoments0", "shape"], 
                      ["PrincipalMoments1", "shape"], 
                      ["Elongation", "shape"], 
                      ["Perimeter", "size"], 
                      ["Roundness", "shape"], 
                      ["EquivalentSphericalRadius", "size"], 
                      ["EquivalentSphericalPerimeter", "size"], 
                      ["EquivalentEllipsoidDiameter0", "size"], 
                      ["EquivalentEllipsoidDiameter1","size"], 
                      ["Flatness", "shape"], 
                      ["meanR", "intensity"], 
                      ["meanG", "intensity"], 
                      ["meanB", "intensity"], 
                      ["stdR", "intensity"], 
                      ["stdG", "intensity"], 
                      ["stdB", "intensity"] ]

    genomic_list = [ ["EGFR", "gene"], 
                         ["KRAS", "gene"],
                         ["STK11_LKB1", "gene"],
                         ["TP53","gene"],
                         ["NF1", "gene"],
                         ["BRAF", "gene"],
                         ["SETD2", "gene"] ]
 
    imaging_features = []
    for i in range(len(imaging_list)):
        img_elem = []
        param_name = imaging_list[i][0] + "_median"
        img_elem.append(param_name)
        img_elem.append(imaging_list[i][1])
        imaging_features.append(img_elem)
        img_elem = []
        param_name = imaging_list[i][0] + "_Q25"
        img_elem.append(param_name)
        img_elem.append(imaging_list[i][1])
        imaging_features.append(img_elem)
        img_elem = []
        param_name = imaging_list[i][0] + "_Q75"
        img_elem.append(param_name)
        img_elem.append(imaging_list[i][1])
        imaging_features.append(img_elem)

    imaging_dict = dict(imaging_features)
    genomic_dict = dict(genomic_list)
    return (imaging_dict,genomic_dict)

def main(argv):
    imaging_dict,genomic_dict = initFeatures()

    fileName = ''
    outFile = ''
    try:
       opts, args = getopt.getopt(argv,"hi:o:")
    except getopt.GetoptError:
       print 'mainAggregateFeatures.py [-h] [-i <inputfile> -o <output file>]'
       sys.exit(2)
    if len(argv) == 0:
       print 'mainAggregateFeatures.py [-h] [-i <inputfile> -o <output file>]'
       sys.exit(2)
    for opt, arg in opts:
       if opt == '-h':
          print 'mainAggregateFeatures.py [-h] [-i <inputfile> -o <output file>]'
          sys.exit()
       elif opt in ("-i", "--ifile"):
          fileName = arg
       elif opt == '-o':
          outFile = arg

    allContent,numFeatures = readCSV(fileName)

    header = allContent[0].split(",")
    allContent.pop(0)

    fout = open(outFile,'w');
    for i in range(len(allContent)-1):
       dobj     = {}
       dobj_img = []
       dobj_cli = []
       dobj_gen = []
       readContent = allContent[i].split(",")
       print len(readContent)
       print len(header)
       for j in range(len(header)):
          if (header[j]=="bcr_patient_barcode"):
             dobj["patient_id"] = readContent[j]
             dobj["bcr_patient_barcode"] = readContent[j]
          if (header[j]=="AnalysisId"):
             dobj["analysis_id"] = readContent[j]
       dobj["visit_id"] = "visit-1"
       dobj["imaging_domain"] = "pathology"
       dobj["imaging_sequence"] = "H&E:tissue"
       for j in range(len(header)):
           if (header[j] in imaging_dict):
              dobj2 = {}
              dobj2["name"]  = header[j] 
              dobj2["value"] = float(readContent[j]) 
              dobj2["type"]  = imaging_dict[header[j]]
              dobj_img.append(dobj2)
           elif (header[j] in genomic_dict):
              dobj2 = {}
              dobj2["name"]  = header[j]
              dobj2["value"] = int(readContent[j])     
              dobj2["type"]  = genomic_dict[header[j]]
              dobj_gen.append(dobj2) 
           else:
              dobj2 = {}
              dobj2["name"]  = header[j]
              if (readContent[j].isdigit()):
                dobj2["value"] = float(readContent[j])
              else:
                dobj2["value"] = readContent[j]
              dobj2["type"]  = "clinical"
              dobj_cli.append(dobj2)
       dobj["imaging_features"]  = dobj_img
       dobj["genomic_features"]  = dobj_gen
       dobj["clinical_features"] = dobj_cli 
       fout.write(str(dobj))
       fout.write("\n");
    fout.close()         

if __name__ == '__main__':
	main(sys.argv[1:])
