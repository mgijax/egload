package org.jax.mgi.dbs.mgd.lookup.entrezGene;

import java.util.Vector;

import org.jax.mgi.shr.cache.FullCachedLookup;
import org.jax.mgi.shr.cache.KeyValue;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.SQLDataManager;
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

        String sql =
            "select a._object_key, a.accid, t.abbreviation " +
            "from acc_accession a, acc_accession a2, " +
            "     seq_sequence s, voc_term t  " +
            "where a._mgitype_key = 2 " +
            // refseqs and genbank accids
            "and a._logicaldb_key in (9, 27) " +
            "and a.accid = a2.accid " +
            "and a2._mgitype_key = 19 " +
            "and a2._logicaldb_key = a._logicaldb_key " +
            "and s._sequence_key = a2._object_key " +
            "and s._sequenceType_key = t._term_key " +
            "union " +
            // MGI secondary sequences
            "select a._object_key, a.accid, 'M' " +
            "from acc_accession a " +
            "where a._mgitype_key = 2 " +
            "and  a._logicaldb_key = 1 " +
            "and a.preferred != 1 " +
            "and a.prefixPart = 'MGI:' " +
            "order by _object_key, accid";


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
