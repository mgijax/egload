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
 * results from the query. It has two types of attributes: instance attributes
 * (typical to any class) and bucketizable attributes (those attributes used
 * in the bucketizing algorithm)
 * @has nothing
 * @does runs the query and creates the MGIMarker objects from the query
 * results and sets all the instance attributes and "bucketizable" attributes
 * for these objects
 * @company The Jackson Laboratory
 * @author M Walker
 */



public class MGIMarkerQuery extends ObjectQuery
{

  // the MGD database manager
  private SQLDataManager sqlMgr = null;

  // a FullCachedLookup for obtaining sequence associations (and non-preferred
  // MGI IDs for the marker
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
     * Get the query string for querying preferred MGI markers
     * @assumes Nothing
     * @effects Nothing
     * @return The query string
     */

    public String getQuery()
    {
        /**
         * gets mouse marker information from MGD
	 * includes interim and official nomenclature only
         */
        String stmt = "select a.accID, m.symbol, m.name, m.chromosome, " +
            "t.name as type, a._Object_key " +
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
     * get a RowDataInterpreter for creating MGIMarker objects from the query
     * results
     * @assumes nothing
     * @effects nothing
     * @return The RowDataInterpreter object
     */

    public RowDataInterpreter getRowDataInterpreter() {
      class Interpreter
          implements RowDataInterpreter {

          public Object interpret(RowReference row)
              throws DBException
          {
	      // create MGIMarker object - this sets the preferred MGI ID as 
	      // a Bucketizable ID attribute
              MGIMarker marker = new MGIMarker(row.getString(1));
	      // set the preferred MGI ID as a MGIMarker attribute
              marker.mgiID = row.getString(1);
	      
              marker.symbol = row.getString(2);
              marker.name = row.getString(3);
              marker.chromosome = row.getString(4);
              marker.type = row.getString(5);
              marker.key = row.getInt(6);

              // adds the the preferred MGI ID to the set of all MGI Ids
	      // for this MGIMarker
              // adds the preferred MGI ID to the Bucketizable SVASet
              marker.addMGIID(new SequenceAccession(marker.mgiID,
                  SequenceAccession.MGI));

              /**
               * obtain sequence associations AND non-preferred MGI ids for the 
	       * marker
               */
              try
              {
		  // sequences also contains non-preferred MGI IDs
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
                          // set bucketizable data
                          marker.addGenBankSequence(acc);
                      }
                      else if (seqCategory.equals(Constants.XM))
                      {
                          // set bucketizable data
                          marker.addXMSequence(acc);
                      }
                      else if (seqCategory.equals(Constants.XR))
                      {
                          // set bucketizable data
                          marker.addXRSequence(acc);
                      }
                      else if (seqCategory.equals(Constants.XP))
		      {
			  // set bucketizable data
                          marker.addXPSequence(acc);
		      }
                      else if (seqCategory.equals(Constants.NM))
		      {
			  // set bucketizable data
                          marker.addNMSequence(acc);
		      }
                      else if (seqCategory.equals(Constants.NR))
		      {
		          // set bucketizable data
                          marker.addNRSequence(acc);
		      }
                      else if (seqCategory.equals(Constants.NP))
		      {
			  // set bucketizable data
                          marker.addNPSequence(acc);
		      }
                      else if (seqCategory.equals(Constants.NG))
		      {
			  // set bucketizable data
                          marker.addNGSequence(acc);
		      }
		     else if (seqCategory.equals(Constants.NT))
                      {
                          // set bucketizable data
                          marker.addNTSequence(acc);
                      }
		      else if (seqCategory.equals(Constants.NW))
                      {
                          // set bucketizable data
                          marker.addNWSequence(acc);
                      }
                      else if (seqCategory.equals(Constants.MGIID))
                      {
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
     * @has MGIMarker instance attributes and "bucketizable" attributes (those
     * attributes used by the AbstractBucketizer to find associations
     * between entrez genes and mgi markers)
     * @does nothing
     * @company The Jackson Laboratory
     * @author M Walker
     *
     */
    public class MGIMarker
        extends EntrezGeneBucketizable
    {
	// Marker attributes
        public Integer key = null;
        public String name = null;
        public String symbol = null;
        public String chromosome = null;
        public String mgiID = null;
        public String type = null;

        public MGIMarker(String id)
        {
            super(id, Constants.PROVIDER_MGI);
        }

        public HashSet getAllRefSeqSequences()
        {
            HashSet refseqSeqs = new HashSet();
            refseqSeqs.addAll(super.getNMSequences());
            refseqSeqs.addAll(super.getNRSequences());
            refseqSeqs.addAll(super.getNPSequences());
            refseqSeqs.addAll(super.getNGSequences());
	    refseqSeqs.addAll(super.getNTSequences());
            refseqSeqs.addAll(super.getNWSequences());
            refseqSeqs.addAll(super.getXMSequences());
            refseqSeqs.addAll(super.getXRSequences());
            refseqSeqs.addAll(super.getXPSequences());
            return refseqSeqs;
        }

        public HashSet getAllSequences()
        {
            HashSet allSeqs = new HashSet();
            allSeqs.addAll(super.getGenBankSequences());
            allSeqs.addAll(getAllRefSeqSequences());
            return allSeqs;
        }

        public String toString()
        {
            HashSet refseqSeqs = getAllRefSeqSequences();

            return mgiID + " : " + name + " | " + symbol + " | " + chromosome +
                " | " + Constants.MGIID + " = " + super.getMGIIDs().toString() +
                " | " + Constants.GENBANK + " = " + super.getGenBankSequences().toString() +
                " | " + Constants.REFSEQ + " = " + getAllRefSeqSequences().toString();
        }
    }

}


