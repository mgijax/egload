select r._Accession_key
into #toDelete
from ACC_AccessionReference r, ACC_Accession a, MRK_Marker m
where r._Refs_key = 64047
and r._Accession_key = a._Accession_key
and a._MGIType_key = 2
and a._Object_key = m._Marker_key
and m._Organism_key = 1

create index idx1 on #toDelete(_Accession_key)

delete ACC_AccessionReference
from #toDelete d, ACC_AccessionReference r
where d._Accession_key = r._Accession_key

delete ACC_Accession
from #toDelete d, ACC_Accession r
where d._Accession_key = r._Accession_key

delete SEQ_Marker_Cache 
from SEQ_Marker_Cache c
where c._Refs_key = 64047
and c._Organism_key = 1

