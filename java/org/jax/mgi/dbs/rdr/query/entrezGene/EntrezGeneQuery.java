package org.jax.mgi.dbs.rdr.query.entrezGene;

import java.util.*;
import java.util.regex.*;

import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.shr.cache.KeyValue;
import org.jax.mgi.shr.cache.MappedOrderedIntegerToStrings;
import org.jax.mgi.shr.dbutils.ObjectQuery;
import org.jax.mgi.shr.dbutils.SQLDataManager;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.DBExceptionFactory;
import org.jax.mgi.shr.dbutils.RowDataInterpreter;
import org.jax.mgi.shr.dbutils.RowReference;
import org.jax.mgi.shr.dbutils.BatchProcessor;
import org.jax.mgi.shr.dbutils.MultiRowInterpreter;
import org.jax.mgi.shr.dbutils.InterpretException;
import org.jax.mgi.shr.sva.SVASet;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.exception.MGIException;
import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.shr.bucketizer.Bucketizable;
import org.jax.mgi.dbs.mgd.lookup.entrezGene.*;
import org.jax.mgi.app.entrezGene.*;
import org.jax.mgi.dbs.mgd.query.entrezGene.*;


/**
 *
 * is an extension of ObjectQuery for specifically getting Entrez Gene data
 * from the database. The inner class EntrezGene is used to store the
 * results from the query. It has two types of attributes: class attributes
 * (typical to any class) and bucketizable attributes (those attributes used
 * in the bucketizing algorithm)
 * @has an AccessionClassifier for classifying the sequence types returned
 * from the database
 * @does provides the query and RowDataInterpreter for obtaining
 * EntrezGene objects from the database
 * @company The Jackson Laboratory
 * @author M Walker
 *
 */

public class EntrezGeneQuery extends ObjectQuery
{

    /**
     * an AccessionClassifier for classifying sequences returned from the
     * query provided in getQuery()
     */
    protected AccessionClassifier accidClassifier =
        new AccessionClassifier();

    /**
     * Constructor
     * @assumes Nothing
     * @effects Nothing
     * @throws CacheException thrown if there is an error accessing the cache
     * @throws ConfigException thrown of there is an error accessing the
     * configuration
     * @throws DBException thrown if there is an error accessing the database
     */

    public EntrezGeneQuery() throws CacheException, ConfigException,
        DBException
    {
        super(SQLDataManagerFactory.getShared(SchemaConstants.RADAR));
    }


    /**
     * get the query string
     * @assumes nothing
     * @effects nothing
     * @return the query string
     */
    public String getQuery()
    {
        return "select geneID = i.geneID, mgiID = i.locusTag, " +
            "COALESCE(a.rna, '-'), COALESCE(a.genomic, '-'), " +
            "COALESCE(a.protein, '-'), i.chromosome, i.symbol " +
            "from DP_EntrezGene_Accession a, DP_EntrezGene_Info i " +
            "where i.geneID *= a.geneID " +
            "and i.taxID = 10090 " +
            //"and i.geneID = '193543' " +
            "order by geneID";
    }


    /**
     * returns a RowDataInterpreter which can interpret the results from the
     * query provided in getQuery(). The object returned is the extended
     * version of the RowDataInterpreter called MultiRowInterpreter which
     * is used for processing cartesian products such that many rows represent
     * a single object
     * @return a RowDataInterpreter for creating EntrezGene objects
     */
    public RowDataInterpreter getRowDataInterpreter()
    {
        class Interpreter
            implements MultiRowInterpreter
        {
            /**
             * process one row from the database
             * @param row the database row
             * @return an object which will later be processed by the
             * interpretRows() method
             * @throws DBException thrown if there is an error with the
             * database
             */
            public Object interpret(RowReference row) throws DBException
            {
                EntrezGeneRow egr = new EntrezGeneRow();
                egr.id = row.getString(1);
                egr.mgiID = row.getString(2);
                if (!row.getString(3).equals("-"))
                    egr.rna = new SequenceAccession(row.getString(3),
                        SequenceAccession.RNA);
                if (!row.getString(4).equals("-"))
                    egr.dna = new SequenceAccession(row.getString(4),
                        SequenceAccession.DNA);
                if (!row.getString(5).equals("-"))
                {
                    String category =
                        accidClassifier.classify(
                        row.getString(5).toUpperCase());
                    if (category.equals(Constants.XP) ||
                        category.equals(Constants.NP))
                        egr.prot = new SequenceAccession(row.getString(5),
                            SequenceAccession.PROTEIN);
                }
                egr.chromosome = row.getString(6);
                egr.symbol = row.getString(7);
                return egr;
            }
            /**
             * processes a Vector of objects to create an EntrezGene object
             * @param v the vector of objects obtained from returns from
             * subsequent calls to the interpret() method
             * @return the new EntrezGene object
             */
            public Object interpretRows(Vector v)
            {
                EntrezGeneRow commonElements = (EntrezGeneRow)v.get(0);
                EntrezGene egene = new EntrezGene(commonElements.id);
                egene.mgiID = commonElements.mgiID;
                egene.chromosome = commonElements.chromosome;
                egene.symbol = commonElements.symbol;

                egene.addMGIID(new SequenceAccession(egene.mgiID,
                    SequenceAccession.MGI));

                for (int i = 0; i < v.size(); i++)
                {
                    EntrezGeneRow row = (EntrezGeneRow)v.get(i);
                    if (row.rna != null)
                        storeSequence(egene, row.rna);
                    if (row.dna != null)
                        storeSequence(egene, row.dna);
                    if (row.prot != null)
                        storeSequence(egene, row.prot);
                }
                return egene;
            }

            /**
             * return a key object which is used for grouping concurrent
             * rows from the result set
             * @param row the database row
             * @return the first column of the row
             * @throws DBException thrown if there is an error reading the
             * result set
             */
            public Object interpretKey(RowReference row) throws DBException
            {
              return row.getString(1);
            }
            /**
             * store the sequence data as EntrezGene attributes while passing
             * some sequence data to the EntrezGene object as Bucketizable
             * data used by the AbstractBucketizer
             * @param egene
             * @param acc
             */
            private void storeSequence(EntrezGene egene,
                                         SequenceAccession acc)
            {
                String seqid = acc.getAccid();
                String seqCategory = accidClassifier.classify(seqid);

                if (seqCategory.equals(Constants.GENBANK))
                {
                    egene.genbank.add(acc);
                    egene.addGenBankSequence(acc);
                }
                else if (seqCategory.equals(Constants.XM))
                {
                    egene.xmSeqs.add(acc);
                    egene.addXMSequence(acc);
                }
                else if (seqCategory.equals(Constants.XR))
                {
                    egene.xrSeqs.add(acc);
                    egene.addXRSequence(acc);
                }
                else if (seqCategory.equals(Constants.XP))
                    egene.xpSeqs.add(acc);
                else if (seqCategory.equals(Constants.NM))
                    egene.nmSeqs.add(acc);
                else if (seqCategory.equals(Constants.NR))
                    egene.nrSeqs.add(acc);
                else if (seqCategory.equals(Constants.NP))
                    egene.npSeqs.add(acc);
            }

        }
        return new Interpreter();
    }

    /**
     * is a plain old java object for EntrezGene data
     * @has nothing
     * @does nothing
     * @company The Jackson Laboratory
     * @author M Walker
     *
     */
    public class EntrezGene
        extends EntrezGeneBucketizable
    {
        private String chromosome = null;
        private String mgiID = null;
        private String symbol = null;
        private HashSet genbank = new HashSet();
        private HashSet nmSeqs = new HashSet();
        private HashSet nrSeqs = new HashSet();
        private HashSet npSeqs = new HashSet();

        private HashSet xmSeqs = new HashSet();
        private HashSet xrSeqs = new HashSet();
        private HashSet xpSeqs = new HashSet();

        public EntrezGene(String id)
        {
            super(id, Constants.PROVIDER_ENTREZGENE);
        }

        public String getChromosome()
        {
            return this.chromosome;
        }

        public String getMGIID()
        {
            return this.mgiID;
        }

        public String getSymbol()
        {
            return this.symbol;
        }

        public Set getNMs()
        {
            return this.nmSeqs;
        }

        public Set getNRs()
        {
            return this.nrSeqs;
        }

        public Set getNPs()
        {
            return this.npSeqs;
        }

        public Set getXMs()
        {
            if (super.getXMSequences() == null)
                return new HashSet();
            else
                return super.getXMSequences();
        }

        public Set getXRs()
        {
            if (super.getXRSequences() == null)
                return new HashSet();
            else
                return super.getXRSequences();
        }

        public Set getXPs()
        {
            return this.xpSeqs;
        }

        public Set getGenBankSeqs()
        {
            if (super.getGenBankSequences() == null)
                return new HashSet();
            else
                return super.getGenBankSequences();
        }


        public String toString()
        {
            HashSet refseqs = new HashSet();
            refseqs.addAll(nmSeqs);
            refseqs.addAll(nrSeqs);
            refseqs.addAll(npSeqs);
            refseqs.addAll(xmSeqs);
            refseqs.addAll(xrSeqs);
            refseqs.addAll(xpSeqs);

            return id + " : " + chromosome + " | " + Constants.MGIID +
                " = " + mgiID.toString() + " | " +
                Constants.GENBANK + " = " + genbank.toString() +
                " | " + Constants.REFSEQ + " = " + refseqs.toString();
        }
    }

    /**
     *
     * is a plain old java object for holding one row of data from the query
     * @has nothing
     * @does nothing
     * @company Jackson Laboratory
     * @author M Walker
     *
     */
    public class EntrezGeneRow
    {
      public String id = null;
      public String mgiID = null;
      public SequenceAccession dna = null;
      public SequenceAccession rna = null;
      public SequenceAccession prot = null;
      public String chromosome = null;
      public String symbol = null;

      public String toString()
      {
        return id + "|" + mgiID + "|" + chromosome + "|" +
            dna + "|" + rna + "|" + prot;
      }
    }



}



