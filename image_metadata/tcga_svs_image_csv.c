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

	if (argc!=5) {
		fprintf(stderr,"Usage: %s <image file list> <hostname> <output file> <cancer type location>\n",argv[0]);
		exit(1);
	}

	FILE *fpi   = fopen(argv[1],"r");
	char *hname = argv[2];
	FILE *fpo   = fopen(argv[3],"w");
	
	char in_line[1024];
	char c_type[1024];
	char s_id[1024];
	char c_id[1024];

	fprintf(fpo,"hostname,filename,last_modified,cancer_type,case_id,subject_id,identifier,width,height,mpp_x,mpp_y,objective,vendor,status,level_count,imageid\n");
	while (fscanf(fpi,"%s",in_line)!=EOF) {

		int check_ok = 1;

		// Get cancer type, caseid, subjectid and identifier
		sprintf(c_type,"%s",in_line);
		char *s = strtok(c_type,"/");
		int i;
		for (i=0;i<atoi(argv[4])-1;i++) 
			s = strtok(NULL,"/");
		char *ctype = s;
		char *fname = NULL;
		while (s!=NULL) {
			fname = s;
			s = strtok(NULL,"/");
		}
		char *caseid = strtok(fname,".");
		fname = strtok(NULL,".");
		if (strcmp(fname,"svs"))
			sprintf(c_id,"%s.%s",caseid,fname);
		else
			sprintf(c_id,"%s",caseid);

		sprintf(s_id,"%s",caseid);
		int loc_cnt = 0;
		for (i=0;i<strlen(s_id);i++) {
			if (s_id[i]=='-') 
				loc_cnt++;
			if (loc_cnt>=3) {
				s_id[i] = '\0';
				break;
			}
		}

		struct stat attrib;
		stat(in_line,&attrib);
		char file_date[20];
		strftime(file_date, 20, "%m-%d-%y:%H.%M.%S", localtime(&(attrib.st_ctime)));

		if (openslide_can_open(in_line)==1) {
  			openslide_t *osr = openslide_open(in_line);
			if (osr==NULL) {
				fprintf(stdout,"Error: %s\n",in_line);
  				fprintf(stdout,"openslide_can_open returns %s\n", openslide_can_open(in_line) ? "true" : "false");
				exit(1);
			}		

			int64_t w,h;
			openslide_get_level0_dimensions(osr,&w,&h);

			fprintf(fpo,"%s,",hname);
			fprintf(fpo,"%s,",in_line);
			fprintf(fpo,"%s,",file_date);
			fprintf(fpo,"%s,",ctype);
			fprintf(fpo,"%s,",caseid);
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
				fprintf(stdout,"SOME_ERRORS: %s\n",in_line);
			} else {
				fprintf(fpo,"status_ok,");
				fprintf(stdout,"OK: %s\n",in_line);
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
		} else {
			fprintf(stdout,"FULL_ERROR: %s\n",in_line);
			fprintf(fpo,"%s,",hname);
			fprintf(fpo,"%s,",in_line);
			fprintf(fpo,"%s,",file_date);
			fprintf(fpo,"%s,",ctype);
			fprintf(fpo,"%s,",caseid);
			fprintf(fpo,"%s,",c_id);
			fprintf(fpo,"%f,",-1.0);
			fprintf(fpo,"%f,",-1.0);
			fprintf(fpo,"%f,",-1.0);
			fprintf(fpo,"%f,",-1.0);
			fprintf(fpo,"%f,",-1.0);
			fprintf(fpo,"no_vendor,");
			fprintf(fpo,"cannot_process\n");
			fflush(fpo);
		}
	}	

	fclose(fpo);
	fclose(fpi);

  	return 0;
}
