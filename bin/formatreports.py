#!/usr/local/bin/python

#
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
# 12/19/2006	lec
#	- this was written in response to 
#         eliminating the html formatting code in the java frameworks
#	  which was extremely complicated and hard to maintain
#

import sys 
import os
import re
import string
import db
import reportlib

CRT = reportlib.CRT
TAB = reportlib.TAB

tableStart = '<TABLE BORDER="1" CELLPADDING="5"><TR ALIGN="center" STYLE="font-weight:bold">'
tableEnd = '</TABLE>'
anchorEnd = '</A>'

# output file names defined in the configuration file and used in the loader
# note that the 1-1 bucket is not translated into html format

outFileTag = '_OUTFILE_NAME'
egFiles = ['ZERO_ONE_OUTFILE_NAME', 'DNA_OUTFILE_NAME', 'RNADNA_OUTFILE_NAME']
column8Files = ['ONE_N_OUTFILE_NAME', 'N_ONE_OUTFILE_NAME', 'N_M_OUTFILE_NAME', 'CHR_MIS_OUTFILE_NAME']
column5Files = ['ONE_ZERO_OUTFILE_NAME']
excludedFiles = ['EX_SEQ_OUTFILE_NAME']

# _ActualDB_key for URLs we need to use
egURL = 57
genbankURL = 12
refseqURL = 35

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

    db.useOneConnection(1)

    # initialize urls

    urls = {}
    results = db.sql('select _ActualDB_key, url from ACC_ActualDB ' + \
	'where _ActualDB_key in (%d,%d,%d)' % (egURL, genbankURL, refseqURL), 'auto')
    for r in results:
        key = r['_ActualDB_key']
        value = r['url']
        urls[key] = '<A HREF="%s">' % (value)

def exit():
    #
    # clean up, close files, etc.
    #

    db.useOneConnection(0)

def initFiles(value):
    #
    # open input and html files
    #

    # open input (text) file

    inFile = open(value, 'r')
    head, tail = os.path.split(value)

    # initialize output (html) file

    htmlFile = reportlib.init(tail, outputdir = os.environ['RPTDIR'], printHeading = None, isHTML = 1)

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

    return reportlib.create_accession_anchor(id) + id + anchorEnd

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

        i1 = string.find(s, '[')
        i2 = string.find(s, ']')
        tokens = string.split(s[:i2], '=')

	# clean up the results of the split

	tag = re.sub('{', '', tokens[0])
	ids = string.split(re.sub('\[', '', tokens[1]), ', ')

	# process tags

	# some MGI ids are '-'...don't create an anchor for these

	if tag == mgiTag:
	    for i in ids:
		if i != '-':
	            anchors[i] = mgiAnchor(i)

	elif tag == genbankTag:
	    for i in ids:
	        anchors[i] = externalAnchor(i, genbankURL)

	elif tag in refseqTag:
	    for i in ids:
	        anchors[i] = externalAnchor(i, refseqURL)

	# we're done if there are no more TAGs to process

	if len(s) < i2 + 3:
	    break

	# else, move to the next TAG

        s = s[i2+3:]

    # substitute the ids in the original string with the anchors

    for a in anchors.keys():
	neworiginal = re.sub(a, anchors[a], original)
	original = neworiginal

    return original

def processEG():
    #
    # process EG-oriented reports
    #

    # iterate thru Files
    
    for b in egFiles:

        value = os.environ[b]
	inFile, htmlFile = initFiles(value)

        # print html header

        htmlFile.write(tableStart + CRT)
        htmlFile.write('<TD>Entrez Gene</TD>')
        htmlFile.write('<TD>Symbol</TD>')
        htmlFile.write('<TD>Chromosome</TD>')
        htmlFile.write('<TD>Associated Sequences</TD>' + CRT)

        # iterate thru input file

        for line in inFile.readlines():

	    tokens = string.split(line, TAB)

	    egID = tokens[0]
	    egSymbol = tokens[1]
            egChromosome = tokens[2];
            sequences = tokens[3];

	    htmlFile.write('<TR><TD>' + externalAnchor(egID, egURL) + '</TD>')
	    htmlFile.write('<TD>' + egSymbol + '</TD>')
	    htmlFile.write('<TD>' + egChromosome + '</TD>')
	    htmlFile.write('<TD>' + idAnchors(sequences) + '</TD></TR>')
	    htmlFile.write(CRT)

        htmlFile.write(tableEnd)
        inFile.close()
        reportlib.finish_nonps(htmlFile, isHTML = 1)

def processMGI_8columns():
    #
    # process MGI-oriented reports with 8 columns
    #

    # iterate thru Files
    
    for b in column8Files:

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

	    tokens = string.split(line, TAB)

	    mgiID = tokens[0]
	    mgiSymbol = tokens[1]
            mgiChromosome = tokens[2]
            sequences = tokens[6]
	    markerType = tokens[7]

	    htmlFile.write('<TR><TD>' + mgiAnchor(mgiID) + '</TD>')
	    htmlFile.write('<TD>' + mgiSymbol + '</TD>')
	    htmlFile.write('<TD>' + mgiChromosome + '</TD>')
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

    # iterate thru Files
    
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

	    tokens = string.split(line, TAB)

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

def processExcluded():
    #
    # process excluded reports
    #

    # iterate thru Files
    
    for b in excludedFiles:

        value = os.environ[b]
	inFile, htmlFile = initFiles(value)

        # print html header

        htmlFile.write(tableStart + CRT)
        htmlFile.write('<TD>Accession</TD>')
        htmlFile.write('<TD>Type</TD>')
        htmlFile.write('<TD>MGI Markers</TD>')
        htmlFile.write('<TD>EntrezGene Genes</TD>' + CRT)

        # iterate thru input file

        for line in inFile.readlines():

	    tokens = string.split(line, TAB)

	    genbankID = tokens[0]
	    sequenceType = tokens[1]
            mgiMarkers = tokens[2]
            egMarkers = tokens[3]

	    mgihtml = []
	    if mgiMarkers != 'None':
		tokens = string.split(mgiMarkers,',')
		for t in tokens:
		    mgihtml.append(mgiAnchor(t))
	    else:
		mgihtml.append(mgiMarkers)

	    eghtml = []
	    if egMarkers != 'None':
		tokens = string.split(egMarkers,',')
		for t in tokens:
		    eghtml.append(externalAnchor(t, egURL))
	    else:
		eghtml.append(egMarkers)

	    htmlFile.write('<TR><TD>' + externalAnchor(genbankID, genbankURL) + '</TD>')
	    htmlFile.write('<TD>' + sequenceType + '</TD>')
	    htmlFile.write('<TD>' + string.join(mgihtml,',') + '</TD>')
	    htmlFile.write('<TD>' + string.join(eghtml,',') + '</TD></TR>')
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
#this report is not used at this time
#processExcluded()
exit()

