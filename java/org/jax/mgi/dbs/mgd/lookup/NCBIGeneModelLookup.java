package org.jax.mgi.dbs.mgd.lookup;

import org.jax.mgi.shr.cache.MappedStringToString;
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

public class NCBIGeneModelLookup extends MappedStringToString
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
         * get accession IDs/chromosomes that are designated as NCBI genes
         */
	String sql = "select a1.accID, cc.chromosome " +
                     "from MAP_Coord_Collection ccc, MAP_Coordinate c, MRK_Chromosome cc, MAP_Coord_Feature f, ACC_Accession a1 " +
                     "where ccc.name = 'NCBI Gene Model' " +
		     "and ccc._Collection_key = c._Collection_key " +
                     "and c._MGIType_key = 27 " +
                     "and c._Object_key = cc._Chromosome_key " +
                     "and c._Map_key = f._Map_key " +
                     "and f._Object_key = a1._Object_key " +
                     "and a1._MGIType_key = 19 " +
                     "and a1._LogicalDB_key = " + LogicalDBConstants.NCBI_GENE;

        return sql;
    }
}
