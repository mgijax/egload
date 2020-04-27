# Program:  formatreports.py
#
# Description:
#
#	Translates text output files from EntrezGene load into HTML format
#
# Inputs:
#	Text files found in EntrezGene load's reports directory
#
# Outputs:
#	HTML formatted files (one per .txt file)
#	We create anchors for MGI ids to the public WI accession report
#	External anchors are created for GenBank, RefSeq and EntrezGene ids
#
# History
#
# 04/09/2012	lec	
#	- MGI 5.0/mgiAnchor/create_accession_anchor: fix call
#
# 03/21/2011	lec
#	- TR10635/ZERO_ONE_MGIID_OUTFILE_NAME, ZERO_ONE_NOMGIID_OUTFILE_NAME
#	  move the formatting of these files from ".sh" to ".py"
#
# 12/19/2006	lec
#	- this was written in response to 
#         eliminating the html formatting code in the java frameworks
#	  which was extremely complicated and hard to maintain
#

import sys 
import os
import re
import db
import reportlib

CRT = reportlib.CRT
TAB = reportlib.TAB

tableStart = '<TABLE BORDER="1" CELLPADDING="5"><TR ALIGN="center" STYLE="font-weight:bold">'
tableEnd = '</TABLE>'
anchorEnd = '</A>'

# output file names defined in the configuration file and used in the loader
# note that the 1-1 bucket is not translated into html format

egFileName = os.environ['ZERO_ONE_OUTFILE_NAME']
egmgiFileName = os.environ['ZERO_ONE_MGIID_OUTFILE_NAME']
egnomgiFileName = os.environ['ZERO_ONE_NOMGIID_OUTFILE_NAME']

column8Files = ['ONE_N_OUTFILE_NAME', 'N_ONE_OUTFILE_NAME', 'N_M_OUTFILE_NAME', 'CHR_MIS_OUTFILE_NAME']
column5Files = ['ONE_ZERO_OUTFILE_NAME']
column2Files = ['GM_NOTIN_OUTFILE_NAME']

# _ActualDB_key for URLs we need to use
EG_URL_KEY = 57
GENBANK_URL_KEY = 12
REFSEQ_URL_KEY = 35

# tags used in Constants.java for tagging specific ids used in the load
mgiTag = 'MGIID'
genbankTag = 'GenBank'
refseqTag = ['XR', 'XM']

urls = {}	# dictionary of url key:url

def init():
    #
    # initialize database, dictionaries, etc.
    #

    global urls

    # initialize urls

    urls = {}
    results = db.sql('select _ActualDB_key, url from ACC_ActualDB ' + \
        'where _ActualDB_key in (%d,%d,%d)' % (EG_URL_KEY, GENBANK_URL_KEY, REFSEQ_URL_KEY), 'auto')
    for r in results:
        key = r['_ActualDB_key']
        value = r['url']
        urls[key] = '<A HREF="%s">' % (value)

def initFiles(value):
    #
    # open input, html files
    #

    # open input (text) file

    try:
        inFile = open(value, 'r')
        head, tail = os.path.split(value)

        # initialize output (html) file

        htmlFile = reportlib.init(tail, outputdir = os.environ['RPTDIR'], printHeading = None, isHTML = 1)

    except:

        print(('Cannot open file: %s' % (value)))
        sys.exit(1)

    return inFile, htmlFile

def externalAnchor(id, urlKey):
    #
    # return anchor for external id
    #

    html = re.sub('@@@@', id, urls[urlKey])
    return html + id + anchorEnd

def mgiAnchor(id):
    #
    # return anchor for MGI id
    #

    return reportlib.create_accession_anchor(id, 'marker') + id + anchorEnd

def idAnchors(s):
    #
    # return id anchors for each id given
    #
    # the format of "s" is:
    #
    # {MGIID=[MGI:1916101], GenBank=[AK004400, AK083883, AK042549]}
    #

    # save original input line...so we can substitute

    original = s
    anchors = {}

    # parse each "TAG:[]" list into key:value pairs
    # where key = ID, value = anchor

    while s:

        i1 = str.find(s, '[')
        i2 = str.find(s, ']')
        tokens = str.split(s[:i2], '=')

        # clean up the results of the split

        tag = re.sub('{', '', tokens[0])
        ids = str.split(re.sub('\[', '', tokens[1]), ', ')

        # process tags

        # some MGI ids are '-'...don't create an anchor for these

        if tag == mgiTag:
            for i in ids:
                if i != '-':
                    anchors[i] = mgiAnchor(i)

        elif tag == genbankTag:
            for i in ids:
                anchors[i] = externalAnchor(i, GENBANK_URL_KEY)

        elif tag in refseqTag:
            for i in ids:
                anchors[i] = externalAnchor(i, REFSEQ_URL_KEY)

        # we're done if there are no more TAGs to process

        if len(s) < i2 + 3:
            break

        # else, move to the next TAG

        s = s[i2+3:]

    # substitute the ids in the original str.with the anchors

    for a in list(anchors.keys()):
        neworiginal = re.sub(a, anchors[a], original)
        original = neworiginal

    return original

def processEG():
    #
    # process EG-oriented reports
    #

    try:
        inFile = open(egFileName, 'r')
        head, tail = os.path.split(egFileName)

        # initialize output (html) file

        htmlFile = reportlib.init(tail, \
                                  outputdir = os.environ['RPTDIR'], printHeading = None, isHTML = 1)
        egmgiFile = reportlib.init(egmgiFileName, fileExt = '.txt', \
                                   outputdir = os.environ['RPTDIR'], printHeading = None)
        egnomgiFile = reportlib.init(egnomgiFileName, fileExt = '.txt', \
                                   outputdir = os.environ['RPTDIR'], printHeading = None)

    except:

        print(('Problem with file: %s' % (egFileName)))
        print(('Problem with file: %s' % (egmgiFileName)))
        print(('Problem with file: %s' % (egnomgiFileName)))
        sys.exit(1)

    # print html header

    htmlFile.write(tableStart + CRT)
    htmlFile.write('<TD>Entrez Gene</TD>')
    htmlFile.write('<TD>Symbol</TD>')
    htmlFile.write('<TD>Chromosome</TD>')
    htmlFile.write('<TD>Associated Sequences</TD>' + CRT)

    # iterate thru input file

    for line in inFile.readlines():

        tokens = str.split(line, TAB)

        if len(tokens) < 3:
                continue

        egID = tokens[0]
        egSymbol = tokens[1]
        egChromosome = tokens[2];
        sequences = tokens[3];

        htmlFile.write('<TR><TD>' + externalAnchor(egID, EG_URL_KEY) + '</TD>')
        htmlFile.write('<TD>' + egSymbol + '</TD>')
        htmlFile.write('<TD>' + egChromosome + '</TD>')
        htmlFile.write('<TD>' + idAnchors(sequences) + '</TD></TR>')
        htmlFile.write(CRT)

        # if file does not contains MGIids...
        if str.find(sequences, 'MGIID=[-]') > 0:
            egnomgiFile.write(egID + TAB)
            egnomgiFile.write(egSymbol + TAB)
            egnomgiFile.write(egChromosome + TAB)
            egnomgiFile.write(sequences + CRT)

        # if file does contains MGIids...
        else:
            seqs = str.split(sequences, ',')

            # only print sequences with MGI ids
            for s in seqs:
                if str.find(s, 'MGIID=[') >= 0:
                    s = str.replace(s, 'MGIID=[', '')
                    s = str.replace(s, ']', '')
                    s = str.replace(s, '{', '')
                    s = str.replace(s, '}', '')
                    s = str.replace(s, ' ', '')
                    s = str.replace(s, '\n', '')
                    egmgiFile.write(egID + TAB)
                    egmgiFile.write(egSymbol + TAB)
                    egmgiFile.write(egChromosome + TAB)
                    egmgiFile.write(s + CRT)

    htmlFile.write(tableEnd)

    egmgiFile.close()
    egnomgiFile.close()

    inFile.close()
    reportlib.finish_nonps(htmlFile, isHTML = 1)

def processMGI_8columns():
    #
    # process MGI-oriented reports with 8 columns
    #

    # iterate thru files
    
    for b in column8Files:

        value = os.environ[b]
        inFile, htmlFile = initFiles(value)

        # print html header

        htmlFile.write(tableStart + CRT)
        htmlFile.write('<TD>Marker</TD>')
        htmlFile.write('<TD>Symbol</TD>')
        htmlFile.write('<TD>Chromosome</TD>')
        htmlFile.write('<TD>Entrez Gene</TD>')
        htmlFile.write('<TD>Associated Sequences</TD>')
        htmlFile.write('<TD>Marker Type</TD>' + CRT)

        # iterate thru input file

        for line in inFile.readlines():

            tokens = str.split(line, TAB)

            if len(tokens) < 3:
                continue

            mgiID = tokens[0]
            mgiSymbol = tokens[1]
            mgiChromosome = tokens[2]
            egID = tokens[3]
            sequences = tokens[6]
            markerType = tokens[7]

            htmlFile.write('<TR><TD>' + mgiAnchor(mgiID) + '</TD>')
            htmlFile.write('<TD>' + mgiSymbol + '</TD>')
            htmlFile.write('<TD>' + mgiChromosome + '</TD>')
            htmlFile.write('<TD>' + externalAnchor(egID, EG_URL_KEY) + '</TD>')
            htmlFile.write('<TD>' + idAnchors(sequences) + '</TD>')
            htmlFile.write('<TD>' + markerType + '</TD></TR>')
            htmlFile.write(CRT)

        htmlFile.write(tableEnd)

        inFile.close()
        reportlib.finish_nonps(htmlFile, isHTML = 1)

def processMGI_5columns():
    #
    # process MGI-oriented reports w/ 5 columns
    #

    # iterate thru files
    
    for b in column5Files:

        value = os.environ[b]
        inFile, htmlFile = initFiles(value)

        # print html header

        htmlFile.write(tableStart + CRT)
        htmlFile.write('<TD>Marker</TD>')
        htmlFile.write('<TD>Symbol</TD>')
        htmlFile.write('<TD>Chromosome</TD>')
        htmlFile.write('<TD>Associated Sequences</TD>')
        htmlFile.write('<TD>Marker Type</TD>' + CRT)

        # iterate thru input file

        for line in inFile.readlines():

            tokens = str.split(line, TAB)

            if len(tokens) < 3:
                continue

            mgiID = tokens[0]
            mgiSymbol = tokens[1]
            mgiChromosome = tokens[2]
            sequences = tokens[3]
            markerType = tokens[4]

            htmlFile.write('<TR><TD>' + mgiAnchor(mgiID) + '</TD>')
            htmlFile.write('<TD>' + mgiSymbol + '</TD>')
            htmlFile.write('<TD>' + mgiChromosome + '</TD>')
            htmlFile.write('<TD>' + idAnchors(sequences) + '</TD>')
            htmlFile.write('<TD>' + markerType + '</TD></TR>')
            htmlFile.write(CRT)

        htmlFile.write(tableEnd)

        inFile.close()
        reportlib.finish_nonps(htmlFile, isHTML = 1)

def processMGI_2columns():
    #
    # process MGI-oriented reports w/ 2 columns
    #

    # iterate thru files

    for b in column2Files:

        value = os.environ[b]
        inFile, htmlFile = initFiles(value)

        # print html header

        htmlFile.write(tableStart + CRT)
        htmlFile.write('<TD>Marker</TD>' + CRT)
        htmlFile.write('<TD>EG ID</TD>')

        # iterate thru input file

        for line in inFile.readlines():

            tokens = str.split(line, TAB)

            if len(tokens) < 2:
                continue

            egID = tokens[0]
            mgiID = tokens[1]
            htmlFile.write('<TR><TD>' + mgiAnchor(mgiID) + '</TD>')
            htmlFile.write('<TD>' + egID + '</TD>')
            htmlFile.write(CRT)

        htmlFile.write(tableEnd)

        inFile.close()
        reportlib.finish_nonps(htmlFile, isHTML = 1)

#
# main
#

init()
processEG()
processMGI_8columns()
processMGI_5columns()
processMGI_2columns()
sys.exit(0)
