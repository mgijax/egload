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
    #
    # initialize database, dictionaries, etc.
    #

    global geneIdDict, assemblyList, fd
    
#    results = db.sql('select geneID, genomic, assembly, substring(genomic, 1, 3) as prefix ' + \
#	'from %s..DP_EntrezGene_Accession ' + \
#	'where taxID = 10090 ' + \
#	'and (assembly = "%s" ' + \
#	'or assembly = "%s") ' + \
#	'order by geneID' % (radar, primaryAssembly, secondaryAssembly), 'auto')
    results = db.sql('select distinct geneID, genomic, assembly, substring(genomic, 1, 3) as prefix from %s..DP_EntrezGene_Accession where taxID = 10090 and (assembly = "%s" or assembly = "%s") and (substring(genomic, 1, 3) = "NT_" or substring(genomic, 1, 3) = "NW_") order by geneID' % (radar, primaryAssembly, secondaryAssembly), 'auto')
    print 'results length: %s' % len(results)   
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
    print 'geneIdDictLength: %s' % len(geneIdDict)
    geneIdList = geneIdDict.keys()
    geneIdList.sort()
    #for g in geneIdList:
	#seqs = geneIdDict[g]
	#for s in seqs:
	    #print '%s%s%s%s%s%s' % (g, TAB, s.getSeqId(), TAB, s.getAssembly(), CRT)

    fd = open(sqlFile, 'w')

def writeUpdate(geneId, seqInfoList):
	for seqInfo in seqInfoList:
	    seqId = seqInfo.getSeqId()
	    cmd = template % (geneId, seqId, CRT)
	    fd.write(cmd)
	    fd.write("go%s" % CRT)

def parseRecords():
    geneIdList = geneIdDict.keys()
    geneIdList.sort()
    for geneId in geneIdList:
	# attributes of the best sequence found so far
	currentPick = ''	    # SequenceInfo object
	currentBestSeqId = ''       
	currentBestPrefix = '' 	
	currentBestAssembly = ''
	
	# get the set of  SequenceInfo objects for a geneId
	seqInfoList = geneIdDict[geneId]
	#if len(seqInfoList) == 0:
		#print 'geneID: %s ' % geneId
                #print "No contigs"
	#elif len(seqInfoList) > 1:
	    #print 'geneID: %s ' % geneId
	    #for s in seqInfoList:
	#	print  'seqId: %s, assembly: %s' %  (s.getSeqId(), s.getAssembly())
	# Iterate through the SequenceInfo objects and determine
	# the one we want to keep
	hasPrimary = 0
	hasSecondary = 0
	hasBoth = 0
	for seqInfo in seqInfoList:
	    seqId = seqInfo.getSeqId()
	    prefix = seqInfo.getPrefix()
	    assembly = seqInfo.getAssembly()
 	    if currentPick == '':
		# This is the first seqInfo for geneId
		currentPick = seqInfo
		currentBestSeqId = seqId
		currentBestPrefix = prefix
		currentBestAssembly = assembly
	    if currentBestPrefix == 'NT_':
		if currentBestAssembly == primaryAssembly:
		    # This is the best case an NT_ on the primary assembly
		    # take the first we come to 
		    #break
		    hasPrimary = 1 
		else: # currentBestAssembly is secondaryAssembly 
		    if assembly == primaryAssembly:
			# primary trumps secondary
			currentPick = seqInfo
			currentBestSeqId = seqId
			currentBestPrefix = prefix
			currentBestAssembly = assembly
			hasSecondary = 1
	    else: # currentBestPrefix is 'NW_'
		if prefix == 'NT_':
		    # NT_ trumps NW_
		    currentPick = seqInfo
                    currentBestSeqId = seqId
                    currentBestPrefix = prefix
                    currentBestAssembly = assembly
		else: # incoming prefix is 'NW_'
		    if currentBestAssembly == primaryAssembly:
			# primary trumps secondary
			# take first one we come to
			continue
		    else: # currentBestAssembly is secondary assembly
			if assembly == primaryAssembly:
			    # primary trumps secondary
			    currentPick = seqInfo
			    currentBestSeqId = seqId
			    currentBestPrefix = prefix
			    currentBestAssembly = assembly
	#print 'list size before: %s' % len(seqInfoList)
	seqInfoList.remove(currentPick)
	#print 'list size after: %s' % len(seqInfoList)
	if hasPrimary == 1 and hasSecondary == 1:
	    hasBoth = 1
	#print 'Picking: %s %s %s %s' % (geneId, currentPick.getSeqId(), currentPick.getAssembly(), hasBoth)
        writeUpdate(geneId, seqInfoList)
#
# main
#

init()
parseRecords()
sys.exit(0)

