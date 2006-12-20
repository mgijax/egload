package org.jax.mgi.dbs.mgd.lookup;

import java.util.Vector;

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
import org.jax.mgi.app.entrezGene.SequenceAccession;

/**
 * is a FullCachedLookup for finding sequences associated to MGI markers
 * @has nothing
 * @does queries the database and stores data into memory
 * @company Jackson Laboratory
 * @author M Walker
 *
 */

public class AssocAccidLookup extends FullCachedLookup
{

    /**
     * Constructor
     * @throws DBException thrown if there is an error accessing the database
     * @throws CacheException thrown if there is an error accessing the cache
     * @throws ConfigException thrown of there is an error accessing the
     * configuration
     */

    public AssocAccidLookup()
        throws DBException,
        ConfigException,
        CacheException
    {
        super(SQLDataManagerFactory.getShared(SchemaConstants.MGD));
    }

    /**
     * lookup associated sequences for a given marker key
     * @assumes nothing
     * @effects if the cache has not been initialized then the query will be
     * executed and the cache will be loaded
     * @param markerKey the database key for a marker
     * @return a vector of SequenceAccession objects
     * @throws CacheException thrown if there is an error accessing the
     * caches
     * @throws DBException thrown if there is an error accessing the database
     */
    public Vector lookup(Integer markerKey)
    throws CacheException, DBException
    {
        return (Vector)super.lookupNullsOk(markerKey);
    }

    /**
     * Get the query to fully initialize the cache.
     * @return The query to fully initialize the cache.
     */
    public String getFullInitQuery()
    {

	// select sequence ids, sequence types for GenBank and RefSeq associated with mouse Markers
	// and secondary MGI ids for Markers

        String sql =
            "select c._Marker_key, a.accID, t.abbreviation  " +
            "from SEQ_Marker_Cache c, ACC_Accession a, VOC_Term t " +
            "where c._Organism_key = 1 " +
            "and c._LogicalDB_key in (9, 27)  " +
            "and c._SequenceType_key = t._Term_key  " +
            "and c._Sequence_key = a._Object_key " +
            "and a._MGIType_key = 19 " +
            "union " +
            "select a._Object_key, a.accID, 'M' " +
            "from ACC_Accession a " +
            "where a._MGIType_key = 2 " +
            "and  a._LogicalDB_key = 1 " +
            "and a.prefixPart = 'MGI:' " +
            "and a.preferred != 1 " +
            "order by _Marker_key, accid";

        return sql;
    }

    /**
     * return the RowDataInterpreter for creating Vectors of
     * SequenceAccession objects from the query results
     * @return the RowDataInterpreter for this query
     */
    public RowDataInterpreter getRowDataInterpreter()
    {
        class Interpreter implements MultiRowInterpreter
        {
            private Integer markerKey = null;

            public Object interpret(RowReference ref)
            throws DBException
            {
                this.markerKey = ref.getInt(1);
                String type = ref.getString(3);
                int typeConstant = 0;
                if (type.equals("M"))
                    typeConstant = SequenceAccession.MGI;
                else if (type.equals("D"))
                    typeConstant = SequenceAccession.DNA;
                else if (type.equals("R"))
                    typeConstant = SequenceAccession.RNA;
                else if (type.equals("P"))
                    typeConstant = SequenceAccession.PROTEIN;
                SequenceAccession accid =
                    new SequenceAccession(ref.getString(2), typeConstant);
                return accid;
            }

            public Object interpretKey(RowReference ref) throws DBException
            {
                return ref.getInt(1);
            }

            public Object interpretRows(Vector v)
            {
                return new KeyValue(this.markerKey, v);
            }
        }
        return new Interpreter();
    }
}
