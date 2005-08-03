package org.jax.mgi.dbs.mgd.lookup;

import org.jax.mgi.shr.cache.MappedStringToBoolean;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.dbs.SchemaConstants;

/**
 * is a FullCachedLookup for looking up whether or not a given accession id is
 * associated with a known problem clone (identified via PRB_notes)
 * @has an internal cache
 * @does provides a lookup for accessing the cached data
 * @company Jackson Laboratory
 * @author M Walker
 *
 */

public class ProblemClonesLookup extends MappedStringToBoolean
{

    /**
     * Constructor
     * @throws CacheException thrown if there is an error accessing the cache
     * @throws ConfigException thrown of there is an error accessing the
     * configuration
     * @throws DBException thrown if there is an error accessing the database
     */

    public ProblemClonesLookup()
        throws DBException,
        ConfigException,
        CacheException
    {
        super(SQLDataManagerFactory.getShared(SchemaConstants.MGD));
    }

    /**
     * Get the query to fully initialize the cache.
     * @assumes Nothing
     * @effects Nothing
     * @return The query to fully initialize the cache.
     */

    public String getFullInitQuery()
    {
        /**
         * gets accession ids of problem clones
         */
        return
            "select a.accID " +
            "from ACC_Accession a, PRB_Notes n " +
            "where n._Probe_key = a._Object_key " +
            "and a._LogicalDB_key = 9 " +
            "and a._MGIType_key = 3 " +
            "and n.note like '%staff have found evidence of artifact%'";
    }
}
