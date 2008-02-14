package org.jax.mgi.dbs.mgd.lookup;

import org.jax.mgi.shr.cache.MappedStringToString;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.dbs.mgd.LogicalDBConstants;
import org.jax.mgi.app.entrezGene.Constants;

/**
 * is a FullCachedLookup for looking up Gene Unification accession ids 
 * and Markers.
 * @has a full cache
 * @does provides a lookup for accessing the cached data
 * @company Jackson Laboratory
 * @author M Walker
 *
 */

public class GeneUnificationLookup extends MappedStringToString
{

    /**
     * Constructor
     * @throws CacheException thrown if there is an error accessing the cache
     * @throws ConfigException thrown of there is an error accessing the
     * configuration
     * @throws DBException thrown if there is an error accessing the database
     */
    public GeneUnificationLookup()
        throws DBException,
        ConfigException,
        CacheException
    {
        super(SQLDataManagerFactory.getShared(SchemaConstants.MGD));
    }

    /**
     * Get the query to fully initialize the cache.
     * @return The query to fully initialize the cache.
     */
    public String getFullInitQuery()
    {
        /**
         * get accession IDs that are gene unification/marker associations
         */
	String sql = "select guID1 = a.accID, guID2 = a.accID " +
	             "from ACC_Accession a, ACC_AccessionReference r " +
	             "where a._MGIType_key = 2 " +
	             "and a._LogicalDB_key = " + LogicalDBConstants.ENTREZ_GENE +
	             "and a._Accession_key = r._Accession_key " +
	             "and r._Refs_key = " + Constants.EGLOAD_GU_REFSKEY;

        return sql;
    }
}
