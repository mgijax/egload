#
# The purpose of this script is to:
#
#       add PARtner marker EntrezGene ids (ldb=55) for PARtners
#
#       use same user key (1416)
#
#       select all distinct PARtner markers
#       if the PARtner does not have an EG id (ldb=55), then add the EG id of its associated PARtner marker
#
#       example:
#       if Erdr1x has EG id and Erdr1y does not, then copy Erdr1x’s EG id to Erdr1y
#       or the reverse
#

import sys 
import os
import db

db.setTrace()

results = db.sql('select max(_Accession_key) + 1 as maxKey from ACC_Accession', 'auto')
accKey = results[0]['maxKey']
addAccSQL = ""

# find PARtner markers whose partner does not have an EG id
results = db.sql('''
select r.*, a1.*
from MGI_Relationship r, ACC_Accession a1
where r._category_key = 1012
and r._object_key_1 = a1._object_key
and a1._mgitype_key = 2
and a1._logicaldb_key = 55
and not exists 
        (select 1 from ACC_Accession a2 
                where r._object_key_2 = a2._object_key 
                and a2._mgitype_key = 2 
                and a2._logicaldb_key = 55
                )
''', 'auto')

for r in results:

        if r['prefixpart'] == None:
                prefixpart = 'null'
        else:
                prefixpart = "'" + r['prefixpart'] + "'"

        if r['numericpart'] == None:
                numericpart = 'null'
        else:
                numericpart = r['numericpart']

        addAccSQL += '''insert into acc_accession values(%s,'%s',%s,%s,%s,%s,%s,%s,%s,%s,%s,now(),now());\n''' \
                % (accKey, r['accid'], prefixpart, numericpart, r['_logicaldb_key'], r['_object_key_2'], 
                        r['_mgitype_key'], r['private'], r['preferred'], r['_createdby_key'], r['_modifiedby_key'])
        accKey += 1

if addAccSQL != "":
        print(addAccSQL)
        db.sql(addAccSQL, None)

print('par accessions to process: ' + str(len(results)) + '\n')
db.commit()

