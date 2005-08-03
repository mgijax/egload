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



abstract public class MappedStringToBoolean extends FullCachedLookup
{
    public Object object = new Object();

    public MappedStringToBoolean(SQLDataManager sqlMgr)
       throws CacheException, ConfigException, DBException
    {
        super(sqlMgr);
    }

    public boolean lookup(String key)
    throws DBException, CacheException
    {
        Object o = super.lookupNullsOk(key);
        if (o == null)
            return false;
        else
            return true;
    }

    abstract public String getFullInitQuery();


    public RowDataInterpreter getRowDataInterpreter() {
      class Interpreter
          implements RowDataInterpreter {

        public Object interpret(RowReference row) throws DBException {
            return new KeyValue(row.getString(1), object);
        }
      }
      return new Interpreter();
    }

}


