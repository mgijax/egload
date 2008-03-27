package org.jax.mgi.shr.dbutils;

import org.jax.mgi.shr.exception.MGIException;

/**
 * An MGIException which represents an error in creating a report file
 * @has an exception message, a data related indicator and a parent
 * exception which can be null.
 * @does nothing
 * @author M Walker
 */

public class QueryReportException extends MGIException {
  public QueryReportException(String pMessage, boolean pDataRelated) {
    super(pMessage, pDataRelated);
  }


}
