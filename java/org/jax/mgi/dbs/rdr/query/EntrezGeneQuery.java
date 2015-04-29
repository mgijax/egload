package org.jax.mgi.dbs.rdr.query;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;


import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.shr.dbutils.ObjectQuery;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.RowDataInterpreter;
import org.jax.mgi.shr.dbutils.RowReference;
import org.jax.mgi.shr.dbutils.MultiRowInterpreter;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.app.entrezGene.AccessionClassifier;
import org.jax.mgi.app.entrezGene.SequenceAccession;
import org.jax.mgi.app.entrezGene.EntrezGeneBucketizable;
import org.jax.mgi.app.entrezGene.Constants;


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
        /**
         * gets geneids and sequence association data from RADAR for mouse
         */
        return "select i.geneID as geneID , x.dbxrefid as mgiID, " +
            "COALESCE(a.rna, '-'), COALESCE(a.genomic, '-'), " +
            "COALESCE(a.protein, '-'), i.chromosome, i.symbol " +
            "from DP_EntrezGene_Info i " +
		"left outer join DP_EntrezGene_Accession a, DP_EntrezGene_DBXRef x " +
            "where lower(i.geneID) = lower(a.geneID) " +
            "and i.taxID = 10090 " +
	    "and lower(i.geneID) = lower(x.geneID) " +
	    "and lower(x.dbXrefID) like 'mgi:%' " +
	    "union " +
            "select i.geneID as geneID, '-' as mgiID, " +
            "COALESCE(a.rna, '-'), COALESCE(a.genomic, '-'), " +
            "COALESCE(a.protein, '-'), i.chromosome, i.symbol " +
            "from DP_EntrezGene_Info i " +
		"left outer join DP_EntrezGene_Accession a " +
            "where lower(i.geneID) = lower(a.geneID) " +
            "and i.taxID = 10090 " +
	    "and not exists (select 1 from DP_EntrezGene_DBXRef x " +
	    "where lower(i.geneID) = lower(x.geneID) " +
	    "and lower(x.dbXrefID) like 'mgi:%') " +
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
                egene.chromosome = commonElements.chromosome;
                egene.symbol = commonElements.symbol;

                egene.addMGIID(new SequenceAccession(commonElements.mgiID,
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
                    egene.addGenBankSequence(acc);
                }
                else if (seqCategory.equals(Constants.XM))
                {
                    egene.addXMSequence(acc);
                }
                else if (seqCategory.equals(Constants.XR))
                {
                    egene.addXRSequence(acc);
                }
                else if (seqCategory.equals(Constants.XP))
		{
		     egene.addXPSequence(acc);
		}
                else if (seqCategory.equals(Constants.NM))
		{
		     egene.addNMSequence(acc);
		}
                else if (seqCategory.equals(Constants.NR))
		{
		     egene.addNRSequence(acc);
		}
                else if (seqCategory.equals(Constants.NP))
		{
		    egene.addNPSequence(acc);
		}
                else if (seqCategory.equals(Constants.NG))
		{
		    egene.addNGSequence(acc);
		}
		else if (seqCategory.equals(Constants.NT))
                {
                    egene.addNTSequence(acc);
                }
		else if (seqCategory.equals(Constants.NW))
                {
                    egene.addNWSequence(acc);
                }
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
        private String symbol = null;

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
            return super.getMGIIDs().toString();
        }

        public String getSymbol()
        {
            return this.symbol;
        }

        public Set getNMs()
        {
	    if (super.getNMSequences() == null)
                return new HashSet();
            else
                return super.getNMSequences();
        }

        public Set getNRs()
        {
	    if (super.getNRSequences() == null)
                return new HashSet();
            else
                return super.getNRSequences();
        }

        public Set getNPs()
        {
	    if (super.getNPSequences() == null)
                return new HashSet();
            else
                return super.getNPSequences();
        }

        public Set getNGs()
        {
	    if (super.getNGSequences() == null)
                return new HashSet();
            else
                return super.getNGSequences();
        }
        public Set getNTs()
        {
            if (super.getNTSequences() == null)
                return new HashSet();
            else
                return super.getNTSequences();
        }

        public Set getNWs()
        {
            if (super.getNWSequences() == null)
                return new HashSet();
            else
                return super.getNWSequences();
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
	    if (super.getXPSequences() == null)
                return new HashSet();
            else
                return super.getXPSequences();
        }

        public Set getGenBankSeqs()
        {
            if (super.getGenBankSequences() == null)
                return new HashSet();
            else
                return super.getGenBankSequences();
        }

        public HashSet getAllRefSeqSequences()
        {
            HashSet refseqSeqs = new HashSet();
            refseqSeqs.addAll(super.getNMSequences());
            refseqSeqs.addAll(super.getNRSequences());
            refseqSeqs.addAll(super.getNPSequences());
            refseqSeqs.addAll(super.getNGSequences());
            refseqSeqs.addAll(super.getNTSequences());
            refseqSeqs.addAll(super.getNWSequences());
            refseqSeqs.addAll(super.getXMSequences());
            refseqSeqs.addAll(super.getXRSequences());
            refseqSeqs.addAll(super.getXPSequences());
            return refseqSeqs;
        }

        public HashSet getAllSequences()
        {
            HashSet allSeqs = new HashSet();
            allSeqs.addAll(super.getGenBankSequences());
            allSeqs.addAll(getAllRefSeqSequences());
            return allSeqs;
        }


        public String toString()
        {
            HashSet refseqs = getAllRefSeqSequences();

            return id + " : " + chromosome + " | " + Constants.MGIID +
                " = " + super.getMGIIDs().toString() + " | " +
                Constants.GENBANK + " = " + super.getGenBankSequences().toString() +
                " | " + Constants.REFSEQ + " = " + getAllRefSeqSequences().toString();
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

