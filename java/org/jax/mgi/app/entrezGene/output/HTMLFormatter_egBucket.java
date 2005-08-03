package org.jax.mgi.app.entrezGene.output;

import org.jax.mgi.shr.ioutils.OutputDataFile;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.dla.output.HTMLFormatter;

/**
 * is a class which extends HTMLFormatter from the lib_java_dla product and
 * provides functionality for rendering zero to one buckets
 * @has nothing
 * @does formats output in HTML
 * @company The Jackson Laboratory
 * @author M Walker
 *
 */


public class HTMLFormatter_egBucket extends HTMLFormatter
{

    /**
     * counstructor
     * @throws ConfigException thrown if there is an error accessing the
     * configuration
     */
    public HTMLFormatter_egBucket()
    throws ConfigException
    {
        super();

    }

    /**
     * get the header text for this format
     * @return header text
     */
    public String getHeader()
    {
        String stdHead = super.getStandardHeader();
        return stdHead + OutputDataFile.CRT +
            "<TABLE " + HTMLFormatterConstants.TABLE_ATTRINUTES + ">" +
            "<TR " + HTMLFormatterConstants.TR_ATTRIBUTES + "><TD>Entrez " +
            "Gene</TD><TD>Symbol</TD><TD>Chromosome</TD><TD>" +
            "Associated Sequences</TD></TR>" +
            OutputDataFile.CRT;
    }

    /**
     * get the trailer text for this format
     * @return trailer text
     */
    public String getTrailer()
    {
        String stdTrailer = super.getTrailer();
        return "</TABLE>" + OutputDataFile.CRT + stdTrailer;
    }


    /**
     * format the given object in HTML
     * @param data object to format
     * @return formatted string
     */
    public String format(Object data)
    {
        String s= (String)data;
        String[] fields = s.split(OutputDataFile.TAB);
        String egID = fields[0];
        String egSymbol = fields[1];
        String egChromosome = fields[2];
        String sequences = fields[3];

        String egID_html = super.formatEntrezGeneAnchorTag(egID);
        String oldMGIID_html = null;
        String sequences_html = super.formatAccidSVA(sequences);

        String rowstart = "<TR>" + OutputDataFile.CRT;
        String rowend = "</TR>" + OutputDataFile.CRT;

        String s2 = rowstart + "<TD>" + egID_html + "</TD><TD>" + egSymbol +
            "</TD><TD>" + egChromosome +
            "</TD><TD>" + sequences_html + "</TD>" + rowend;

        return s2;
    }

    /**
     * required by the HTMLFormatter base class, but not used
     */
    public void postprocess() {}
    /**
     * required by the HTMLFormatter base class, but not used
     */
    public void preprocess() {}


}