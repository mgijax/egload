package org.jax.mgi.shr.dbutils;

/**
 *
 * A combination of query and the corresponding RowDataInterpreter.
 * @has a query string and implements RowDataInterpreter
 * @does provides a database query and an interpret method for the results.
 * @company The Jackson Laboratory
 * @author MWalker
 *
 */

public interface InterpretedQuery
{
    /**
     * get the sql query string
     * @return the sql query string
     */
    public String getQuery();

    public RowDataInterpreter getRowDataInterpreter();
}
