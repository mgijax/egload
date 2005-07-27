select r._Accession_key
into #toDelete
from ACC_AccessionReference r
where r._Refs_key = 64047

create index idx1 on #toDelete(_Accession_key)

delete ACC_AccessionReference
from #toDelete d, ACC_AccessionReference r
where d._Accession_key = r._Accession_key

delete ACC_Accession
from #toDelete d, ACC_Accession r
where d._Accession_key = r._Accession_key

delete SEQ_Marker_Cache where _Refs_key = 64047

select r._Accession_key
into #toDeleteSP
from ACC_AccessionReference r, ACC_Accession a
where r._Refs_key = 53672
and a._accession_key = r._accession_key
and a._logicaldb_key = 9
and a._mgitype_key = 2

create index idx1 on #toDeleteSP(_Accession_key)

delete ACC_AccessionReference
from #toDeleteSP d, ACC_AccessionReference r
where d._Accession_key = r._Accession_key

delete ACC_Accession
from #toDeleteSP d, ACC_Accession r
where d._Accession_key = r._Accession_key

delete SEQ_Marker_Cache where _Refs_key = 53672
