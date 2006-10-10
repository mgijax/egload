package org.jax.mgi.app.entrezGene.output;

import org.jax.mgi.shr.ioutils.OutputDataFile;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.dla.output.HTMLFormatter;


/**
 * is a class which extends HTMLFormatter from the lib_java_dla product and
 * provides functionally for rendering EntrezGene buckets in HTML
 * specific to those reports which contain only contain MGI markers
 * (for example one to zero buckets).
 * @has nothing
 * @does formats output in HTML
 * @company The Jackson Laboratory
 * @author M Walker
 *
 */


public class HTMLFormatter_mgiBucket extends HTMLFormatter
{



    /**
     * counstructor
     * @throws ConfigException thrown if there is an error accessing the
     * configuration
     */
    public HTMLFormatter_mgiBucket()
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
            "<TR " + HTMLFormatterConstants.TR_ATTRIBUTES + "><TD>MGI " +
            "Marker</TD><TD>Symbol</TD><TD>Chromosome</TD><TD>" +
            "Associated Sequences</TD><TD>Marker Type</TD></TR>" +
            OutputDataFile.CRT;
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
        String mgiID = fields[0];
        String mgiSymbol = fields[1];
        String mgiChromosome = fields[2];
        String sequences = fields[3];
        String markerType = fields[4];
        String mgiID_html = super.formatAccidAnchorTag(mgiID);

        String sequences_html = super.formatAccidSVA(sequences);

        String rowstart = "<TR>" + OutputDataFile.CRT;
        String rowend = "</TR>" + OutputDataFile.CRT;

        String s2 = rowstart + "<TD>" + mgiID_html + "</TD><TD>" +
            mgiSymbol + "</TD><TD>" + mgiChromosome + "</TD><TD>" +
            sequences_html + "</TD><TD>" + markerType + "</TD>" + rowend;

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
