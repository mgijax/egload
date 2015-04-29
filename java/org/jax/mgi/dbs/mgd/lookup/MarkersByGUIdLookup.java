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
 * @is An object that knows how to look up the set of markers associated
 *   with a GU Id
 *   A GU Id is an NCBI Gene Model ID determined to be associated with an 
 *   MGI Gene by the MGI Gene Unification (GU) process
 * @has
 * @does
 *   <UL>
 *   <LI> Provides a method to lookup marker keys for a given GU id
 *   </UL>
 * @company The Jackson Laboratory
 * @author sc
 * @version 1.0
 */

public class MarkersByGUIdLookup extends FullCachedLookup {
    /**
     * Constructor
     * @throws DBException thrown if there is an error accessing the database
     * @throws CacheException thrown if there is an error accessing the cache
     * @throws ConfigException thrown of there is an error accessing the
     * configuration
     */

    public MarkersByGUIdLookup()
        throws DBException,
        ConfigException,
        CacheException {
        super(SQLDataManagerFactory.getShared(SchemaConstants.MGD));
    }

    /**
     * lookup associated marker keys for a given GU id
     * @assumes nothing
     * @effects if the cache has not been initialized then the query will be
     * executed and the cache will be loaded. Queries a database.
     * @param GU Id - an NCBI Gene Model Id
     * @return a set of marker keys
     * @throws CacheException thrown if there is an error accessing the
     * caches
     * @throws DBException thrown if there is an error accessing the database
     */
    public HashSet lookup(String guID)
    throws CacheException, DBException {
        return (HashSet)super.lookupNullsOk(guID);
    }

    /**
     * Get the query to fully initialize the cache.
     * @return The query to fully initialize the cache.
     */
    public String getFullInitQuery() {

	// select marker keys of markers and their guId associations
        String sql =
            "select a.accid as guId , a._object_key as _Marker_key  " + 
             "from ACC_Accession a, MRK_Marker m, ACC_AccessionReference r " +
             "where a._MGIType_key = 2 " +
             "and a._LogicalDB_key = " + LogicalDBConstants.NCBI_GENE + " " +
             "and a._Object_key = m._Marker_key " +
             "and m._Organism_key = 1 " +
             "and a._Accession_key = r._Accession_key " +
             "and r._Refs_key = " + Constants.EGLOAD_GU_REFSKEY + " " +
             "order by guId";

        return sql;
    }

    /**
     * return the RowDataInterpreter for creating  KeyValue objects 
     * from the query results
     * @return the RowDataInterpreter for this query
     */
    public RowDataInterpreter getRowDataInterpreter() {
       class Interpreter implements MultiRowInterpreter {
	
	   public Object interpret(RowReference ref)
	   throws DBException
	   {
		return new RowData(ref);
	   }

	   public Object interpretKey(RowReference ref) 
		throws DBException
	   {
	       return ref.getInt(1);
	   }

	    public Object interpretRows(Vector v)
	   {
		RowData rd = (RowData)v.get(0);
		String guID = rd.guID;
		HashSet markerKeySet = new HashSet();
		for (Iterator it = v.iterator(); it.hasNext();)
		{ 
		    rd = (RowData)it.next();
		    markerKeySet.add(rd.markerKey);
		}
		return new KeyValue(guID, markerKeySet);
	   }
       }
	
        return new Interpreter();
    }
    /**
     * Simple data object representing a row of data from the query
     */
    class RowData  {
	protected Integer markerKey;
	protected String guID;
	public RowData (RowReference row) throws DBException {
	    guID = row.getString(1);
	    markerKey = row.getInt(2);
	}
    }
}
