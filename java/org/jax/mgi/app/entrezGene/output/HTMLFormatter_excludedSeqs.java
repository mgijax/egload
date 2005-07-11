package org.jax.mgi.app.entrezGene.output;

import org.jax.mgi.shr.ioutils.OutputFormatter;
import org.jax.mgi.shr.ioutils.OutputDataFile;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.exception.MGIException;
import org.jax.mgi.shr.config.EntrezGeneCfg;
import org.jax.mgi.shr.dla.output.HTMLFormatter;
import org.jax.mgi.app.entrezGene.*;

/**
 * is a class which extends HTMLFormatter from the lib_java_dla product and
 * provides functionally for rendering EntrezGene buckets in HTML
 * specific to those reports which contain excluded sequence data
 * @has nothing
 * @does formats output in HTML
 * @company The Jackson Laboratory
 * @author M Walker
 *
 */


public class HTMLFormatter_excludedSeqs extends HTMLFormatter
{

    /**
     * counstructor
     * @throws ConfigException thrown if there is an error accessing the
     * configuration
     */
    public HTMLFormatter_excludedSeqs()
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
            "<TR " + HTMLFormatterConstants.TR_ATTRIBUTES + "><TD>Accession" +
            "</TD><TD>Type</TD><TD>MGI Markers</TD><TD>" +
            "EntrezGenes</TD></TR>" +
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

        // AJ421478	DNA	MGI:1203732,MGI:1922314,MGI:1926128	75064,20502,78878
        String s= (String)data;
        String[] fields = s.split(OutputDataFile.TAB);
        String accid = fields[0];
        String type = fields[1];
        String mgiMarkersText = fields[2];
        String entrezGenesText = fields[3];

        String markers_html = null;
        String entrezGenes_html = null;
        if (!mgiMarkersText.equals("None"))
            markers_html = super.formatAccidList(mgiMarkersText);
        else  // mgiMarkers == 'None'
            markers_html = mgiMarkersText;

        if (!entrezGenesText.startsWith("None"))
            entrezGenes_html = super.formatEntrezGeneList(entrezGenesText);
        else  // mgiMarkers == 'None'
            entrezGenes_html = entrezGenesText;


        String accid_html = super.formatAccidAnchorTag(accid);

        String rowstart = "<TR>" + OutputDataFile.CRT;
        String rowend = "</TR>" + OutputDataFile.CRT;

        String s2 = rowstart + "<TD>" + accid_html + "</TD><TD>" +
            type + "</TD><TD>" + markers_html + "</TD><TD>" +
            entrezGenes_html + "</TD>" + rowend;

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
