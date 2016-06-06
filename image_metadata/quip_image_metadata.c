#define _GNU_SOURCE

#include "openslide.h"
#include "openslide-features.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdbool.h>
#include <sys/time.h>
#include <sys/stat.h>
#include <inttypes.h>
#include <time.h>

int main(int argc, char **argv) {

	if (argc!=6) {
		fprintf(stderr,"Usage: %s <image file> <cancer type> <subject id> <case id> <output file>\n",argv[0]);
		exit(1);
	}

	char *inp_file = argv[1];
	char *c_type   = argv[2];
	char *s_id     = argv[3];
	char *c_id     = argv[4];
	char *h_name   = "localhost";
	FILE *fpo      = fopen(argv[5],"w");
	
	int check_ok = 1;

	struct stat attrib;
	stat(inp_file,&attrib);
	char file_date[20];
	strftime(file_date, 20, "%m-%d-%y:%H.%M.%S", localtime(&(attrib.st_ctime)));

	openslide_t *osr = openslide_open(inp_file);
	if (osr==NULL) {
		fprintf(stderr,"Error: openslide cannot read file: %s\n",inp_file);
		exit(1);
	}		

	int64_t w,h;
	openslide_get_level0_dimensions(osr,&w,&h);

	if (w<=0 || h<=0) {
		fprintf(stderr,"Error: openslide cannot get width (%ld) and height (%ld) from the image file: %s\n.",w,h,inp_file)
		exit(1);
	}

	fprintf(fpo,"hostname,filename,last_modified,cancer_type,case_id,subject_id,identifier,width,height,mpp_x,mpp_y,objective,vendor,status,level_count,imageid\n");
	fprintf(fpo,"%s,",h_name);
	fprintf(fpo,"%s,",inp_file);
	fprintf(fpo,"%s,",file_date);
	fprintf(fpo,"%s,",c_type);
	fprintf(fpo,"%s,",c_id);
	fprintf(fpo,"%s,",s_id);
	fprintf(fpo,"%s,",c_id);
	fprintf(fpo,"%f,",(float)w);
	fprintf(fpo,"%f,",(float)h);

	/* get objective value */
	char *o_val = (char*)openslide_get_property_value(osr,"openslide.objective-power"); 
	if (o_val==NULL) 
	    o_val = (char*)openslide_get_property_value(osr,"aperio.AppMag");	

	char *mpp_x = (char*)openslide_get_property_value(osr,"openslide.mpp-x");
	if (mpp_x!=NULL) 
		fprintf(fpo,"%.8f,",atof(mpp_x));
	else { 
		if (o_val!=NULL) 
			fprintf(fpo,"%f,",0.25*40.0/atof(o_val));
		else {
			check_ok = 0;
			fprintf(fpo,"%f,",-1.0);
		}
	}
	char *mpp_y = (char*)openslide_get_property_value(osr,"openslide.mpp-y");
	if (mpp_y!=NULL) 
		fprintf(fpo,"%.8f,",atof(mpp_y));
	else {
		if (o_val!=NULL) 
			fprintf(fpo,"%f,",0.25*40.0/atof(o_val));
		else {
			check_ok = 0;
			fprintf(fpo,"%f,",-1.0);
		}
	}

	/* print objective value */
	if (o_val!=NULL) 
		fprintf(fpo,"%f,",(float)atof(o_val));
	else {
		fprintf(fpo,"%f,",-1.0);
		check_ok = 0;
	}
			
	char *vendor = (char*)openslide_get_property_value(osr,"openslide.vendor");
	if (vendor!=NULL) 
		fprintf(fpo,"%s,",vendor);
	else
		fprintf(fpo,"no_vendor,");

	if (check_ok==0) {
		fprintf(fpo,"some_errors,");
		fprintf(stderr,"SOME_ERRORS: %s\n",inp_file);
	} else {
		fprintf(fpo,"status_ok,");
		fprintf(stdout,"OK: %s\n",inp_file);
	}

	char *level_count = (char*)openslide_get_property_value(osr,"openslide.level-count");
	if (level_count!=NULL)
		fprintf(fpo,"%s,",level_count);
	else
		fprintf(fpo,"1,");

	char *image_id = (char*)openslide_get_property_value(osr,"aperio.ImageID");
	if (image_id!=NULL) 
		fprintf(fpo,"%d\n",atoi(image_id));
	else
		fprintf(fpo,"-1\n");

	fflush(fpo);
  	openslide_close(osr);
	fclose(fpo);

  	exit(0);
}
