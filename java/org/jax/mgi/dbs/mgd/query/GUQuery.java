package org.jax.mgi.dbs.mgd.query;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;


import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.shr.dbutils.ObjectQuery;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.RowDataInterpreter;
import org.jax.mgi.shr.dbutils.RowReference;
import org.jax.mgi.shr.dbutils.MultiRowInterpreter;
import org.jax.mgi.shr.dbutils.InterpretException;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.app.entrezGene.EntrezGeneBucketizable;
import org.jax.mgi.app.entrezGene.Constants;
import org.jax.mgi.dbs.mgd.LogicalDBConstants;
import org.jax.mgi.dbs.mgd.lookup.SeqIdsByMarkerKeyLookup;


/**
 *
 * is an extension of ObjectQuery for specifically getting Entrez Gene data
 * from the database. The inner class GU is used to store the
 * results from the query. It has two types of attributes: class attributes
 * (typical to any class) and bucketizable attributes (those attributes used
 * in the bucketizing algorithm)
 * @does provides the query and RowDataInterpreter for obtaining
 * GU objects from the database
 * @company The Jackson Laboratory
 * @author M Walker
 *
 */

public class GUQuery extends ObjectQuery
{
    SeqIdsByMarkerKeyLookup rsLookup = new SeqIdsByMarkerKeyLookup(LogicalDBConstants.REFSEQ);
    SeqIdsByMarkerKeyLookup gbLookup = new SeqIdsByMarkerKeyLookup(LogicalDBConstants.SEQUENCE);
    
    /**
     * Constructor
     * @assumes Nothing
     * @effects Nothing
     * @throws CacheException thrown if there is an error accessing the cache
     * @throws ConfigException thrown of there is an error accessing the
     * configuration
     * @throws DBException thrown if there is an error accessing the database
     */

    public GUQuery() throws CacheException, ConfigException,
        DBException
    {
        super(SQLDataManagerFactory.getShared(SchemaConstants.MGD));
    }


    /**
     * get the query string
     * @assumes nothing
     * @effects nothing
     * @return the query string
     */
    public String getQuery()
    {
        /**
         * gets gene unification/marker associations data
         */
	return "select guId = a.accID, mgiKey = a2._Object_key " +
	     "from ACC_Accession a, MRK_Marker m, ACC_AccessionReference r, ACC_Accession a2 " +
	     "where a._MGIType_key = 2 " +
	     "and a._LogicalDB_key = " + LogicalDBConstants.NCBI_GENE +
	     "and a._Object_key = m._Marker_key " +
	     "and m._Organism_key = 1 " +
	     "and a._Accession_key = r._Accession_key " +
	     "and r._Refs_key = " + Constants.EGLOAD_GU_REFSKEY +
	     "and a._Object_key = a2._Object_key " +
	     "and a2._MGIType_key = 2 " +
	     "and a2._LogicalDB_key = 1 " +
	     "and a2.prefixPart = 'MGI:' " +
	     "and a2.preferred = 1 " +
	     "order by guId";

    }


    /**
     * returns a RowDataInterpreter which can interpret the results from the
     * query provided in getQuery(). The object returned is the extended
     * version of the RowDataInterpreter called MultiRowInterpreter which
     * is used for processing cartesian products such that many rows represent
     * a single object
     * @return a RowDataInterpreter for creating GU objects
     */
    public RowDataInterpreter getRowDataInterpreter()
    {
        class Interpreter
            implements MultiRowInterpreter
        {
            /**
             * process one row from the database
             * @param row the database row
             * @return an object which will later be processed by the
             * interpretRows() method
             * @throws DBException thrown if there is an error with the
             * database
             */
            public Object interpret(RowReference row) throws DBException
            {
                GURow gr = new GURow();
                gr.guId = row.getString(1);
                gr.mgiKey = row.getInt(2);
                return gr;
            }
            /**
             * processes a Vector of objects to create an GU object
	     * this is selecting all of the GU/RefSeq/GenBank associations
	     * that already exist in MGI
             * @param v the vector of objects obtained from returns from
             * subsequent calls to the interpret() method
             * @return the new GU object
             */
            public Object interpretRows(Vector v) throws InterpretException
            {
		try {
                    GURow commonElements = (GURow)v.get(0);
                    GU gu = new GU(commonElements.guId);

                    for (int i = 0; i < v.size(); i++)
                    {
                        GURow row = (GURow)v.get(i);
		        Integer mgiKey = row.mgiKey;
			HashMap seqs = new HashMap();

		        // get refseq for the marker
		        HashSet refseqs = rsLookup.lookup(mgiKey);
			seqs.put("RefSeq", refseqs);

		        // get genbank for the marker
		        HashSet genbanks = gbLookup.lookup(mgiKey);
			seqs.put("GenBank", genbanks);

			gu.addMarker(mgiKey, seqs);
                    }

                    return gu;

		} catch (DBException e) {
		    throw new InterpretException(e.getMessage());
		} catch (CacheException e) {
		    throw new InterpretException(e.getMessage());
		} catch (ConfigException e) {
		    throw new InterpretException(e.getMessage());
                }
            }

            /**
             * return a key object which is used for grouping concurrent
             * rows from the result set
             * @param row the database row
             * @return the first column of the row
             * @throws DBException thrown if there is an error reading the
             * result set
             */
            public Object interpretKey(RowReference row) throws DBException
            {
              return row.getString(1);
            }
        }
        return new Interpreter();
    }

    /**
     * is a plain old java object for GU data (guId and its set of markers)
     * @has nothing
     * @does nothing
     * @company The Jackson Laboratory
     * @author M Walker
     *
     */
    public class GU
    {
	private String guId;
	private HashMap markers;

	public GU(String id) throws CacheException, ConfigException, DBException
	{
	    guId = id;
	    markers = new HashMap();
	}

	public void addMarker(Integer mkr, HashMap seqs)
	{
	    markers.put(mkr, seqs);
	}

	public String getGUId() { return guId; };

	public HashMap getMarkers() { return markers; };
    }

    /**
     *
     * is a plain old java object for holding one row of data from the query
     * @has nothing
     * @does nothing
     * @company Jackson Laboratory
     * @author M Walker
     *
     */
    public class GURow
    {
      public String guId = null;
      public Integer mgiKey = null;

      public String toString()
      {
        return guId + "|" + mgiKey;
      }
    }
}

