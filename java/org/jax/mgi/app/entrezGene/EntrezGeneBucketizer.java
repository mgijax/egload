package org.jax.mgi.app.entrezGene;

import java.util.*;
import java.io.File;


import org.jax.mgi.dbs.mgd.lookup.ProblemClonesLookup;
import org.jax.mgi.dbs.mgd.lookup.EntrezGeneHistory;
import org.jax.mgi.dbs.mgd.lookup.GUIdsByMarkerKeyLookup;
import org.jax.mgi.dbs.mgd.lookup.GUIdsLookup;
import org.jax.mgi.dbs.rdr.lookup.HomoloGeneLookup;
import org.jax.mgi.dbs.rdr.query.EntrezGeneQuery.EntrezGene;
import org.jax.mgi.dbs.mgd.query.MGIMarkerQuery.MGIMarker;
import org.jax.mgi.dbs.mgd.query.GUQuery;
import org.jax.mgi.dbs.mgd.query.GUQuery.GU;
import org.jax.mgi.shr.bucketizer.AbstractBucketizer;
import org.jax.mgi.shr.bucketizer.BucketItem;
import org.jax.mgi.shr.bucketizer.BucketItem.Association;
import org.jax.mgi.shr.bucketizer.Bucketizable;
import org.jax.mgi.shr.sva.SVASet;
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
 * functionality for 'bucketizing' Entrez Gene and MGI marker data into
 * associated clusters and to process these clusters
 * @has
 * <UL>
 * <LI> A EntrezGene DataIterator and MGIMarker DataIterator</LI>
 * <LI>A set of attribute names used by the Bucketizer algorithm</LI>
 * <LI>A SQLStream for writing data to the database</LI>
 * <LI>An instance of the EntrezGeneHistory class for reporting previous</LI>
 * MGIMarker to EntrezGene associations
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

    // a Configurator for configuring runtime aspects of the load
    private EntrezGeneCfg egCfg = null;

    // a SQLStream for writing to MGD
    private SQLStream loadStream = null;

    // A FullcachedLookup for obtainning problem clones
    private ProblemClonesLookup problemClones = null;

    // A FullCachedLookup for obtaining GU IDs
    private GUIdsLookup guLookup = null;

    // A FullCachedLookup for obtaining HomoloGene IDs
    private HomoloGeneLookup homoloGeneLookup = null;

    // An FullCachedLookup for obtaining previous associations
    // between EntrezGenes and MGIMarkers
    private EntrezGeneHistory history = null;

    // A Runtime instance for obtaining runtime memory usage
    private Runtime rtime = Runtime.getRuntime();

    // a HashMap to map egID to its EntrezGene object
    private HashMap egMap = null;

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
                                EntrezGeneHistory history)
    throws MGIException
    {
        super(it1, it2, sequenceGroups);
        this.history = history;
        this.loadStream = loadStream;
        this.egCfg = new EntrezGeneCfg();
        this.egCfg = new EntrezGeneCfg();
        this.problemClones = new ProblemClonesLookup();
        this.problemClones.initCache();
        this.guLookup = new GUIdsLookup();
        this.guLookup.initCache();
        this.homoloGeneLookup = new HomoloGeneLookup();
        this.homoloGeneLookup.initCache();
	this.egMap = new HashMap();
    }

    /**
     * @assumes the bucketizer algorithm has been run
     * @effects nothing
     * @throws MGIException thrown to represent any error
     */
    public void postProcess() throws MGIException
    {
    }

    /**
     * processes the one to many associations discovered by the Bucketizer
     * @assumes nothing
     * @effects new report entries created
     * @param bucketItem A BucketItem which stores the association data
     * @throws MGIException thrown to represent any error
     */
    public void process_One_To_Many(BucketItem bucketItem)
        throws MGIException
    {
        this.reportConnectedComponents(bucketItem, BUCKET_ONE_TO_MANY);
	this.addEgToMap(bucketItem);
    }

    /**
     * processes the zero to one associations discovered by the Bucketizer
     * @assumes nothing
     * @effects new report entries created
     * @param bucketItem A BucketItem which stores the association data
     * @throws MGIException thrown to represent any error
     */
    public void process_Zero_To_One(BucketItem bucketItem)
        throws MGIException
    {
        this.reportUnConnectedComponents(bucketItem, BUCKET_ZERO_TO_ONE);
	this.addEgToMap(bucketItem);
    }

    /**
     * processes the one to zero associations discovered by the Bucketizer
     * @assumes nothing
     * @effects new report entries created
     * @param bucketItem A BucketItem which stores the association data
     * @throws MGIException thrown to represent any error
     */
    public void process_One_To_Zero(BucketItem bucketItem)
        throws MGIException
    {
        this.reportUnConnectedComponents(bucketItem, BUCKET_ONE_TO_ZERO);
	this.addEgToMap(bucketItem);
    }

    /**
     * processes the many to one associations discovered by the Bucketizer
     * @assumes nothing
     * @effects new report entries created
     * @param bucketItem A BucketItem which stores the association data
     * @throws MGIException thrown to represent any error
     */
    public void process_Many_To_One(BucketItem bucketItem)
        throws MGIException
    {
        this.reportConnectedComponents(bucketItem, BUCKET_MANY_TO_ONE);
	this.addEgToMap(bucketItem);
    }

    /**
     * processes the many to many associations discovered by the Bucketizer
     * @assumes nothing
     * @effects new report entries created
     * @param bucketItem A BucketItem which stores the association data
     * @throws MGIException thrown to represent any error
     */
    public void process_Many_To_Many(BucketItem bucketItem)
        throws MGIException
    {
        this.reportConnectedComponents(bucketItem, BUCKET_MANY_TO_MANY);
	this.addEgToMap(bucketItem);
    }

    /**
     * processes the one to one associations discovered by the Bucketizer
     * @assumes nothing
     * @effects new report entries created and new ACC_Accession and
     * ACC_AccessionReference records created for storing the database. A
     * post check is made to assure that the chromosomes match
     * @param bucketItem A BucketItem which stores the association data which
     * contains one MGI marker and one Entrez gene
     * @throws MGIException thrown to represent any error
     */
    public void process_One_To_One(BucketItem bucketItem)
        throws MGIException
    {
        Iterator it = bucketItem.associationsIterator(Constants.PROVIDER_MGI);
        Association assoc = (Association) it.next();
        MGIMarker mgiMarker =
            (MGIMarker) assoc.getMember(Constants.PROVIDER_MGI);
        EntrezGene entrezGene =
            (EntrezGene)assoc.getMember(Constants.PROVIDER_ENTREZGENE);

        this.addEgToMap(bucketItem);

        // have to match on chromosomes unless one is undetermined.
        // report to either one to one bucket or the mismatched
        // chromosome bucket

        if (!mgiMarker.chromosome.equals(entrezGene.getChromosome()) &&
            !mgiMarker.chromosome.equals("UN") &&
            !entrezGene.getChromosome().equals("UN"))
        {
            this.reportConnectedComponents(bucketItem, CHROMOSOME_MISMATCH);
            // mismatch found...exit so as not to create new accession records
            return;
        }
        else
        {
            this.reportConnectedComponents(bucketItem, BUCKET_ONE_TO_ONE);
        }

	/**
	 * create a new EntrezGene association to the MGIMarker in DB
	 * if the EntrezGene id is not already associated with a GU id
	 * associate entrez gene genbank sequences to the marker
         * associate entrez gene refseq sequences to the marker
	 */

	String guID = this.guLookup.lookup(entrezGene.getId());

	if (guID == null)
	{
            /**
             * create a new EntrezGene association to the MGIMarker in DB
             */
    
            AccessionLib.createMarkerAssociation(
                new Integer(LogicalDBConstants.ENTREZ_GENE),
                entrezGene.getId(), mgiMarker.key,
                new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);

            /**
             * associate entrez gene genbank sequences to the marker
             */
            for (Iterator i = entrezGene.getGenBankSeqs().iterator();
                 i.hasNext();)
            {
                SequenceAccession acc = (SequenceAccession)i.next();
                String accid = acc.getAccid();
                if (acc.getType() == SequenceAccession.RNA)
                {
                    Set seqAssociations =
                        (Set)super.index.lookup(Constants.GENBANK, acc);
                    makeAssociationToMarker(accid, mgiMarker, seqAssociations,
                                        LogicalDBConstants.SEQUENCE);
                }
            }

            /**
             * associate entrez gene refseq sequences to the marker,
             * XMs, XRs, XPs, NMs, NRs, NPs, NGs
             */

            for (Iterator i = entrezGene.getXMs().iterator(); i.hasNext();)
            {
                SequenceAccession acc = (SequenceAccession)i.next();
                String accid = acc.getAccid();
                Set seqAssociations =
                    (Set)super.index.lookup(Constants.XM, acc);
                makeAssociationToMarker(accid, mgiMarker, seqAssociations,
                                    LogicalDBConstants.REFSEQ);
            }

            for (Iterator i = entrezGene.getXRs().iterator(); i.hasNext();)
            {
                SequenceAccession acc = (SequenceAccession)i.next();
                String accid = acc.getAccid();
                Set seqAssociations =
                    (Set)super.index.lookup(Constants.XR, acc);
                makeAssociationToMarker(accid, mgiMarker, seqAssociations,
                                    LogicalDBConstants.REFSEQ);
            }

            for (Iterator i = entrezGene.getXPs().iterator(); i.hasNext();)
            {
                SequenceAccession acc = (SequenceAccession)i.next();
                String accid = acc.getAccid();
                makeAssociationToMarker(accid, mgiMarker, null,
                                    LogicalDBConstants.REFSEQ);
            }

            for (Iterator i = entrezGene.getNMs().iterator(); i.hasNext();)
            {
                SequenceAccession acc = (SequenceAccession)i.next();
                String accid = acc.getAccid();
                makeAssociationToMarker(accid, mgiMarker, null,
                                    LogicalDBConstants.REFSEQ);
            }

            for (Iterator i = entrezGene.getNRs().iterator(); i.hasNext();)
            {
                SequenceAccession acc = (SequenceAccession)i.next();
                String accid = acc.getAccid();
                makeAssociationToMarker(accid, mgiMarker, null,
                                    LogicalDBConstants.REFSEQ);
            }

            for (Iterator i = entrezGene.getNPs().iterator(); i.hasNext();)
            {
                SequenceAccession acc = (SequenceAccession)i.next();
                String accid = acc.getAccid();
                makeAssociationToMarker(accid, mgiMarker, null,
                                    LogicalDBConstants.REFSEQ);
            }

            for (Iterator i = entrezGene.getNGs().iterator(); i.hasNext();)
            {
                SequenceAccession acc = (SequenceAccession)i.next();
                String accid = acc.getAccid();
                makeAssociationToMarker(accid, mgiMarker, null,
                                    LogicalDBConstants.REFSEQ);
            }
        }

        /**
        * associate HomoloGene ID to marker
        */

        String homolGeneGroupID = this.homoloGeneLookup.lookup(entrezGene.getId());

        if (homolGeneGroupID != null)
	{
           AccessionLib.createMarkerAssociation(
               new Integer(LogicalDBConstants.HOMOLOGENE),
               homolGeneGroupID, mgiMarker.key,
               new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
        }
    }

    /**
     * processes the gene unification associations
     * @assumes nothing
     * @effects new report entries created and new ACC_Accession and
     * ACC_AccessionReference records created for storing the database.
     * @throws MGIException thrown to represent any error
     */
    public void process_GU()
        throws MGIException
    {
	// lookup of existing GU associations
        GUQuery guQuery = new GUQuery();

	// lookup of same GU associations in the opposite order
        GUIdsByMarkerKeyLookup guByMarker = new GUIdsByMarkerKeyLookup();

        DataIterator guData = guQuery.execute();
	GU x = null;
	String guId;
	HashMap markers;

	while (guData.hasNext()) {
	    x = (GU) guData.next();
	    guId = x.getGUId();
	    markers = x.getMarkers();

	    //
	    // create ldb 55 association between GU and all markers
	    //
    
	    Iterator iterator1 = markers.keySet().iterator();
	    while (iterator1.hasNext()) {
		Integer markerKey = (Integer) iterator1.next();
                AccessionLib.createMarkerAssociation(
                    new Integer(LogicalDBConstants.ENTREZ_GENE),
                    guId, markerKey,
                    new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
	    }

	    // if the GU is associated with only one marker

	    if (markers.size() == 1) {

		// check if marker/guId in guByMarker == guId/marker in guData

	        Iterator iterator2 = markers.keySet().iterator();
	        while (iterator2.hasNext()) {
		    Integer markerKey = (Integer) iterator2.next();
                    HashSet guMarkers = guByMarker.lookup(markerKey);

		    // if there is a one-to-one between guData and guByMarker...

		    if (guMarkers.size() == 1 && guMarkers.contains(guId)) {

			// if the GU id is in the egMap, then assign the RefSeq/GenBank associations

	                EntrezGene eg = (EntrezGene) egMap.get(guId);
			if (eg != null) {

			    // get all sequences
			    HashMap allSequencesSet = (HashMap) markers.get(markerKey);

			    // get genbank set
			    HashSet genbankSet = (HashSet) allSequencesSet.get("GenBank");

                            for (Iterator i = eg.getGenBankSeqs().iterator();i.hasNext();) {

                                SequenceAccession acc = (SequenceAccession)i.next();
                                String accid = acc.getAccid();

				// if sequence set is null, continue
				// if sequence exists in GU, then continue to next sequence

				if (genbankSet != null) {
				    if (genbankSet.contains(accid)) {
				        continue;
				    }
				}

				// else, add the association

				if (acc.getType() == SequenceAccession.RNA) {
                                    AccessionLib.createMarkerAssociation(
                                        new Integer(LogicalDBConstants.SEQUENCE),
                                        accid, markerKey, 
				        new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
				}
                            }

                            // associate entrez gene refseq sequences to the marker,
                            // XMs, XRs, XPs, NMs, NRs, NPs, NGs

			    // get refseq set
			    HashSet refseqSet = (HashSet) allSequencesSet.get("RefSeq");

			    for (Iterator i = eg.getXMs().iterator();i.hasNext();)
                            {
                                SequenceAccession acc = (SequenceAccession)i.next();
                                String accid = acc.getAccid();

				// if sequence set is null, continue
				// if sequence exists in GU, then continue to next sequence

				if (refseqSet != null) {
				     if (refseqSet.contains(accid)) {
				         continue;
				     }
				}

				// else, add the association

                                AccessionLib.createMarkerAssociation(
                                    new Integer(LogicalDBConstants.REFSEQ),
                                    accid, markerKey, 
				    new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
                            }

			    for (Iterator i = eg.getXRs().iterator();i.hasNext();)
                            {
                                SequenceAccession acc = (SequenceAccession)i.next();
                                String accid = acc.getAccid();

				// if sequence set is null, continue
				// if sequence exists in GU, then continue to next sequence

				if (refseqSet != null) {
				     if (refseqSet.contains(accid)) {
				         continue;
				     }
				}

				// else, add the association

                                AccessionLib.createMarkerAssociation(
                                    new Integer(LogicalDBConstants.REFSEQ),
                                    accid, markerKey, 
				    new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
                            }

			    for (Iterator i = eg.getXPs().iterator();i.hasNext();)
                            {
                                SequenceAccession acc = (SequenceAccession)i.next();
                                String accid = acc.getAccid();

				// if sequence set is null, continue
				// if sequence exists in GU, then continue to next sequence

				if (refseqSet != null) {
				     if (refseqSet.contains(accid)) {
				         continue;
				     }
				}

				// else, add the association

                                AccessionLib.createMarkerAssociation(
                                    new Integer(LogicalDBConstants.REFSEQ),
                                    accid, markerKey, 
				    new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
                            }

			    for (Iterator i = eg.getNMs().iterator();i.hasNext();)
                            {
                                SequenceAccession acc = (SequenceAccession)i.next();
                                String accid = acc.getAccid();

				// if sequence set is null, continue
				// if sequence exists in GU, then continue to next sequence

				if (refseqSet != null) {
				     if (refseqSet.contains(accid)) {
				         continue;
				     }
				}

				// else, add the association

                                AccessionLib.createMarkerAssociation(
                                    new Integer(LogicalDBConstants.REFSEQ),
                                    accid, markerKey, 
				    new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
                            }

			    for (Iterator i = eg.getNRs().iterator();i.hasNext();)
                            {
                                SequenceAccession acc = (SequenceAccession)i.next();
                                String accid = acc.getAccid();

				// if sequence set is null, continue
				// if sequence exists in GU, then continue to next sequence

				if (refseqSet != null) {
				     if (refseqSet.contains(accid)) {
				         continue;
				     }
				}

				// else, add the association

                                AccessionLib.createMarkerAssociation(
                                    new Integer(LogicalDBConstants.REFSEQ),
                                    accid, markerKey, 
				    new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
                            }

			    for (Iterator i = eg.getNPs().iterator();i.hasNext();)
                            {
                                SequenceAccession acc = (SequenceAccession)i.next();
                                String accid = acc.getAccid();

				// if sequence set is null, continue
				// if sequence exists in GU, then continue to next sequence

				if (refseqSet != null) {
				     if (refseqSet.contains(accid)) {
				         continue;
				     }
				}

				// else, add the association

                                AccessionLib.createMarkerAssociation(
                                    new Integer(LogicalDBConstants.REFSEQ),
                                    accid, markerKey, 
				    new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
                            }

			    for (Iterator i = eg.getNGs().iterator();i.hasNext();)
                            {
                                SequenceAccession acc = (SequenceAccession)i.next();
                                String accid = acc.getAccid();

				// if sequence set is null, continue
				// if sequence exists in GU, then continue to next sequence

				if (refseqSet != null) {
				     if (refseqSet.contains(accid)) {
				         continue;
				     }
				}

				// else, add the association

                                AccessionLib.createMarkerAssociation(
                                    new Integer(LogicalDBConstants.REFSEQ),
                                    accid, markerKey, 
				    new Integer(Constants.EGLOAD_REFSKEY), this.loadStream);
                            }
			}
		    }
		}
	    }
	}
    }

    /**
     * checks to assure that the sequence is not associated to a problem
     * clone and creates the marker to sequence association
     * @param accid the accession of the sequence
     * @param mgiMarker the MGIMarker
     * @param currentAssociations a list of current sequence associations for
     * the given marker
     * @param logicaldb the logical db to be used when creating the accession
     * @param refkey the reference to be used when creating the accession
     * record in the database
     * @throws MGIException
     */
    private void makeAssociationToMarker(String accid,
                                         MGIMarker mgiMarker,
                                         Set currentAssociations,
                                         int logicaldb)
    throws MGIException
    {
        boolean newAssociation = false;
        if (currentAssociations == null)
        {
            // this will be the case for refseq since they were
            // not sent to the bucketizer process (exceptions are XMs, XRs)
            newAssociation = true;
        }
        else
        {
            if (currentAssociations.size() == 1)
            {
                Bucketizable item =
                    (Bucketizable) currentAssociations.iterator().next();
                if (item.getProvider().equals(Constants.
                                              PROVIDER_ENTREZGENE))
                    newAssociation = true;

            }
            if (newAssociation)
            {
                // make sure this is not associated with a "problem" clone.
                // see requirements doc
                if (this.problemClones.lookup(accid))
                    newAssociation = false;
            }
        }
        // checks were sucessful, make the association
        if (newAssociation)
        {
            AccessionLib.createMarkerAssociation(
               new Integer(logicaldb),
               accid, mgiMarker.key, new Integer(Constants.EGLOAD_REFSKEY),
               this.loadStream);
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
    throws MGIException
    {
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
                String oldEGID =
                    this.history.lookupEGeneID(marker.mgiID);
                String oldMGIID =
                    this.history.lookupMGIID(egene.getId());
                fields.add(oldEGID == null ? "None" : oldEGID);
                fields.add(oldMGIID == null ? "None" : oldMGIID);

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
    throws MGIException
    {
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
                    String oldEGID =
                        this.history.lookupEGeneID(marker.mgiID);
                    fields.add(oldEGID == null ? "None" : oldEGID);
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
                    String oldMGIID =
                        this.history.lookupEGeneID(egene.getId());
                    fields.add(oldMGIID == null ? "None" : oldMGIID);
                    output = Sprintf.sprintf("%s\t%s\t%s\t%s\t%s", fields);
                }
                else
                    output = Sprintf.sprintf("%s\t%s\t%s\t%s", fields);
            }

            OutputManager.writeln(reportAlias, output);
        }
    }

    /**
     * adds entrezgene object to entrezgene hash map
     * @param b the bucket item
     * @throws MGIException
     */
    private void addEgToMap(BucketItem b)
    throws MGIException
    {
	for (Iterator it = b.membersIterator(Constants.PROVIDER_ENTREZGENE); it.hasNext();)
	{
	      EntrezGene g = (EntrezGene)it.next();
	      String id = g.getId();
	      egMap.put(id,g);
	}
    }

}
