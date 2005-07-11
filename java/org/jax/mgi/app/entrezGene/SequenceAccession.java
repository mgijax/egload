package org.jax.mgi.app.entrezGene;

/**
 * is a plain old java object for storing an accession id for sequence data
 * and the type of sequence
 * @has nothing
 * @does nothing
 * @company The Jackson Laboratory
 * @author M Walker
 *
 */

public class SequenceAccession
{
    public static final int UNDEFINED = 0;
    public static final int RNA = 1;
    public static final int DNA = 2;
    public static final int PROTEIN = 3;
    public static final int MGI = 4;

    /**
     * accession id of sequence
     */
    private String accid = null;
    /**
     * type of sequence
     */
    private int type = 0;

    /**
     * constructor
     * @param accid the accession id
     * @param type the sequence type
     */
    public SequenceAccession(String accid, int type)
    {
        this.accid = accid;
        if (type <= 4 && type > 0)
            this.type = type;
    }

    /**
     * get the accession id
     * @return the accession id
     */
    public String getAccid()
    {
        return this.accid;
    }

    /**
     * get the sequence type
     * @return the sequence type
     */
    public int getType()
    {
        return this.type;
    }

    /**
     * get the sequence type as a string
     * @return the sequence type as either RNA, DNA, PROTEIN, MGI.
     * Other possible values are UNDETERMINED which means the type was never
     * set and UNKOWN which means the type was set but it is unknown.
     */
    public String getTypeAsString()
    {
        String typeAsString = "UNDETERMINED";

        switch (this.type)
        {
            case 0: typeAsString = "UNKOWN"; break;
            case 1: typeAsString = "RNA"; break;
            case 2: typeAsString = "DNA"; break;
            case 3: typeAsString = "PROTEIN"; break;
            case 4: typeAsString = "MGI"; break;
        }
        return typeAsString;

    }
    /**
     * override base class method for calculating hash codes
     * @return hash for this instance
     */
    public int hashCode()
    {
        return this.accid.hashCode();
    }

    /**
     * override base class method for equals
     * @param obj the comparison object
     * @return true if equal, false otherwise
     */
    public boolean equals(Object obj)
    {
        SequenceAccession accid = (SequenceAccession)obj;
        return this.accid.equals(accid.getAccid());
    }

    /**
     * override base class metho fro creating strings
     * @return a string representation of this instance
     */
    public String toString()
    {

        return this.accid;
    }

}