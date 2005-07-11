package org.jax.mgi.dbs.mgd.lookup.entrezGene;

import java.util.*;

import org.jax.mgi.shr.sva.SVASetConverter;
import org.jax.mgi.shr.cache.*;
import org.jax.mgi.shr.dbutils.*;
import org.jax.mgi.shr.config.*;
import org.jax.mgi.shr.sva.*;
import org.jax.mgi.shr.stringutil.StringLib;
import org.jax.mgi.dbs.SchemaConstants;

/**
 * is a SVAIndexCache for looking up the previous state of Entrez gene to
 * MGI Marker associations. History data is obtained from the database before
 * doing a delete of the current data (via delete-reload strategy) so that
 * this information can be used in reporting. This is an extension of
 * the SVAIndexCache which allows indexing on secondary keys along with
 * the more typical primary key indexing
 * @has internal cache
 * @does queries the database and fully caches the results into memory
 * @company The Jackson Laboratory
 * @author M Walker
 *
 */

public class EntrezGeneHistory
    extends SVAIndexCache
{
    /**
     * constructor
     * @throws ConfigException thrown if there is an error accesing the
     * configuration
     * @throws DBException thrown if there is an error accessing the database
     * @throws CacheException thrown if there is an error accesing the cache
     */
    public EntrezGeneHistory()
        throws ConfigException, DBException, CacheException
    {
        super(SQLDataManagerFactory.getShared( SchemaConstants.MGD),
              new String[] {"mgi"});
    }

    /**
     * find the Entrez gene associated to the given MGI marker
     * @param mgiID the MGI id
     * @return the associated Entrez Gene
     */
    public String lookupEGeneID(String mgiID)
    {
        Set s = super.lookupIndex("mgi", mgiID);
        if (s == null)
            return null;
        Set ids = new HashSet();
        for (Iterator i = s.iterator(); i.hasNext();)
        {
            ids.add(((Association)i.next()).entrezGeneID);
        }
        return StringLib.join(ids, ",");
    }

    public String lookupMGIID(String entrezGeneID)
    throws DBException, CacheException
    {
        Association assoc =
            (Association)super.lookupNullsOk(entrezGeneID);
        if (assoc == null)
            return null;
        return assoc.mgiID;
    }

    public SVASetConverter getSVASetConverter()
    {
        class Converter
            implements SVASetConverter
        {
            public void convert(Object o, SVASet set)
            {
                Association assoc = (Association)o;
                set.addSVAMember("mgi", assoc.mgiID);
            }
        }
        return new Converter();
    }

    public String getFullInitQuery()
    {
        return
            "select a1.accid, a2.accid " +
            "from ACC_AccessionReference r, " +
            "         ACC_Accession a1, " +
            "         ACC_Accession a2 " +
            "where r._Refs_key = 64047 " +
            "and a1._accession_key = r._accession_key " +
            "and a1._logicaldb_key = 55 " +
            "and a1._mgitype_key = 2 " +
            "and a1.preferred = 1 " +
            "and a2._object_key = a1._object_key " +
            "and a2._mgitype_key = 2 " +
            "and a2._logicaldb_key = 1 " +
            "and a2.preferred = 1 " +
            "and a2.prefixpart = 'MGI:'";

    }

    public RowDataInterpreter getRowDataInterpreter()
    {
        class Interpreter implements RowDataInterpreter
        {
            public Object interpret(RowReference row)
            throws DBException
            {
                Association assoc =
                    new Association(row.getString(1), row.getString(2));
                return new KeyValue(row.getString(1), assoc);
            }
        }
        return new Interpreter();
    }

    public Iterator keySetIterator(String name)
    {
        return this.index.keySet(name).iterator();
    }

    public class Association
    {
        public String entrezGeneID = null;
        public String mgiID = null;
        public Association(String entrezGeneID, String mgiID)
        {
            this.entrezGeneID = entrezGeneID;
            this.mgiID = mgiID;
        }
    }

}