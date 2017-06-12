import os
import json

chunk_size=8000

folder = '334334d6-9506-4dfb-8018-b09daefe319f'
files  = []
files += [each for each in os.listdir(folder) if each.endswith('-features.csv')]

metas = []
metas += [each for each in os.listdir(folder) if each.endswith('-algmeta.json')]

dest_dir='./tmp'
dest_pre='SEER_VTR_BC100_001_SLIDE_002'

file_hdr = open(os.path.join(folder,files[0]),'r')
header   = file_hdr.readline()
file_hdr.close();

mfile    = open(os.path.join(folder,metas[0]),'r')
metadata = json.load(mfile)
mfile.close()

line_cnt=1
file_cnt=0
file_prefix = dest_pre+'_'+str(file_cnt)
file_out = dest_dir+'/'+file_prefix+'-features.csv'
fout = open(file_out,"w");
fout.write(header)

metadata['out_file_prefix'] = file_prefix 
meta_out = dest_dir+'/'+file_prefix+'-algmeta.json'
mout = open(meta_out,"w")
mout.write(json.dumps(metadata))
mout.close()
for file in files:
    print 'Processing: ' + str(file)
    fin = open(os.path.join(folder,file),'r')
    file_data = fin.readlines()
    fin.close()

    for i in range(1,len(file_data)):
        fout.write(file_data[i])
        line_cnt = line_cnt + 1
        if (line_cnt>chunk_size):
           fout.close()
           file_cnt = file_cnt + 1
           file_prefix = dest_pre+'_'+str(file_cnt)
           file_out = dest_dir+'/'+file_prefix+'-features.csv'
           fout = open(file_out,"w");
           fout.write(header)

           metadata['out_file_prefix'] = file_prefix 
           meta_out = dest_dir+'/'+file_prefix+'-algmeta.json'
           mout = open(meta_out,"w")
           mout.write(json.dumps(metadata))
           mout.close()

           line_cnt = 1
fout.close()
