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
import org.jax.mgi.dbs.mgd.MGITypeConstants;
import org.jax.mgi.dbs.mgd.lookup.SeqIdsByMarkerKeyLookup;
import org.jax.mgi.app.entrezGene.Constants;


/**
 *
 * is an extension of ObjectQuery for specifically NCBI Gene Model Ids
 * from the database. The inner class GM is used to store the
 * results from the query.
 * @does provides the query and RowDataInterpreter for obtaining
 * NCBI Gene Model objects from the database
 * @company The Jackson Laboratory
 * @author sc
 *
 */

public class NCBIGMQuery extends ObjectQuery
{
    SeqIdsByMarkerKeyLookup rsLookup = 
       new SeqIdsByMarkerKeyLookup(LogicalDBConstants.REFSEQ);
    SeqIdsByMarkerKeyLookup gbLookup = 
       new SeqIdsByMarkerKeyLookup(LogicalDBConstants.SEQUENCE);
    
    /**
     * Constructor
     * @assumes Nothing
     * @effects Nothing
     * @throws CacheException thrown if there is an error accessing the cache
     * @throws ConfigException thrown of there is an error accessing the
     * configuration
     * @throws DBException thrown if there is an error accessing the database
     */

    public NCBIGMQuery() throws CacheException, ConfigException,
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
         * gets NCBI Gene Model and marker associations (MGI ID if there is one)
         */
	return "select  distinct a1.accid as ncbiGMID,  a3.accID " +
	    "from ACC_Accession a1, ACC_Accession a2, ACC_Accession a3 " +
	    "where a1._MGIType_key = " + MGITypeConstants.SEQUENCE + " " +
	    "and a1._LogicalDB_key = " + LogicalDBConstants.NCBI_GENE + " " +
	    "and lower(a1.accid) = lower(a2.accid) " +
	    "and a1.preferred = 1 " +
	    "and a2._MGItype_key = " + MGITypeConstants.MARKER + " " +
	    "and a2._LogicalDB_key = " + LogicalDBConstants.NCBI_GENE + " " +
	    "and a2._Object_key = a3._Object_key " +
	    "and a2.preferred = 1 " +
	    "and a3._MGIType_key = " + MGITypeConstants.MARKER + " " +
	    "and a3._LogicalDB_key = 1 " +
	    "and a3.prefixPart = 'MGI:' " +
	    "and a3.preferred = 1 " +
	    "union " +
	    "select distinct a1.accid as ncbiGMID, null as accID " +
	    "from ACC_Accession a1 " +
	    "where a1._MGIType_key = " + MGITypeConstants.SEQUENCE + " " +
	    "and a1._LogicalDB_key = " + LogicalDBConstants.NCBI_GENE + " " +
	    "and a1.preferred = 1 " +
	    "and not exists (select 1 " +
	    "from ACC_Accession a2, ACC_Accession a3 " +
	    "where lower(a1.accid) = lower(a2.accid) " +
	    "and a2._MGItype_key = " + MGITypeConstants.MARKER + " " +
	    "and a2._LogicalDB_key = " + LogicalDBConstants.NCBI_GENE + " " +
	    "and a2._Object_key = a3._Object_key " +
	    "and a2.preferred = 1 " +
	    "and a3._MGIType_key = " + MGITypeConstants.MARKER + " " +
	    "and a3._LogicalDB_key = 1 " +
	    "and a3.prefixPart = 'MGI:' " +
	    "and a3.preferred = 1) " + 
	    "order by ncbiGMID";
    }


    /**
     * returns a RowDataInterpreter which can interpret the results from the
     * query provided in getQuery(). The object returned is the extended
     * version of the RowDataInterpreter called MultiRowInterpreter which
     * is used for processing cartesian products such that many rows represent
     * a single object
     * @return a RowDataInterpreter for creating GM objects
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
                GMRow gr = new GMRow();
                gr.gmId = row.getString(1);
                gr.mgiId = row.getString(2);
                return gr;
            }
            /**
             * processes a Vector of objects to create an GM object
	     * this is selecting all of the GU/RefSeq/GenBank associations
	     * that already exist in MGI
             * @param v the vector of objects obtained from returns from
             * subsequent calls to the interpret() method
             * @return the new GM object
             */
            public Object interpretRows(Vector v) throws InterpretException
            {
		try {
                    GMRow commonElements = (GMRow)v.get(0);
                    GM gm = new GM(commonElements.gmId);

                    for (int i = 0; i < v.size(); i++)
                    {
                        GMRow row = (GMRow)v.get(i);
		        String mgiId = row.mgiId;

			gm.addMarker(mgiId);
                    }

                    return gm;

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
     * is a plain old java object for NCBI GMs and their associations to markers
     * @has gmId - an NCBI Gene Model Id
     * and a set of markers with their associated GenBank & RefSeq Ids
     * @does nothing
     * @company The Jackson Laboratory
     * @author lec
     *
     */
    public class GM
    {
        // NCBI Gene Model Id
	private String gmId;
	
	// Marker MGI IDs associated with 'gmID' via the GU
	private HashSet markers;

	public GM(String id) throws CacheException, ConfigException, DBException
	{
	    gmId = id;
	    markers = new HashSet();
	}

	// add an marker MGI ID
	public void addMarker(String mkr)
	{
	    markers.add(mkr);
	}

	// get the NCBI Gene Model ID
	public String getGMId() { return gmId; };

	// get the set of marker MGI Ids for 'gmId'
	public HashSet getMarkers() { return markers; };
    }

    /**
     *
     * is a plain old java object for holding one row of data from the query
     * @has gmId and mgiKey
     * @does nothing
     * @company Jackson Laboratory
     * @author lec
     *
     */
    public class GMRow
    {
      public String gmId = null;
      public String mgiId = null;

      public String toString()
      {
        return gmId + "|" + mgiId;
      }
    }
}

