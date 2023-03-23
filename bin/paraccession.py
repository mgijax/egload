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

addAccSQL = ""

# find PARtner markers whose partner does not have an EG id
results = db.sql('''
select r.*, a1.*, ar._refs_key
from MGI_Relationship r, ACC_Accession a1, ACC_AccessionReference ar
where r._category_key = 1012
and r._object_key_1 = a1._object_key
and a1._mgitype_key = 2
and a1._logicaldb_key = 55
and a1._accession_key = ar._accession_key
and not exists 
        (select 1 from ACC_Accession a2 
                where r._object_key_2 = a2._object_key 
                and a2._mgitype_key = 2 
                and a2._logicaldb_key = 55
                )
''', 'auto')

for r in results:
        addAccSQL += '''select count(*) from ACC_insert (%s,%s,'%s',%s,'Marker',%s,%s,%s,1);\n''' \
                % (r['_createdby_key'], r['_object_key_2'], r['accid'], r['_logicaldb_key'], 
                        r['_refs_key'], r['preferred'], r['private'])

if addAccSQL != "":
        db.sql(addAccSQL, None)

print('par accessions to process: ' + str(len(results)) + '\n')
db.commit()

