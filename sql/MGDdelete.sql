select r._Accession_key
into temp toDelete
from ACC_AccessionReference r, ACC_Accession a, MRK_Marker m
where r._Refs_key = 64047
and r._Accession_key = a._Accession_key
and a._MGIType_key = 2
and a._Object_key = m._Marker_key
and m._Organism_key = 1
;

create index idx1 on toDelete(_Accession_key)
;

delete from ACC_AccessionReference
using toDelete d
where d._Accession_key = ACC_AccessionReference._Accession_key
;

delete from ACC_Accession
using toDelete d
where d._Accession_key = ACC_Accession._Accession_key
;

delete from SEQ_Marker_Cache 
where _Refs_key = 64047
and _Organism_key = 1
;

