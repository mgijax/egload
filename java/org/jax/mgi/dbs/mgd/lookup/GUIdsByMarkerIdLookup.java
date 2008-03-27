package org.jax.mgi.dbs.mgd.lookup;

import java.util.Vector;
import java.util.Iterator;
import java.util.HashSet;

import org.jax.mgi.shr.cache.FullCachedLookup;
import org.jax.mgi.shr.cache.KeyValue;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.dbutils.MultiRowInterpreter;
import org.jax.mgi.shr.dbutils.RowDataInterpreter;
import org.jax.mgi.shr.dbutils.RowReference;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.dbs.mgd.LogicalDBConstants;
import org.jax.mgi.app.entrezGene.Constants;

/**
 * @is An object that knows how to look up the set of GU ids for a Marker MGI ID
 * @has
 * @does
 *   <UL>
 *   <LI> Provides a method to lookup GU ids for a given MGI ID
 *        MGI ID
 *   </UL>
 * @company The Jackson Laboratory
 * @author sc
 * @version 1.0
 */

public class GUIdsByMarkerIdLookup extends FullCachedLookup {
    /**
     * Constructor
     * @throws DBException thrown if there is an error accessing the database
     * @throws CacheException thrown if there is an error accessing the cache
     * @throws ConfigException thrown of there is an error accessing the
     * configuration
     */

    public GUIdsByMarkerIdLookup()
        throws DBException,
        ConfigException,
        CacheException {
        super(SQLDataManagerFactory.getShared(SchemaConstants.MGD));
    }

    /**
     * lookup associated GU ids for a given marker MGI ID
     * @assumes nothing
     * @effects if the cache has not been initialized then the query will be
     * executed and the cache will be loaded. Queries a database.
     * @param mgiID the mgiID of a marker
     * @return a set of GU IDs
     * @throws CacheException thrown if there is an error accessing the
     * caches
     * @throws DBException thrown if there is an error accessing the database
     */
    public HashSet lookup(String mgiID)
    throws CacheException, DBException {
        return (HashSet)super.lookupNullsOk(mgiID);
    }

    /**
     * Get the query to fully initialize the cache.
     * @return The query to fully initialize the cache.
     */
    public String getFullInitQuery() {

	// select mgiIds of markers and their guId associations
        String sql =
            "select mgiId = a2.accID, guId = a.accID " +
             "from ACC_Accession a, MRK_Marker m, ACC_AccessionReference r, " +
	     "ACC_Accession a2 " +
             "where a._MGIType_key = 2 " +
             "and a._LogicalDB_key = " + LogicalDBConstants.NCBI_GENE + " " +
             "and a._Object_key = m._Marker_key " +
             "and m._Organism_key = 1 " +
             "and a._Accession_key = r._Accession_key " +
             "and r._Refs_key = " + Constants.EGLOAD_GU_REFSKEY + " " +
             "and a._Object_key = a2._Object_key " +
             "and a2._MGIType_key = 2 " +
             "and a2._LogicalDB_key = " + LogicalDBConstants.MGI + " " +
             "and a2.prefixPart = 'MGI:' " + 
             "and a2.preferred = 1 " +
             "order by guId";

        return sql;
    }

    /**
     * return the RowDataInterpreter for creating  KeyValue objects from the query results
     * @return the RowDataInterpreter for this query
     */
    public RowDataInterpreter getRowDataInterpreter() {
           class Interpreter implements MultiRowInterpreter {
            
		public Object interpret(RowReference ref)
		throws DBException
		{
		    return new RowData(ref);
		}

		public Object interpretKey(RowReference ref) throws DBException
		{
		    return ref.getString(1);
		}

		public Object interpretRows(Vector v)
		{
		    RowData rd = (RowData)v.get(0);
		    String mgiID = rd.mgiID;
		    HashSet guIDSet = new HashSet();
		    for (Iterator it = v.iterator(); it.hasNext();)
		    { 
			rd = (RowData)it.next();
			guIDSet.add(rd.guID);
		    }
		    return new KeyValue(mgiID, guIDSet);
		}
	    }
	    
        return new Interpreter();
    }
    /**
     * Simple data object representing a row of data from the query
     */
    class RowData  {
	protected String mgiID;
	protected String guID;
	public RowData (RowReference row) throws DBException {
	    mgiID = row.getString(1);
	    guID = row.getString(2);
	}
    }
}
