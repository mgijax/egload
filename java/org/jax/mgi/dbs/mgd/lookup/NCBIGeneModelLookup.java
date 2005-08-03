package org.jax.mgi.dbs.mgd.lookup;

import org.jax.mgi.shr.cache.MappedStringToBoolean;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.dbs.mgd.LogicalDBConstants;

/**
 * is a FullCachedLookup for looking up whether or not a given accession
 * id is a NCBI Gene Model and is associated with a marker
 * @has a full cache
 * @does provides a lookup for accessing the cached data
 * @company Jackson Laboratory
 * @author M Walker
 *
 */

public class NCBIGeneModelLookup extends MappedStringToBoolean
{

    /**
     * Constructor
     * @throws CacheException thrown if there is an error accessing the cache
     * @throws ConfigException thrown of there is an error accessing the
     * configuration
     * @throws DBException thrown if there is an error accessing the database
     */
    public NCBIGeneModelLookup()
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
         * get those accessions which are designated as NCBI genes
         */
        String sql = "select accID " +
                     "from ACC_Accession " +
                     "where _LogicalDB_key = " +
                     LogicalDBConstants.NCBI_GENE;
        return sql;
    }
}
