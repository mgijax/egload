package org.jax.mgi.shr.dbutils;

import org.jax.mgi.shr.exception.ExceptionFactory;

/**
 * An ExceptionFactory for JDBC batch processing errors.
 * @has a hashmap of predefined BatchExceptions stored by a name key
 * @does looks up BCPExceptions by name
 * @company The Jackson Laboratory
 * @author M Walker
 */

public class QueryReportExceptionFactory extends ExceptionFactory {

  /**
   * sort command was interrupted
   */
  public static final String SortInterrupt =
      "org.jax.mgi.shr.report.SortInterrupt";
  static {
    exceptionsMap.put(SortInterrupt, new QueryReportException(
        "The following sort command was interrupted:\n ??", false));
  }

  /**
   * io error during sort operation
   */
  public static final String SortIOErr =
      "org.jax.mgi.shr.report.SortIOErr";
  static {
    exceptionsMap.put(SortIOErr, new QueryReportException(
        "The sort command threw IO error for the following command:\n ??",
        false));
  }

  /**
   * An interrupt signal was received during the execution of sort command
   */
  public static final String NonZeroErr =
      "org.jax.mgi.shr.report.NonZeroErr";
  static {
    exceptionsMap.put(NonZeroErr, new QueryReportException(
        "Sort command returned non-zero status when " +
        "executing the following command: ??", false));
  }


}
