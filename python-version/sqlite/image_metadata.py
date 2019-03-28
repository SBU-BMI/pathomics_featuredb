import hashlib
import sys
import openslide
import json
import csv
import ntpath
import os.path
import sqlite3
import argparse

# Command line arguments
parser = argparse.ArgumentParser(description="QuIP image metadata loader.")
parser.add_argument("--dbname",required=True,type=str,metavar="<database name>",help="The name of the sqlite3 database.")
parser.add_argument("--input",required=True,type=str,metavar="<manisfest file>",help="Image metadata manifest file.")
parser.add_argument("--folder",required=True,type=str,metavar="<root folder for images>",help="input image folder.")

inp_folder="/data/images/"
inp_manifest="manifest.csv"
out_manifest_csv="manifest-out.csv"
out_manifest_json="manifest-out.json"

# compute md5sum hash of image file
def md5(fname):
    hash_md5 = hashlib.md5()
    with open(fname, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_md5.update(chunk)
    return hash_md5.hexdigest()

# Extract openslide metadata from image file
def package_metadata(img_meta,img):
    img_prop = img.properties;
    img_meta["vendor"] = img_prop["openslide.vendor"];
    img_meta["level_count"] = int(img.level_count);
    img_meta["width"]  = img.dimensions[0];
    img_meta["height"] = img.dimensions[1];
    img_meta["objective_power"] = img_prop[openslide.PROPERTY_NAME_OBJECTIVE_POWER];
    img_meta["mpp_x"] = float(img_prop[openslide.PROPERTY_NAME_MPP_X]);
    img_meta["mpp_y"] = float(img_prop[openslide.PROPERTY_NAME_MPP_Y]);
    img_meta["mpp-x"] = float(img_prop[openslide.PROPERTY_NAME_MPP_X]);
    img_meta["mpp-y"] = float(img_prop[openslide.PROPERTY_NAME_MPP_Y]);
    img_meta_prop = {}
    for p in img_prop:
        img_meta_prop[p] = img_prop[p];
    img_meta["properties"] = img_meta_prop;
    return img_meta;

def openslide_metadata(fname):
    ierr = 0;
    img = None;
    try:
        img = openslide.OpenSlide(fname);
    except openslide.OpenSlideUnsupportedFormatError:
        ierr = 1;
    except openslide.OpenSlideError:
        ierr = 2;
    except:
        ierr = 3;

    img_meta = {};
    if ierr == 1:
        img_meta["error"] = "format-error";
    elif ierr == 2:
        img_meta["error"] = "openslide-error";
    elif ierr == 3:
        img_meta["error"] = "unknown-error";
    elif ierr == 0:
        img_meta["error"] = "no-error";
        img_meta = package_metadata(img_meta,img);
    img_temp = json.dumps(img_meta);
    img_json = json.loads(img_temp);
    return img_json,img;

def extract_macro_image(img):
    img_rgba  = img.associated_images;
    macro_rgb = None;
    label_rgb = None;
    thumb_rgb = None;
    if img_rgba != None:
        macro_rgb = img_rgba["macro"].convert("RGB");
        label_rgb = img_rgba["macro"].convert("RGB");
        thumb_rgb = img_rgba["macro"].convert("RGB");
    return macro_rgb,label_rgb,thumb_rgb;

def write_macro_image(macro_rgb,label_rgb,thumb_rgb,fname):
    base_name = ntpath.basename(fname);
    fname_pre = os.path.splitext(base_name)[0];
    fname_out = inp_folder + fname_pre + "-macro.jpg";
    macro_rgb.save(fname_out);
    fname_out = inp_folder + fname_pre + "-label.jpg";
    label_rgb.save(fname_out);
    fname_out = inp_folder + fname_pre + "-thumb.jpg";
    thumb_rgb.save(fname_out);

def create_image_table(dbname):
    conn = sqlite3.connect(dbname);
    sql = "CREATE TABLE IF NOT EXISTS images ";
    sql = sql + "(subject_id text, case_id text, study_id text, file-location text, filename text ";
    sql = sql + "md5sum text, md5_error text, error text);";
    c = conn.cursor();
    c.execute(sql);
    conn.commit();
    c.close();
    conn.close();

def write_image_table(dbname,img_json):
    conn = sqlite3.connect(dbname);
    sql = "INSERT INTO images (subject_id,case_id,study_id,file-location,filename,md5sum,md5_error,error) ";
    sql = sql + "values(?,?,?,?,?,?,?,?)";
    tdata = (img_json["subject_id"],img_json["case_id"],img_json["study_id"]);
    tdata = tdata + (img_json["file-location"],img_json["filename"],img_json["md5sum"],);
    tdata = tdata + (img_json["md5_error"],img_json["error"],);
    c = conn.cursor();
    c.execute(sql,tdata);
    conn.commit();
    c.close();
    conn.close();

def main(argv):
    quip_args = vars(parser.parse_args());
    inp_file = quip_args["manifest"];
    dbname = quip_args["dbname"];
    inp_folder = int(quip_args["folder"]);

    create_image_table(dbname);
 
    csv_reader = csv.reader(inp_file,delimiter=',')
    for row in csv_reader:
        fname = inp_folder+row[2];
        md5_val = md5(fname); 

        # Extract metadata from image
        img_json,img = openslide_metadata(fname);
        img_json["subject_id"] = row[0];
        img_json["case_id"] = row[1];
        img_json["study_id"] = "default";
        img_json["file-location"] = inp_folder + row[2];
        img_json["filename"] = img_json["file-location"]; 
        img_json["md5sum"] = str(md5_val);

        # Check if md5sum matches, if it has been provided
        if (len(row)==4 and row[3]!="-1"):
           if row[3]!=md5_val:
              img_json["md5_error"] = "md5_error";
           else:
              img_json["md5_error"] = "md5_ok";
        else:
           img_json["md5_error"] = "md5_computed";

        write_image_table(dbname,img_json);

        # If file is OK, extract macro image and write it out
        # if img_json["error"]=="no-error":
        #    macro_rgb,label_rgb,thumb_rgb = extract_macro_image(img);
        #    write_macro_image(macro_rgb,label_rgb,thumb_rgb,fname);

    inp_file.close();

if __name__ == "__main__":
   main(sys.argv[1:])

