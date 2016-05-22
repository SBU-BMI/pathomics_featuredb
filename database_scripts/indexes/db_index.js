print('Creating indexes...');
var coll = db.objects;
coll.createIndex( {'provenance.image.case_id' : 1, 'provenance.analysis.execution_id' : 1, 'footprint' : 1, 'x' : 1, 'y': 1}, {'name' : "exe_case_fp_xy"});
coll.createIndex( {'provenance.image.case_id' : 1, 'provenance.analysis.execution_id' : 1, 'randval': 1}, {'name' : "case_exe_randval"});
coll.createIndex( {'provenance.image.subject_id' : 1, 'provenance.analysis.execution_id' : 1, 'randval': 1}, {'name' : "sub_exe_randval"});
coll.createIndex( {'randval' : 1} );
var collimg = db.images;
collimg.createIndex( {'subject_id' : 1, 'caseid_id' : 1}, {unique: true});
print('Completed index creation.');
