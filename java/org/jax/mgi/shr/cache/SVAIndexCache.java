package org.jax.mgi.shr.cache;

import java.util.*;

import org.jax.mgi.shr.dbutils.*;
import org.jax.mgi.shr.cache.*;
import org.jax.mgi.shr.sva.*;

public abstract class SVAIndexCache
    extends FullCachedLookup
{

    protected SVAIndex index = null;
    protected String[] names;

    public SVAIndexCache(SQLDataManager sqlMgr, String[] names)
        throws CacheException, DBException
    {
        super(sqlMgr);
        this.index = new SVAIndex(names);
        this.names = names;
    }

    public Set lookupIndex(String indexName, Object key)
    {
        return this.index.lookup(indexName, key);
    }

    public void initCache()
    throws DBException, CacheException
    {
        super.initCache();
        this.initSVAIndex();
    }

    public void initSVAIndex()
    throws DBException
    {
        for (Iterator i = super.cache.keySet().iterator(); i.hasNext();)
        {
            Object key = i.next();
            Object value = super.cache.get(key);
            SVASet set = new SVASet(this.names);
            SVASetConverter converter = this.getSVASetConverter();
            converter.convert(value, set);
            this.index.add(value, set);
        }

    }

    public String[] getIndexNames()
    {
        return this.names;
    }

    public abstract SVASetConverter getSVASetConverter();



}