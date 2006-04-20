package org.jax.mgi.dbs.rdr.lookup;

import org.jax.mgi.shr.cache.MappedStringToString;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.dbs.SchemaConstants;

/**
 * is a FullCachedLookup for looking up whether or not a given accession
 * id has a HomoloGene ID and is associated with a marker
 * @has a full cache
 * @does provides a lookup for accessing the cached data
 * @company Jackson Laboratory
 * @author L Corbani
 *
 */

public class HomoloGeneLookup extends MappedStringToString
{

    /**
     * Constructor
     * @throws CacheException thrown if there is an error accessing the cache
     * @throws ConfigException thrown of there is an error accessing the
     * configuration
     * @throws DBException thrown if there is an error accessing the database
     */
    public HomoloGeneLookup()
        throws DBException,
        ConfigException,
        CacheException
    {
        super(SQLDataManagerFactory.getShared(SchemaConstants.RADAR));
    }

    /**
     * Get the query to fully initialize the cache.
     * @return The query to fully initialize the cache.
     */
    public String getFullInitQuery()
    {
        /**
         * retrieve mouse records that have HomoloGene IDs
         */
        String sql = "select geneID, groupID from DP_HomoloGene where taxID = 10090";
        return sql;
    }
}
