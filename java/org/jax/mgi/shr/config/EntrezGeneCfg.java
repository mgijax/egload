package org.jax.mgi.shr.config;


import org.jax.mgi.shr.log.LoggerFactory;

/**
 * A class for accessing configuration information for the cache package
 * @has accessor methods for accessing configuration parameters
 * @does lookups configuration parameters from the ConfigurationManagement
 * object
 * @company The Jackson Laboratory
 * @author M Walker
 */

public class EntrezGeneCfg extends Configurator {

    private String DEFAULT_SQLDIR = "sql";

  /**
   * constructor
   * @throws ConfigException thrown if the there is an error accessing the
   * configuration file
   */

  public EntrezGeneCfg() throws ConfigException {
    super();
  }

  /**
   * get whether or not it is ok to prevent the query and reporting of history
   * data consisting of the previous Entrez gene to MGI marker associations.
   * The parameter name read from the configuration file or system properties
   * is EGLOAD_PERFORM_HISTORY. The default value is false.
   * @return true if it is ok to perform history, false otherwise
   * @throws ConfigException thrown if there is an error accessing the
   * configuration
   */
  public Boolean getOkToPerformHistory() throws ConfigException
  {
      return getConfigBoolean("EGLOAD_PERFORM_HISTORY", new Boolean(false));
  }

  /**
   * get whether or not it is ok to prevent the delete portion of the
   * delete and reload strategy.
   * data consisting of the previous Entrez gene to MGI marker associations.
   * The parameter name read from the configuration file or system properties
   * is EGLOAD_PREVENT_DELETE. The default value is false.
   * @return
   * @throws ConfigException thrown if there is an error accessing the
   * configuration
   */
  public Boolean getOkToPreventDelete() throws ConfigException
  {
      return getConfigBoolean("EGLOAD_PREVENT_DELETE", new Boolean(false));
  }

  /**
   * get the sql directory configured for this load which stores the location
   * of external sql files.
   * The parameter name read from the configuration file or system properties
   * is SQLDIR. The default value is './sql'.
   * @return
   * @throws ConfigException thrown if there is an error accessing the
   * configuration
   */
  public String getSQLDirectory() throws ConfigException
  {
      return getConfigString("SQLDIR", DEFAULT_SQLDIR);
  }




}
