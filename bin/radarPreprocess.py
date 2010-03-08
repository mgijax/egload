#!/usr/local/bin/python

#
# Program:  radarPreprocess.py
#
# Description:
#
#	selects a single NT or NW sequence for each geneID from
#       the genomic column of radar..DP_EntrezGene_Accession
#       where taxID = 10090 (mouse). Sets the genomic column for the
#       rejected NT or NW sequences to '-'
#
# Inputs:
#	queries radar..DP_EntrezGene_Accession
#
# Outputs:
#	Updates radar..DP_EntrezGene_Accession
#
# History
#
# 02/04/2010	sc
#	- TR9773, created
#

import sys 
import os
import re
import string
import db
import reportlib

#
# Constants
#
CRT = reportlib.CRT
TAB = reportlib.TAB

#
# Globals
#

# mapping of geneID to its list of NT and NW sequences
geneIdDict = {} # {geneId:[id1, ..., idn], ...}

# the radar database name
radar = os.environ['RADAR_DBNAME']
sqlFile = os.environ['UPDATE_FILE']
fd = ''

# list of assemblies
primaryAssembly = "Reference assembly (C57BL/6J)"
secondaryAssembly =  "Reference assembly"
assemblyList = [primaryAssembly, secondaryAssembly]

# sql update template
template = 'update DP_EntrezGene_Accession ' + \
	'set genomic = "-" ' + \
	'where geneId = "%s" ' + \
	'and genomic = "%s"%s'

class SequenceInfo:
    # Concept: Represents the sequence info needed to determine
    #          Which NT or NW contig we want to keep for each gene
    #   IS: container object for sequence info
    #   HAS: attributes representing sequence info
    #   DOES: provides methods to get the sequence info
    # Implementation:

    def __init__(self, \
	seqId, \
	prefix, \
	assembly):
	
	self.seqId = seqId
	self.prefix = prefix
	self.assembly = assembly

    def getSeqId(self):
	return self.seqId
   
    def getPrefix(self):
	return self.prefix
 
    def getAssembly(self):
	return self.assembly

def init():
    # Purpose: initialize database, dictionaries, file descriptor
    # Returns: nothing
    # Assumes: nothing
    # Effects: queries database, creates file in filesystem, 
    # Throws: nothing

    global geneIdDict, assemblyList, fd
    
    results = db.sql('select distinct geneID, genomic, assembly, substring(genomic, 1, 3) as prefix from %s..DP_EntrezGene_Accession where taxID = 10090 and (substring(genomic, 1, 3) = "NT_" or substring(genomic, 1, 3) = "NW_") order by geneID' % radar, 'auto')
    #print 'results length: %s' % len(results)   
    for r in results:
	seqId = string.strip(r['genomic'])
	assembly = string.strip(r['assembly'])
	geneId = string.strip(r['geneID'])
	prefix = string.strip(r['prefix'])
	sequenceInfo = SequenceInfo(seqId, prefix, assembly)
	if geneIdDict.has_key(geneId):
	    geneIdDict[geneId].append(sequenceInfo)
	else:
	    geneIdDict[geneId] = [sequenceInfo]
    #print 'geneIdDictLength: %s' % len(geneIdDict)
    geneIdList = geneIdDict.keys()
    geneIdList.sort()
    for g in geneIdList:
	seqs = geneIdDict[g]

    fd = open(sqlFile, 'w')

def writeUpdate(geneId, seqInfoList):
    # Purpose: writes update statement to sql file
    # Returns: nothing
    # Assumes: fd is valid file descriptor
    # Effects: nothing 
    # Throws: nothing

    for seqInfo in seqInfoList:
	seqId = seqInfo.getSeqId()
	cmd = template % (geneId, seqId, CRT)
	fd.write(cmd)
	fd.write("go%s" % CRT)

def parseRecords():
    # Purpose: iterates through dictionary representing lines from 
    #		the input file, determining the congtig we want to keep
    #           writing update statements for those we don't want to keep
    # Returns: nothing
    # Assumes: nothing
    # Effects: writes to the filesystem
    # Throws: nothing

    geneIdList = geneIdDict.keys()
    geneIdList.sort()
    for geneId in geneIdList:
	# attributes of the best sequence found so far
	currentPick = ''	    # SequenceInfo object
	currentBestPrefix = ''
	
	# get the set of  SequenceInfo objects for a geneId
	seqInfoList = geneIdDict[geneId]
	#print 'GeneID: %s Contigs: %s' % (geneId, len(seqInfoList))
	# Iterate through the SequenceInfo objects and determine
	# the one we want to keep
	for seqInfo in seqInfoList:
	    seqId = seqInfo.getSeqId()
	    prefix = seqInfo.getPrefix()
	    assembly = seqInfo.getAssembly()
	    if assembly != secondaryAssembly:
		if currentPick == '':
		    # This is the first seqInfo for geneId
		    currentPick = seqInfo
		    currentBestPrefix = prefix
		if currentBestPrefix == 'NT_' :
		    # any NT means we are done
		    break
		else: # currentBestPrefix is 'NW_'
		    if prefix == 'NT_':
			# NT_ trumps NW_
			currentPick = seqInfo
			currentBestPrefix = prefix
			break
	#print 'list size before: %s' % len(seqInfoList)
	# In this case no contig picked because all on secondaryAssembly
        if currentPick != '':
	    seqInfoList.remove(currentPick)
	    #print 'list size after: %s' % len(seqInfoList)
	    #print 'Picking: %s %s %s' % (geneId, currentPick.getSeqId(), currentPick.getAssembly())
        writeUpdate(geneId, seqInfoList)
    fd.close()
#
# main
#

init()
parseRecords()
sys.exit(0)
