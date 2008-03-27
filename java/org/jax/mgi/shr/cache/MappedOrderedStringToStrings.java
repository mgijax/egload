package org.jax.mgi.shr.cache;

import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;

import org.jax.mgi.shr.cache.FullCachedLookup;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.shr.cache.CacheConstants;
import org.jax.mgi.shr.cache.KeyValue;
import org.jax.mgi.shr.dbutils.SQLDataManager;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.dbutils.BatchProcessor;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.RowDataInterpreter;
import org.jax.mgi.shr.dbutils.MultiRowInterpreter;
import org.jax.mgi.shr.dbutils.RowReference;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.exception.MGIException;
import org.jax.mgi.dbs.SchemaConstants;



abstract public class MappedOrderedStringToStrings extends FullCachedLookup
{
  private SQLDataManager sqlMgr = null;


    public MappedOrderedStringToStrings(SQLDataManager sqlMgr)
       throws CacheException, ConfigException, DBException
    {
        super(sqlMgr);
        this.sqlMgr = sqlMgr;
    }



    public String[] lookup(String key)
    throws DBException, CacheException
    {
        Vector v = (Vector)super.lookupNullsOk(key);
        if (v == null)
            return new String[0];
        else
            return (String[])v.toArray(new String[0]);
    }

    abstract public String getFullInitQuery();


    public RowDataInterpreter getRowDataInterpreter() {
      class Interpreter
          implements MultiRowInterpreter {

        private String egId = null;

        public Object interpret(RowReference row) throws DBException {
          this.egId = row.getString(1);
          return row.getString(2);
        }

        public Object interpretKey(RowReference row) throws DBException {
          return row.getString(1);
        }

        public Object interpretRows(Vector v) {
          return new KeyValue(this.egId, v);
        }
      }
      return new Interpreter();
    }

}


