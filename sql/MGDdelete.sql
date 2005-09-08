select r._Accession_key
into #toDelete
from ACC_AccessionReference r, ACC_Accession a, MRK_Marker m
where r._Refs_key = 64047
and a._Accession_key = r._Accession_key
and a._Object_key = m._Marker_key
and m._Organism_key = 1
and a._MGIType_key = 2

create index idx1 on #toDelete(_Accession_key)

delete ACC_AccessionReference
from #toDelete d, ACC_AccessionReference r
where d._Accession_key = r._Accession_key

delete ACC_Accession
from #toDelete d, ACC_Accession r
where d._Accession_key = r._Accession_key

delete SEQ_Marker_Cache 
from SEQ_Marker_Cache c, MRK_Marker m
where c._Refs_key = 64047
and c._Marker_key = m._Marker_key
and m._Organism_key = 1

select r._Accession_key
into #toDeleteSP
from ACC_AccessionReference r, ACC_Accession a, MRK_Marker m
where r._Refs_key = 53672
and a._Accession_key = r._Accession_key
and a._LogicalDB_key = 9
and a._MGIType_key = 2
and m._Marker_key = a._Object_key 
and m._Organism_key = 1

create index idx1 on #toDeleteSP(_Accession_key)

delete ACC_AccessionReference
from #toDeleteSP d, ACC_AccessionReference r
where d._Accession_key = r._Accession_key

delete ACC_Accession
from #toDeleteSP d, ACC_Accession r
where d._Accession_key = r._Accession_key

delete SEQ_Marker_Cache
from SEQ_Marker_Cache c, MRK_Marker m
where c._Refs_key = 53672
and c._Marker_key = m._Marker_key
and m._Organism_key = 1
