package org.jax.mgi.app.entrezGene;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;

import org.jax.mgi.shr.dla.loader.DLALoader;
import org.jax.mgi.shr.dbutils.DataIterator;
import org.jax.mgi.shr.dbutils.BatchProcessor;
import org.jax.mgi.shr.exception.MGIException;
import org.jax.mgi.shr.timing.Stopwatch;
import org.jax.mgi.dbs.mgd.query.MGIMarkerQuery;
import org.jax.mgi.dbs.rdr.query.EntrezGeneQuery;
import org.jax.mgi.dbs.mgd.lookup.EntrezGeneHistory;
import org.jax.mgi.shr.config.EntrezGeneCfg;
import org.jax.mgi.shr.ioutils.OutputManager;


/**
 *
 * is a DLALoader extended to load Entrez Gene data
 * @has an EntrezGeneCfg class for configuration
 * @does clusters data between Entrez Gene and MGI based on associations
 * between them and loads one to one associations into the database and
 * creates reports
 * @company The Jackson Laboratory
 * @author M Walker
 *
 */


public class EntrezGeneLoader
    extends DLALoader
{
    /**
     * configuration
     */
    protected EntrezGeneCfg egCfg = null;

    /**
     * bucketizer implementation
     */
    private EntrezGeneBucketizer bucketizer = null;

    /**
     * a query for MGI markers
     */
    private MGIMarkerQuery markerQuery = null;

    /**
     * a query for EntrezGene data
     */
    private EntrezGeneQuery entrezGeneQuery = null;

    /**
     * a cached lookup for finding current associations between Entrez Gene
     * and MGI markers
     */
    private EntrezGeneHistory entrezGeneHistory = null;

    /**
     * the list of names of attributes used in the Bucketizer 
     */
    private String[] sequenceGroups = {Constants.GENBANK, 
		Constants.MGIID, Constants.XM, Constants.XR,
                Constants.XP, Constants.NM, Constants.NR, Constants.NP,
                Constants.NG};
    /**
     * stopwatch for timing
     */
    private Stopwatch loadStopWatch = new Stopwatch();
    /**
     * Runtime for statusing memory usage
     */
    private Runtime rtime = Runtime.getRuntime();
    /**
     * starting memory usage
     */
    private long freememStart = rtime.freeMemory();


    /**
     * runs the Entrez Gene query and the MGI marker query and initializes the
     * EntrezGeneBucketizer
     * @assumes nothing
     * @effects the loader will be initialized and memory caches will be
     * created
     * @throws MGIException thrown if errors occur during initialization
     */
    public void initialize()
        throws MGIException
    {
        loadStopWatch.start();

        this.egCfg = new EntrezGeneCfg();

        if (this.egCfg.getOkToPerformHistory().booleanValue())
        {
            // obtain current entrez gene/ mgi associations
            this.entrezGeneHistory = new EntrezGeneHistory();
            this.entrezGeneHistory.initCache();
        }

        // run mgd prepartion by deleting accession ids for SwissProt & Entrez Gene
        if (!this.egCfg.getOkToPreventDelete().booleanValue())
        {
            printStats("deleting existing accession ids...");
            BatchProcessor batch = super.loadDBMgr.getBatchProcessor();
            batch.addScriptBatch(Constants.DELETE_SCRIPT);
            batch.executeBatch();
            printStats("finished deleting existing accession ids.");
        }

        printStats("database prepared");

        markerQuery = new MGIMarkerQuery();
        entrezGeneQuery = new EntrezGeneQuery();
        DataIterator entrezGeneData = entrezGeneQuery.execute();
        DataIterator markerData = markerQuery.execute();

        bucketizer = new EntrezGeneBucketizer(entrezGeneData,
                                              markerData,
                                              sequenceGroups,
                                              super.loadStream,
                                              this.entrezGeneHistory);
        printStats("bucketizer initialized");
    }

    /**
     * preprocess method required by the DLALoader but in this case does
     * nothing
     */
    public void preprocess()
    {
    }

    /**
     * runs the EntrezGeneBucketizer
     * @assumes nothing
     * @effects the EntrezGeneBucketizer will be run
     * @throws MGIException thrown if there is an error running the
     * EntrezGeneBucketizer
     */
    public void run()
        throws MGIException
    {
        bucketizer.run(Constants.PROVIDER_MGI, Constants.PROVIDER_ENTREZGENE);
        printStats("run complete");
	bucketizer.getInvalidGMIds();
	printStats("reporting invalid NCBI GM Ids complete");
    }

    /**
     * closes the SQLStream to persist data to the database
     * @assumes nothing
     * @effects data will be persisted
     * @throws MGIException thrown if there is an error accessing the database
     */
    public void postprocess()
        throws MGIException
    {
        super.loadStream.close();
        printStats("post process complete");
    }


    /**
     * print timestamps to the logger
     * @param header the header text for the log entry
     */
    private void printStats(String header)
    {
        String text = header + "  |  ";
        long freememEnd = rtime.freeMemory();
        loadStopWatch.stop();
        double totalProcessTime = loadStopWatch.time();
        text = text.concat("Total bytes: " + new Double(freememStart - freememEnd));
        text = text.concat("  |  Total time: " + totalProcessTime);
        super.logger.logInfo(text);
        loadStopWatch.start();
    }
}
