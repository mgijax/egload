package org.jax.mgi.shr.sva;

/**
 * is an object which can convert a given object into SVASet data
 * and add it to the given SVASet
 * @has nothing
 * @does converts data from a given object to SVASet data and adds it to the
 * given SVASet
 * @company The Jackson Laboratory
 * @author M Walker
 *
 */

public interface SVASetConverter {
    /**
     * add SVASet data obtained from the given object to the given SVASet
     * @param obj the given object
     * @param svaSet the given SVASet
     */
  public void convert(Object obj, SVASet svaSet);
}
