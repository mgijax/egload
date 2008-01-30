package org.jax.mgi.app.entrezGene;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * is a class that can identify types of accession numbers by their
 * character patterns
 * @has nothing
 * @does identifies accession types (NM, NR, NP, XM, XR, XP, GenBank, MGI)
 * by applying pattern matching on the accession characters
 * @company Jackson Laboratory
 * @author M Walker
 *
 */

public class AccessionClassifier {

    protected Pattern nmPattern = Pattern.compile("NM_.*");
    protected Pattern npPattern = Pattern.compile("NP_.*");
    protected Pattern nrPattern = Pattern.compile("NR_.*");
    protected Pattern ngPattern = Pattern.compile("NG_.*");
    protected Pattern xmPattern = Pattern.compile("XM_.*");
    protected Pattern xrPattern = Pattern.compile("XR_.*");
    protected Pattern xpPattern = Pattern.compile("XP_.*");
    protected Pattern mgiPattern = Pattern.compile("MGI:.*");
    protected Pattern genbankPattern =
        Pattern.compile("[A-Z]{1}[0-9]{5}|[A-Z]{2}[0-9]{6}");

    /**
     * obtain a named type from org.jax.mgi.app.entrezGene.Constants based on
     * the given accession id
     * @param accid the accession id to classify
     * @return the type of accession
     */
    public String classify(String accid)
    {
        Matcher mgiMatcher =
            mgiPattern.matcher(accid.toUpperCase());
        Matcher nmMatcher =
            nmPattern.matcher(accid.toUpperCase());
        Matcher nrMatcher =
            nrPattern.matcher(accid.toUpperCase());
        Matcher npMatcher =
            npPattern.matcher(accid.toUpperCase());
        Matcher ngMatcher =
            ngPattern.matcher(accid.toUpperCase());
        Matcher xmMatcher =
            xmPattern.matcher(accid.toUpperCase());
        Matcher xpMatcher =
            xpPattern.matcher(accid.toUpperCase());
        Matcher xrMatcher =
            xrPattern.matcher(accid.toUpperCase());

        Matcher genbankMatcher =
            genbankPattern.matcher(accid.toUpperCase());

        if (mgiMatcher.find())
             return Constants.MGIID;
        else if (xmMatcher.find())
            return Constants.XM;
        else if (xrMatcher.find())
            return Constants.XR;
        else if (xpMatcher.find())
            return Constants.XP;
        else if (nmMatcher.find())
            return Constants.NM;
        else if (nrMatcher.find())
            return Constants.NR;
        else if (npMatcher.find())
            return Constants.NP;
        else if (ngMatcher.find())
            return Constants.NG;
        else if (genbankMatcher.find())
             return Constants.GENBANK;
        else
            return Constants.UNKNOWN;

    }


}
