package org.jax.mgi.shr.dbutils;

import org.jax.mgi.shr.exception.MGIException;


public abstract class ObjectQuery implements InterpretedQuery {

    protected SQLDataManager sqlMgr = null;

    // the following constant defintions are exceptions thrown by this class
    private static final String PrePostSQLErr =
        DBExceptionFactory.PrePostSQLErr;


    public ObjectQuery(SQLDataManager sqlMgr)
    {
        this.sqlMgr = sqlMgr;
    }

    public DataIterator execute()
    throws DBException
    {
        DataIterator iterator = null;
        String sql = this.getQuery();
        RowDataInterpreter interpreter = this.getRowDataInterpreter();
        try
        {
            this.runPreSQL();
        }
        catch (MGIException e)
        {
            DBExceptionFactory eFactory = new DBExceptionFactory();
            DBException e2 = (DBException)
                eFactory.getException(PrePostSQLErr, e);
            throw e2;
        }
        ResultsNavigator nav = this.sqlMgr.executeQuery(sql);
        if (interpreter instanceof MultiRowInterpreter)
        {
            MultiRowInterpreter mri =
                (MultiRowInterpreter)interpreter;
            iterator = new MultiRowIterator(nav, mri);
        }
        else
        {
            iterator = new RowDataIterator(nav, interpreter);
        }
        try
        {
            this.runPostSQL();
        }
        catch (MGIException e)
        {
            DBExceptionFactory eFactory = new DBExceptionFactory();
            DBException e2 = (DBException)
                eFactory.getException(PrePostSQLErr, e);
            throw e2;
        }

        return iterator;
    }

    public void runPreSQL() throws MGIException {}

    public void runPostSQL() throws MGIException {}

}
