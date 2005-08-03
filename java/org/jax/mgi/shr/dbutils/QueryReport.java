package org.jax.mgi.shr.dbutils;

import org.jax.mgi.shr.ioutils.OutputDataFile;
import org.jax.mgi.shr.ioutils.IOUException;
import org.jax.mgi.shr.config.ConfigException;


public class QueryReport
{
    public static void output(String filename, SQLDataManager sqlMgr,
                              String query, String formatPattern)
    throws ConfigException, IOUException, DBException
    {
        OutputDataFile file = new OutputDataFile(filename);
        ResultsNavigator nav = sqlMgr.executeQuery(query);
        nav.setInterpreter(new RowToPrintInterpreter(formatPattern));
        while (nav.next())
        {
            String s = (String)nav.getCurrent();
            file.writeln(s);
        }
        nav.close();
        file.close();
    }

}