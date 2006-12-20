package org.jax.mgi.dbs.mgd.query;

import java.util.*;
import java.util.regex.*;
import java.io.File;

import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.shr.dbutils.ObjectQuery;
import org.jax.mgi.shr.dbutils.SQLDataManager;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.DBExceptionFactory;
import org.jax.mgi.shr.dbutils.RowDataInterpreter;
import org.jax.mgi.shr.dbutils.RowReference;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.app.entrezGene.AccessionClassifier;
import org.jax.mgi.app.entrezGene.EntrezGeneBucketizable;
import org.jax.mgi.app.entrezGene.SequenceAccession;
import org.jax.mgi.dbs.mgd.lookup.AssocAccidLookup;
import org.jax.mgi.app.entrezGene.Constants;



/**
 * is an extension of ObjectQuery for specifically getting MGI marker data
 * from the database. The inner class MGIMarker is used to store the
 * results from the query. It has two types of attributes: class attributes
 * (typical to any class) and bucketizable attributes (those attributes used
 * in the bucketizing algorithm)
 * @has nothing
 * @does runs the query and creates the MGIMarker objects from the query
 * results and sets all the class attributes and "bucketizable" attributes
 * for these objects
 * @company The Jackson Laboratory
 * @author M Walker
 */



public class MGIMarkerQuery extends ObjectQuery
{

  // the MGD database manager
  private SQLDataManager sqlMgr = null;

  // a FullCachedLookup for obtaining sequence associations to a marker
  private AssocAccidLookup sequenceLookup = null;

  // classify a sequence type using character pattern matching on an accid
  private AccessionClassifier accidClassifier = new AccessionClassifier();

    /**
     * Constructor
     * @throws CacheException thrown if there is an error accessing the cache
     * @throws ConfigException thrown of there is an error accessing the
     * configuration
     * @throws DBException thrown if there is an error accessing the database
     */

    public MGIMarkerQuery() throws CacheException, ConfigException,
        DBException
    {
        super(SQLDataManagerFactory.getShared(SchemaConstants.MGD));
        sqlMgr = SQLDataManagerFactory.getShared(SchemaConstants.MGD);
        this.sequenceLookup = new AssocAccidLookup();
        this.sequenceLookup.initCache();
    }



    /**
     * Get the query string for querying MGI markers
     * @assumes Nothing
     * @effects Nothing
     * @return The query string
     */

    public String getQuery()
    {
        /**
         * gets mouse marker information from MGD
         */
        String stmt = "select a.accID, m.symbol, m.name, m.chromosome, " +
            "type = t.name, a._Object_key " +
            "from ACC_Accession a, MRK_Marker m, MRK_Types t " +
            "where m._Organism_key = 1 " +
            "and m._Marker_Status_key in (1, 3) " +
            "and m._Marker_key = a._Object_key " +
            "and a._MGIType_key = 2 " +
            "and a._LogicalDB_key = 1 " +
            "and a.prefixPart = 'MGI:' " +
            "and a.preferred = 1 " +
            "and m._Marker_Type_key = t._Marker_Type_key";
        return stmt;
    }

    /**
     * get a RowDataInterpreter for creating EntrezGene objects from the query
     * results
     * @assumes nothing
     * @effects nothing
     * @return The RowDataInterpreter object.
     */

    public RowDataInterpreter getRowDataInterpreter() {
      class Interpreter
          implements RowDataInterpreter {

          public Object interpret(RowReference row)
              throws DBException
          {
              MGIMarker marker = new MGIMarker(row.getString(1));
              marker.mgiID = row.getString(1);
              marker.symbol = row.getString(2);
              marker.name = row.getString(3);
              marker.chromosome = row.getString(4);
              marker.type = row.getString(5);
              marker.key = row.getInt(6);

              // adds the MGI ID to the marker
              marker.mgiIDs.add(new SequenceAccession(marker.mgiID,
                  SequenceAccession.MGI));
              // adds the MGI ID as a Bucketizable element
              marker.addMGIID(new SequenceAccession(marker.mgiID,
                  SequenceAccession.MGI));

              /**
               * obtain sequence associations
               */
              try
              {
                  Vector sequences = sequenceLookup.lookup(marker.key);
                  if (sequences == null)
                      return marker;
                  for (int i = 0; i < sequences.size(); i++)
                  {
                      SequenceAccession acc =
                          (SequenceAccession)sequences.get(i);
                      String seq = acc.getAccid();
                      String seqCategory = accidClassifier.classify(seq);

                      if (seqCategory.equals(Constants.GENBANK))
                      {
                          // set marker attribute
                          marker.genbankSeqs.add(acc);
                          // set bucketizable data
                          marker.addGenBankSequence(acc);
                      }
                      else if (seqCategory.equals(Constants.XM))
                      {
                          // set marker attribute
                          marker.xmSeqs.add(acc);
                          // set bucketizable data
                          marker.addXMSequence(acc);
                      }
                      else if (seqCategory.equals(Constants.XR))
                      {
                          // set marker attribute
                          marker.xrSeqs.add(acc);
                          // set bucketizable data
                          marker.addXRSequence(acc);
                      }
                      else if (seqCategory.equals(Constants.XP))
                          // set marker attribute
                          marker.xpSeqs.add(acc);
                      else if (seqCategory.equals(Constants.NM))
                          // set marker attribute
                          marker.nmSeqs.add(acc);
                      else if (seqCategory.equals(Constants.NR))
                          // set marker attribute
                          marker.nrSeqs.add(acc);
                      else if (seqCategory.equals(Constants.NP))
                          // set marker attribute
                          marker.npSeqs.add(acc);
                      else if (seqCategory.equals(Constants.MGIID))
                      {
                          // set marker attribute
                          marker.mgiIDs.add(acc);
                          // set bucketizable data
                          marker.addMGIID(acc);
                      }
                  }
              }
              catch (CacheException e)
              {
                  DBExceptionFactory eFactory = new DBExceptionFactory();
                  DBException e2 = (DBException)
                      eFactory.getException(DBExceptionFactory.ConfigErr, e);
                  throw e2;
              }

              return marker;
        }
      }

      return new Interpreter();
    }

    /**
     * is a plain old java object for MGI marker data
     * @has entrez gene class attributes and "bucketzable" attributes (those
     * attributes used by the AbstractBucketizer to find discover associations
     * between entrez genes and mgi markers)
     * @does nothing
     * @company The Jackson Laboratory
     * @author M Walker
     *
     */
    public class MGIMarker
        extends EntrezGeneBucketizable
    {
        public Integer key = null;
        public String name = null;
        public String symbol = null;
        public String chromosome = null;
        public String mgiID = null;
        public String type = null;

        private String[] sequenceGroups =
            {Constants.GENBANK, Constants.XM, Constants.XR, Constants.MGIID};

        // all HashSets defined here contain instances of SequenceAccession
        public HashSet genbankSeqs = new HashSet();

        public HashSet nmSeqs = new HashSet();
        public HashSet nrSeqs = new HashSet();
        public HashSet npSeqs = new HashSet();

        public HashSet xmSeqs = new HashSet();
        public HashSet xrSeqs = new HashSet();
        public HashSet xpSeqs = new HashSet();

        // mgiIDs can be primary or secondary
        public HashSet mgiIDs = new HashSet();

        public MGIMarker(String id)
        {
            super(id, Constants.PROVIDER_MGI);
        }

        public HashSet getAllRefSeqSequences()
        {
            HashSet refseqSeqs = new HashSet();
            refseqSeqs.addAll(nmSeqs);
            refseqSeqs.addAll(nrSeqs);
            refseqSeqs.addAll(npSeqs);
            refseqSeqs.addAll(xmSeqs);
            refseqSeqs.addAll(xrSeqs);
            refseqSeqs.addAll(xpSeqs);
            return refseqSeqs;
        }

        public HashSet getAllSequences()
        {
            HashSet allSeqs = new HashSet();
            allSeqs.addAll(genbankSeqs);
            allSeqs.addAll(getAllRefSeqSequences());
            return allSeqs;
        }

        public String toString()
        {
            HashSet refseqSeqs = getAllRefSeqSequences();

            return mgiID + " : " + name + " | " + symbol + " | " + chromosome +
                " | " + Constants.MGIID + " = " + mgiIDs.toString() +
                " | " + Constants.GENBANK + " = " + genbankSeqs.toString() +
                " | " + Constants.REFSEQ + " = " + refseqSeqs.toString();
        }
    }

}


