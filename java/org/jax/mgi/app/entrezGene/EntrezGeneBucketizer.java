package org.jax.mgi.app.entrezGene;

import java.util.*;
import java.io.File;

import org.jax.mgi.dbs.mgd.lookup.ProblemClonesLookup;
import org.jax.mgi.dbs.mgd.lookup.EntrezGeneHistory;
import org.jax.mgi.dbs.mgd.lookup.GUIdsByMarkerKeyLookup;
import org.jax.mgi.dbs.mgd.lookup.MarkersByGUIdLookup;
import org.jax.mgi.dbs.rdr.query.EntrezGeneQuery.EntrezGene;
import org.jax.mgi.dbs.mgd.query.MGIMarkerQuery.MGIMarker;
import org.jax.mgi.dbs.mgd.query.NCBIGMQuery;
import org.jax.mgi.dbs.mgd.query.NCBIGMQuery.GM;
import org.jax.mgi.shr.bucketizer.AbstractBucketizer;
import org.jax.mgi.shr.bucketizer.BucketItem;
import org.jax.mgi.shr.bucketizer.BucketItem.Association;
import org.jax.mgi.shr.bucketizer.Bucketizable;
import org.jax.mgi.shr.exception.MGIException;
import org.jax.mgi.shr.ioutils.OutputManager;
import org.jax.mgi.shr.dbutils.DataIterator;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.dao.SQLStream;
import org.jax.mgi.shr.stringutil.Sprintf;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.shr.stringutil.StringLib;
import org.jax.mgi.dbs.mgd.AccessionLib;
import org.jax.mgi.dbs.mgd.LogicalDBConstants;
import org.jax.mgi.shr.config.EntrezGeneCfg;


/**
 * is an extension of the AbstractBucketizer which contains specific
 * functionality for 'bucketizing' EntrezGene and MGI marker data into
 * associated clusters and to process these clusters
 * @has
 * <UL>
 * <LI>An EntrezGene DataIterator and MGIMarker DataIterator</LI>
 * <LI>A set of attribute names used by the Bucketizer algorithm</LI>
 * <LI>A SQLStream for writing data to the database</LI>
 * <LI>An instance of the EntrezGeneHistory class for reporting previous</LI>
 * MGIMarker to EntrezGene associations
 * <LI> Several Lookups to help us determine when to make associations
 *      1) whether an EntrezGene Id has MGI GU association(s), 
 *      2) whether a Marker has MGI GU association(s)
 *      3) whether a sequence is associated with a problem clone
 *      4) whether there is an EntrezGene association to a Homologene Group ID
 * </UL>
 * @does processes the association clusters by creating report entries and
 * creating accession associations in the MGI database
 * @company Jackson Laboratory
 * @author M Walker
 */

public class EntrezGeneBucketizer extends AbstractBucketizer
{
    // static report aliases which correspond to the prefixes used in
    // the configuration file for configuring the OutputDataFiles
    public static String BUCKET_ONE_TO_ONE = "ONE_ONE";
    public static String BUCKET_ONE_TO_MANY = "ONE_N";
    public static String BUCKET_MANY_TO_ONE = "N_ONE";
    public static String BUCKET_MANY_TO_MANY = "N_M";
    public static String BUCKET_ONE_TO_ZERO = "ONE_ZERO";
    public static String BUCKET_ZERO_TO_ONE = "ZERO_ONE";
    public static String CHROMOSOME_MISMATCH = "CHR_MIS";
    public static String GM_NOTIN_ENTREZGENE = "GM_NOTIN";

    // a Configurator for configuring runtime aspects of the load
    private EntrezGeneCfg egCfg = null;

    // a SQLStream for writing to MGD
    private SQLStream loadStream = null;

    // A FullcachedLookup for obtaining problem clones
    private ProblemClonesLookup problemClones = null;

    // A FullCachedLookup for obtaining the set of markers for a GU id
    private MarkersByGUIdLookup markersByGUIdLookup = null;

    // A FullCachedLookup for obtaining the set of GU ids for a marker
    private GUIdsByMarkerKeyLookup guIdsByMarkerKeyLookup = null;

    // An FullCachedLookup for obtaining previous associations
    // between EntrezGenes and MGIMarkers
    private EntrezGeneHistory history = null;
    
    // The full set of egIds from EntrezGene
    HashSet egIdSet = null;

    // A Runtime instance for obtaining runtime memory usage
    private Runtime rtime = Runtime.getRuntime();

    /**
     * constructor
     * @param it1 A DataIterator for incoming data to be bucketized. Could be
     * EntrezGenes or MGIMarkers... it doesnt matter which one.
     * @param it2 A DataIterator for incoming data to be bucketized. Could be
     * EntrezGenes or MGIMarkers... it doesnt matter which one.
     * @param sequenceGroups a list of attribute names used for bucketizing
     * @param loadStream A SQLStream for loading data in MGD
     * @param history An instance of EntrezGeneHistory for reporting on
     * previous associations between EntrezGene and MGIMarkers
     * @throws MGIException thrown to represent any error
     */
    public EntrezGeneBucketizer(DataIterator it1, DataIterator it2,
                                String[] sequenceGroups,
                                SQLStream loadStream,
                                EntrezGeneHistory history) throws MGIException {
        super(it1, it2, sequenceGroups);
        this.history = history;
        this.loadStream = loadStream;
        this.egCfg = new EntrezGeneCfg();
        this.egCfg = new EntrezGeneCfg();

	// create all lookups and init caches upfront
        this.problemClones = new ProblemClonesLookup();
        this.problemClones.initCache();
        this.markersByGUIdLookup = new MarkersByGUIdLookup();
        this.markersByGUIdLookup.initCache();
	this.guIdsByMarkerKeyLookup = new GUIdsByMarkerKeyLookup();
	this.guIdsByMarkerKeyLookup.initCache();
	this.egIdSet = new HashSet();
    }

    /**
     * @assumes the bucketizer algorithm has been run
     * @effects nothing
     * @throws MGIException thrown to represent any error
     */
    public void postProcess() throws MGIException {
    }

    /**
     * processes the one (MGIMarker) to many (EG) associations 
     * discovered by the Bucketizer
     * @assumes nothing
     * @effects new report entries created
     *  may create sequence and Homologene Group Id associations to GU markers
     * @param bucketItem A BucketItem which stores the association data in 
     *  this case one MGIMarker object associated with many EntrezGene objects
     * @throws MGIException thrown to represent any error
     */
    public void process_One_To_Many(BucketItem bucketItem)
        throws MGIException {
	
	processNonOne_To_OneBucketItem(bucketItem);
        this.reportConnectedComponents(bucketItem, BUCKET_ONE_TO_MANY);
    }

    /**
     * process the zero (MGIMarker) to one (EG) associations 
     * discovered by the Bucketizer
     * @assumes nothing
     * @effects new report entries created
     * may create sequence and Homologene Group Id associations to GU markers
     * @param bucketItem A BucketItem which stores the association data
     * @throws MGIException thrown to represent any error
     */
    public void process_Zero_To_One(BucketItem bucketItem)
        throws MGIException {	
	
	processNonOne_To_OneBucketItem(bucketItem);
	this.reportUnConnectedComponents(bucketItem, BUCKET_ZERO_TO_ONE);
    }
    
    /**
     * processes the one (MGIMarker) to zero (EG)associations 
     * discovered by the Bucketizer
     * This marker may still be in the GU, but that marker GU association
     * will be processed in the bucket that contains that GU Id OR get
     * caught at the end if the GU Id is not in EntrezGene - see getInvalidGUIds()
     * @assumes nothing
     * @effects new report entries created
     * @param bucketItem A BucketItem which stores the association data
     * @throws MGIException thrown to represent any error
     */
    public void process_One_To_Zero(BucketItem bucketItem)
        throws MGIException {
        this.reportUnConnectedComponents(bucketItem, BUCKET_ONE_TO_ZERO);
    }

    /**
     * processes the many (MGIMarker to one (EG)associations 
     * discovered by the Bucketizer
     * @assumes nothing
     * @effects new report entries created     
     * may create sequence and Homologene Group Id associations to GU markers
     * @param bucketItem A BucketItem which stores the association data
     * @throws MGIException thrown to represent any error
     */
    public void process_Many_To_One(BucketItem bucketItem)
        throws MGIException {
	processNonOne_To_OneBucketItem(bucketItem);
        this.reportConnectedComponents(bucketItem, BUCKET_MANY_TO_ONE);
    }

    /**
     * processes the many (MGIMarker) to many (EG) associations 
     * discovered by the Bucketizer
     * @assumes nothing
     * @effects new report entries created
     * may create sequence and Homologene Group Id associations to GU markers
     * @param bucketItem A BucketItem which stores the association data
     * @throws MGIException thrown to represent any error
     */
    public void process_Many_To_Many(BucketItem bucketItem)
        throws MGIException{
	processNonOne_To_OneBucketItem(bucketItem);
        this.reportConnectedComponents(bucketItem, BUCKET_MANY_TO_MANY);
    }

    /**
     * processes the one to one associations discovered by the Bucketizer
     * only creates EntrezGene associations where there are no GU associations and
     * the EntrezGene chromosome matches to MGI Marker chromosome.
     * If there are GU associations migrates EntrezGene sequences and Homologene
     * group ids to GU markers when GU/Marker association is 1:1
     * @assumes nothing
     * @effects new report entries created and new ACC_Accession and
     * ACC_AccessionReference records created in a database. A
     * post check is made to assure that the chromosomes match
     * @param bucketItem A BucketItem which stores the association data which
     * contains one MGI marker and one Entrez gene
     * @throws MGIException thrown to represent any error
     */
    public void process_One_To_One(BucketItem bucketItem)
        throws MGIException{
        Iterator it = bucketItem.associationsIterator(Constants.PROVIDER_MGI);

	// since this is the one-to-one bucket there is only one association
        Association assoc = (Association) it.next();

	// get the mgiMarker object and its marker key; for the marker 
	// association from EntrezGene
        MGIMarker mgiMarker =
            (MGIMarker) assoc.getMember(Constants.PROVIDER_MGI);
	Integer egMarkerKey = mgiMarker.key;

	// get the set of GU Ids with which this marker is associated
	HashSet guIdsAssocWithEGMarker = guIdsByMarkerKeyLookup.lookup(egMarkerKey);

	// get the entrezGene object and the entrezgene Id
        EntrezGene entrezGene =
            (EntrezGene)assoc.getMember(Constants.PROVIDER_ENTREZGENE);
	String egId = entrezGene.getId();
	
	// add to the set of egIds (for later processing
	egIdSet.add(egId);
	    
	// get the set of GU Markers with which this egId is associated
	HashSet guMarkersAssocWithEgId = this.markersByGUIdLookup.lookup(egId);

	// if there are gu associations for 'egId' process accordingly
	// and we are done
	if(guMarkersAssocWithEgId != null) {
	    // add this item to the one-to-one bucket report regardless
	    // that there is a gu association
	    this.reportConnectedComponents(bucketItem, BUCKET_ONE_TO_ONE);
	    //System.out.println("In process_One_To_One getting ready to call process_GU");
	    process_GU(entrezGene, guMarkersAssocWithEgId);
	    return;
	}	

	/**
	 * If we get this far we will process the EntrezGene/Marker association
	 * if there is a chromosome match AND the marker doesn't have a GU 
	 * association
	 */

        // determine if there is a chr match, 
	// if not, report to the mismatched chromosome bucket and return
        if (!mgiMarker.chromosome.equals(entrezGene.getChromosome()) &&
            !mgiMarker.chromosome.equals("UN") &&
            !entrezGene.getChromosome().equals("UN")) {
            this.reportConnectedComponents(bucketItem, CHROMOSOME_MISMATCH);
            // mismatch found...exit so as not to create entrezgene associations
            return;
        }

	// if marker has GU association(s) we don't want to make any EntrezGene
	// sequence associations to that marker - should we report this?
	else if (guIdsAssocWithEGMarker != null) {
	    // add this item to the one-to-one bucket report regardless
	    // that there is a gu association to the marker
	    this.reportConnectedComponents(bucketItem, BUCKET_ONE_TO_ONE);
	    return;
	}

	/**
	 * If we get this far:
	 * 1) write to the 1:1 bucket report
	 * 2) create a new EntrezGene association to the MGIMarker in DB
	 * 3) associate EntrezGene GenBank RNA sequences and RefSeq sequences 
	 *    to the marker if marker has no GU association
	 */
	
	    // add this item to the one-to-one bucket report
	    this.reportConnectedComponents(bucketItem, BUCKET_ONE_TO_ONE);
 
	    // create a new EntrezGene association to the MGIMarker in DB
	    AccessionLib.createMarkerAssociation(
		new Integer(LogicalDBConstants.ENTREZ_GENE),
		entrezGene.getId(), egMarkerKey,
		new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
	    createAssociations(entrezGene, egMarkerKey);
    }
    /**
     * Report all NCBI Gene Models in MGI that are not in EntrezGene 
     * regardless of whether they have marker association).
     */
     public void getInvalidGMIds() throws MGIException {
	NCBIGMQuery query = new NCBIGMQuery();
	DataIterator it = query.execute();
	//String header = "NCBI GM ID\tComma delimited list of Marker MGI IDs\n";
	//OutputManager.write(GM_NOTIN_ENTREZGENE, header);
	while (it.hasNext()) {
	    GM gm = (GM)it.next();
	    String gmId = gm.getGMId();
	    HashSet markers = gm.getMarkers();
	    if (!egIdSet.contains(gmId)) {
		StringBuffer output = new StringBuffer();
		output.append(gmId);
		output.append("\t");
		for (Iterator i = markers.iterator(); i.hasNext();) {
		    String m = (String)i.next();
		    if (m != null) {
			output.append(m);
			output.append(",");
		    }
		}
		if (output.charAt(output.length()-1) == ',') {
		    output.deleteCharAt(output.length()-1);
		}
                output.append("\n");
		//report it
		OutputManager.write(GM_NOTIN_ENTREZGENE, output.toString());
	    }
	}
    }
    /**
    * All Non-One_To_One buckets (except for One_To_Zero) are processed the same
    * If there are GU associations, pass on to process_GU method
    * @assumes nothing
    * @effects Sequences associations to GU markers may be created, ldb 55
    * associations will be created.
    * @param bucketItem BucketItem containing EntrezGene and MGIMarker associations
    * however:
    * if 'bucketItem' zero-to-one cardinality, it will have no MGIMarker object
    * if 'bucketItem' one-to-zero cardinality, it will have no EntrezGene object
    * EntrezGene objects may have GU associations 
    * @throws nothing
    */
    private void processNonOne_To_OneBucketItem(BucketItem bucketItem) 
        throws MGIException {
	
	// Iterate over individual members looking for EntrezGene members
	for (Iterator i = bucketItem.membersIterator(); i.hasNext();) { 
	    Bucketizable b = (Bucketizable)i.next();
            if (b.getProvider().equals(Constants.PROVIDER_ENTREZGENE)) {
	    
		// get the entrezGene object and the entrezgene Id
		EntrezGene entrezGene = (EntrezGene)b;    
		String egId = entrezGene.getId();
		
		// add to the set of egIds (for later processing
		egIdSet.add(egId);
	    
		// get the set of GU Markers with which this egId is associated
		HashSet guMarkersAssocWithEgId = this.markersByGUIdLookup.lookup(egId);
		// if there are gu associations for 'egId' process accordingly
		if(guMarkersAssocWithEgId != null) {
		    process_GU(entrezGene, guMarkersAssocWithEgId);
		}
	    }
	}
    }
    
    /**
     * associate EntrezGene sequences and Homologene group ids to a marker
     * For each GenBank and RefSeq sequence in 'entrezGene'
     * associate it with 'markerKey' if:
     * 1) GenBank, only RNAs
     * 2) Sequence not already associated with another Marker in MGI
     *     We determine this by getting the index entry for each seqId.
     *     a) if there is only one Bucketizable object associated with seqId
     *        in the index, it is an EntrezGene object (i.e. not marker assoc)
     *        so associate the seqId with 'markerKey'
     *     b) There can be at most two Bucketizable objects associated with
     *        the seqId because the bucketizer has excluded all  ids associated 
     *        with > 1 marker from bucketizing consideration
     *	   c) if there are two Bucektizable objects associated with the seqId
     *        then there is one EntrezGene and one MGIMarker object indicating
     *        this sequence is associated with a marker, so we don't want
     *        to associate it with 'markerKey'
     * @assumes nothing
     * @effects Sequences associations to 'markerKey' may be created.
     * @param entrezGene EntrezGene object whose sequences we may want to 
     *        associated with 'markerKey' and whose Homologene group Id 
     *        (if present) we will associate with 'markerKey'
     * @param markerKey the marker to which we may associate sequences and
     *        to which we will associate Homologene group id (if present)
     * @throws nothing
     */
    private void createAssociations(EntrezGene entrezGene, 
		Integer markerKey) throws MGIException {
	for (Iterator i = entrezGene.getGenBankSeqs().iterator();
	     i.hasNext();) {
	    SequenceAccession acc = (SequenceAccession)i.next();
	    String accid = acc.getAccid();
	    if (acc.getType() == SequenceAccession.RNA) {
		// get set of Bucketizables associated with this seqId
		Set seqAssociations =
                        (Set)super.index.lookup(Constants.GENBANK, acc);
		/**
		 * if only one member, it is an EntrezGene object with no 
		 * corresponding MGIMarker object. This sequence is not associated with
		 * any so we want to associate
		 * the sequence with the marker
		 */
		if (seqAssociations.size() == 1) {
		    makeSeqAssociationToMarker (new Integer(
		    LogicalDBConstants.SEQUENCE),
                        accid, markerKey);
		}
	    }
	}
	/**
	 * associate EntrezGene refseq sequences to the marker,
	 * XMs, XRs, XPs, NMs, NRs, NPs, NGs, NTs, NWs
	 */

	for (Iterator i = entrezGene.getXMs().iterator(); i.hasNext();) {
	    SequenceAccession acc = (SequenceAccession)i.next();
	    String accid = acc.getAccid();
	    Set seqAssociations =
		(Set)super.index.lookup(Constants.XM, acc);
	    if (seqAssociations.size() == 1) {
		makeSeqAssociationToMarker (new Integer(
		LogicalDBConstants.REFSEQ),
                        accid, markerKey);
	    }
	}

	for (Iterator i = entrezGene.getXRs().iterator(); i.hasNext();) {
	    SequenceAccession acc = (SequenceAccession)i.next();
	    String accid = acc.getAccid();
	    Set seqAssociations =
		(Set)super.index.lookup(Constants.XR, acc);
	    if (seqAssociations.size() == 1) {
		makeSeqAssociationToMarker (new Integer(
		LogicalDBConstants.REFSEQ),
                        accid, markerKey);
            }
	}

	for (Iterator i = entrezGene.getXPs().iterator(); i.hasNext();) {
	    SequenceAccession acc = (SequenceAccession)i.next();
	    String accid = acc.getAccid();
	     Set seqAssociations =
                (Set)super.index.lookup(Constants.XP, acc);
	    if (seqAssociations.size() == 1) {
		 makeSeqAssociationToMarker (new Integer(
		LogicalDBConstants.REFSEQ),
                        accid, markerKey);
            } 
	}

	for (Iterator i = entrezGene.getNMs().iterator(); i.hasNext();) {
	    SequenceAccession acc = (SequenceAccession)i.next();
	    String accid = acc.getAccid();
	    Set seqAssociations =
                (Set)super.index.lookup(Constants.NM, acc);
	    if (seqAssociations.size() == 1) {
		makeSeqAssociationToMarker (new Integer(
		LogicalDBConstants.REFSEQ),
                        accid, markerKey);
            }
	}

	for (Iterator i = entrezGene.getNRs().iterator(); i.hasNext();) {
	    SequenceAccession acc = (SequenceAccession)i.next();
	    String accid = acc.getAccid();
	    Set seqAssociations =
                (Set)super.index.lookup(Constants.NR, acc);
	    if (seqAssociations.size() == 1) {
		makeSeqAssociationToMarker (new Integer(
		LogicalDBConstants.REFSEQ),
                        accid, markerKey);
            }
	}

	for (Iterator i = entrezGene.getNPs().iterator(); i.hasNext();) {
	    SequenceAccession acc = (SequenceAccession)i.next();
	    String accid = acc.getAccid();
	    Set seqAssociations =
                (Set)super.index.lookup(Constants.NP, acc);
	    if (seqAssociations.size() == 1) {
		makeSeqAssociationToMarker (new Integer(
		LogicalDBConstants.REFSEQ),
                        accid, markerKey);
            }
	}

	for (Iterator i = entrezGene.getNGs().iterator(); i.hasNext();) {
	    SequenceAccession acc = (SequenceAccession)i.next();
	    String accid = acc.getAccid();
	    Set seqAssociations =
                (Set)super.index.lookup(Constants.NG, acc);
	    makeSeqAssociationToMarker (new Integer(
	    LogicalDBConstants.REFSEQ),
		    accid, markerKey);
	}
        for (Iterator i = entrezGene.getNTs().iterator(); i.hasNext();) {
            SequenceAccession acc = (SequenceAccession)i.next();
            String accid = acc.getAccid();
	    //System.out.println("EntrezGeneBucketizer egID: " + entrezGene.getId() + " Contig: " + accid);
            Set seqAssociations =
                (Set)super.index.lookup(Constants.NT, acc);
	    //System.out.println("seqAssociations.size()= " + seqAssociations.size());
            //System.out.println("Associating " + accid);
	    // TR9773 - do not restrict NT to associations with only one marker
	    makeSeqAssociationToMarker (new Integer(
		LogicalDBConstants.NCBI_GENEMODEL_EVIDENCE), accid, markerKey);
        }
        for (Iterator i = entrezGene.getNWs().iterator(); i.hasNext();) {
            SequenceAccession acc = (SequenceAccession)i.next();
            String accid = acc.getAccid();
            Set seqAssociations =
                (Set)super.index.lookup(Constants.NW, acc);
	    // TR9773 - do not restrict NW to associations with only one marker
	    makeSeqAssociationToMarker (new Integer(
		LogicalDBConstants.NCBI_GENEMODEL_EVIDENCE), accid, markerKey);
        }
    }

     /**
     * associate a sequence ID with a marker if the sequence is not associated
     * with a problem clone. Problem Clone lookup only applies to therefore
     * only contains genbank sequences, for simplicity we apply all seqids
     * to this lookup. RefSeqs will not be found.
     * @assumes nothing
     * @effects 'seqID' may be associated with 'markerKey' in the database
     * @param logicalDBKey - ldb with which to make the sequence to marker
     *  association
     * @param seqID - id to associate with 'markerKey'
     * @param markerKey the marker to which we may associate 'seqID'
     * @throws nothing
     */
    private void makeSeqAssociationToMarker(Integer logicalDBKey, String seqID, 
	    Integer markerKey) throws MGIException {
	if ( ! this.problemClones.lookup(seqID)) {
	    AccessionLib.createMarkerAssociation(
                        logicalDBKey, seqID, markerKey,
                        new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
	}
    }
				    
    /**
     * processes the gene unification associations
     * @assumes 'guMarkers' contains at least one member
     * @effects new report entries created and new ACC_Accession and
     * ACC_AccessionReference records created in a database.
     * @throws MGIException thrown to represent any error
     */
    private void process_GU(EntrezGene entrezGene, HashSet guMarkers)
        throws MGIException {
	//System.out.println("In process_GU");
	// there will be at least one marker in 'markers'
	Integer guMarkerKey = null;
	
	// Create EntrezGene association (ldbKey 55) for GU association
	// This is needed by the WI
	for (Iterator i = guMarkers.iterator(); i.hasNext();) {
	    guMarkerKey = (Integer)i.next();
	    
	    AccessionLib.createMarkerAssociation(new Integer(
		LogicalDBConstants.ENTREZ_GENE), 
		entrezGene.getId(), guMarkerKey,
		new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
	}
	
	// For GU 1:1s only associate 'entrezGene' GenBank, RefSeq sequences
	// and Homologene group id with 'guMarkerKey'
	if (guMarkers.size() == 1) {
	    //System.out.println("Associating GenBank, RefSeq, and Homologene because GU one-to-one has been determined");
	    // get the set of GU Ids with which this marker is associated
	    HashSet guIdsAssocWithMarker = guIdsByMarkerKeyLookup.lookup(guMarkerKey);
	    // create sequence associations if GU 1:1
	    if (guIdsAssocWithMarker.size() == 1) {
		 createAssociations(entrezGene, guMarkerKey);
	    }
	}	    
    }


    /**
     * create an entry in one of the bucket reporting some association between
     * a MGIMarker and EntrezGene
     * @assumes nothing
     * @effects a new rport entry will be made to the given report
     * @param item the BucketItem representing the association
     * @param reportAlias the report alias to write to
     * @throws MGIException thrown to represent any error
     */
    private void reportConnectedComponents(BucketItem item,
                                           String reportAlias)
    throws MGIException {
        for (Iterator i =
             item.associationsIterator(Constants.PROVIDER_MGI); i.hasNext();)
        {
            Association assoc = (Association)i.next();
            MGIMarker marker =
                (MGIMarker)assoc.getMember(Constants.PROVIDER_MGI);
            EntrezGene egene =
                (EntrezGene)assoc.getMember(Constants.PROVIDER_ENTREZGENE);

            Vector fields = new Vector();

            fields.add(marker.mgiID == null ? "" : marker.mgiID);
            fields.add(marker.symbol == null ? "" : marker.symbol);
            fields.add(marker.chromosome == null ? "" : marker.chromosome);
            fields.add(egene.getId() == null ? "" : egene.getId());
            fields.add(egene.getSymbol() == null ? "" : egene.getSymbol());
            fields.add(egene.getChromosome() == null ? "" :
                       egene.getChromosome());
            fields.add(assoc.getLabel().toString());
            fields.add(marker.type);

            String output = null;

            if (egCfg.getOkToPerformHistory().booleanValue())
            {
                String oldEgId =
                    this.history.lookupEGeneID(marker.mgiID);
                String oldMgiId =
                    this.history.lookupMGIID(egene.getId());
                fields.add(oldEgId == null ? "None" : oldEgId);
                fields.add(oldMgiId == null ? "None" : oldMgiId);

                output = Sprintf.sprintf(
                    "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", fields);
            }
            else
                output = Sprintf.sprintf(
                    "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", fields);
            OutputManager.writeln(reportAlias, output);
        }
    }

    /**
     * create an entry in one of the bucket reporting a one to zero or a zero
     * to one relationship
     * @assumes nothing
     * @effects a new rport entry will be made to the given report
     * @param item the BucketItem representing the association
     * @param reportAlias the report alias to write to
     * @throws MGIException thrown to represent any error
     */
    private void reportUnConnectedComponents(BucketItem item,
                                             String reportAlias)
    throws MGIException {
        Vector fields = new Vector();
        for (Iterator i = item.membersIterator(); i.hasNext();)
        {
            Bucketizable b = (Bucketizable)i.next();
            String output = null;
            if (b.getProvider().equals(Constants.PROVIDER_MGI))
            {
                MGIMarker marker = (MGIMarker)b;
                fields.add(marker.mgiID == null ? "" : marker.mgiID);
                fields.add(marker.symbol == null ? "" : marker.symbol);
                fields.add(marker.chromosome == null ? "" :
                           marker.chromosome);
                fields.add(marker.svaString());
                fields.add(marker.type);
                if (this.egCfg.getOkToPerformHistory().booleanValue())
                {
                    String oldEgId =
                        this.history.lookupEGeneID(marker.mgiID);
                    fields.add(oldEgId == null ? "None" : oldEgId);
                    output = Sprintf.sprintf("%s\t%s\t%s\t%s\t%s\t%s",
                                             fields);
                }
                else
                    output = Sprintf.sprintf("%s\t%s\t%s\t%s\t%s", fields);
            }
            else
            {
                EntrezGene egene = (EntrezGene)b;
                fields.add(egene.getId() == null ? "" : egene.getId());
                fields.add(egene.getSymbol() == null ? "" :
                           egene.getSymbol());
                fields.add(egene.getChromosome() == null ? "" :
                           egene.getChromosome());
                fields.add(egene.svaString());
                if (this.egCfg.getOkToPerformHistory().booleanValue())
                {
                    String oldMgiId =
                        this.history.lookupEGeneID(egene.getId());
                    fields.add(oldMgiId == null ? "None" : oldMgiId);
                    output = Sprintf.sprintf("%s\t%s\t%s\t%s\t%s", fields);
                }
                else
                    output = Sprintf.sprintf("%s\t%s\t%s\t%s", fields);
            }

            OutputManager.writeln(reportAlias, output);
        }
    }
}
