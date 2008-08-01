package org.jax.mgi.app.entrezGene;

import java.util.Set;

import org.jax.mgi.shr.bucketizer.SimpleBucketizable;



/**
 * is an extension of the SimpleBucketizable class from the package
 * org.jax.mgi.shr.bucketizable which provides the methods required by
 * the Bucketizable interface. Both EntrezGene and MgiMarker objects extend
 * this class so that they can be processed through the Bucketizer algorithm
 * @has a set of names identifying the set valued attributes that will be
 * used in the Bucketizer algorithm, an id and a provider id
 * @does implements the Bucketizable interface for use in 'bucketizing' via
 * the AbstractBucketizer
 * @company Jackson Laboratory
 * @author M Walker
 *
 */

public class EntrezGeneBucketizable extends SimpleBucketizable
{
    /**
     * the names of the set valued attributes that will be used in
     * the Bucketizer
     */
    protected static String[] svaNames =
        {Constants.GENBANK, Constants.MGIID, Constants.XM, Constants.XR,
		Constants.XP, Constants.NM, Constants.NR, Constants.NP,
		Constants.NG};

    /**
     * constructor
     * @param id the id of this instance
     * @param provider the provider name of this instance
     */
    public EntrezGeneBucketizable(String id, String provider)
    {
        super(id, provider, svaNames);
    }

    /**
     * get the GenBank sequences for this instance
     * @return GenBank sequences
     */
    public Set getGenBankSequences()
    {
        return super.svaSet.getSVA(Constants.GENBANK);
    }

    /**
     * get the MGI ids for this instance
     * @return MG ids
     */
    public Set getMGIIDs()
    {
        return super.svaSet.getSVA(Constants.MGIID);
    }

    /**
     * get the XM sequences from this instance
     * @return XM sequences
     */
    public Set getXMSequences()
    {
        return super.svaSet.getSVA(Constants.XM);
    }

    /**
     * get the XR sequences from this instance
     * @return XR sequences
     */
    public Set getXRSequences()
    {
        return super.svaSet.getSVA(Constants.XR);
    }

    /**
     * get the XP sequences from this instance
     * @return XP sequences
     */
    public Set getXPSequences()
    {
        return super.svaSet.getSVA(Constants.XP);
    }

    /**
     * get the NM sequences from this instance
     * @return NM sequences
     */
    public Set getNMSequences()
    {
        return super.svaSet.getSVA(Constants.NM);
    }

    /**
     * get the NR sequences from this instance
     * @return NR sequences
     */
    public Set getNRSequences()
    {
        return super.svaSet.getSVA(Constants.NR);
    }

    /**
     * get the NP sequences from this instance
     * @return NP sequences
     */
    public Set getNPSequences()
    {
        return super.svaSet.getSVA(Constants.NP);
    }

    /**
     * get the NG sequences from this instance
     * @return NG sequences
     */
    public Set getNGSequences()
    {
        return super.svaSet.getSVA(Constants.NG);
    }

    /**
     * add a GenBank sequence to this instance
     * @param accid the GenBank sequence to add
     */
    public void addGenBankSequence(SequenceAccession accid)
    {
        super.svaSet.addSVAMember(Constants.GENBANK, accid);
    }

    /**
     * add a MGI id to this instance
     * @param mgiID the MGI id to add
     */
    public void addMGIID(SequenceAccession mgiID)
    {
        super.svaSet.addSVAMember(Constants.MGIID, mgiID);
    }

    /**
     * add an XM sequence to this instance
     * @param accid the XM sequence to add
     */
    public void addXMSequence(SequenceAccession accid)
    {
        super.svaSet.addSVAMember(Constants.XM, accid);
    }

    /**
     * add an XR sequence to this instance
     * @param accid the XR sequence to add
     */
    public void addXRSequence(SequenceAccession accid)
    {
        super.svaSet.addSVAMember(Constants.XR, accid);
    }

    /**
     * add an XP sequence to this instance
     * @param accid the XP sequence to add
     */
    public void addXPSequence(SequenceAccession accid)
    {
        super.svaSet.addSVAMember(Constants.XP, accid);
    }

    /**
     * add an NM sequence to this instance
     * @param accid the NM sequence to add
     */
    public void addNMSequence(SequenceAccession accid)
    {
        super.svaSet.addSVAMember(Constants.NM, accid);
    }

    /**
     * add an NR sequence to this instance
     * @param accid the NR sequence to add
     */
    public void addNRSequence(SequenceAccession accid)
    {
        super.svaSet.addSVAMember(Constants.NR, accid);
    }

    /**
     * add an NP sequence to this instance
     * @param accid the NP sequence to add
     */
    public void addNPSequence(SequenceAccession accid)
    {
        super.svaSet.addSVAMember(Constants.NP, accid);
    }

    /**
     * add an NG sequence to this instance
     * @param accid the NG sequence to add
     */
    public void addNGSequence(SequenceAccession accid)
    {
        super.svaSet.addSVAMember(Constants.NG, accid);
    }

    /**
     * get the String representation of this instance
     * @return String representation
     */
    public String svaString()
    {
        return super.toString();
    }


}
