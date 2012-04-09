#!/bin/sh
#
#  formatreports.sh
###########################################################################
#
#  Purpose:  This script formats the reports generated by the egload.
#
#  Usage:
#
#      formatreports.sh
#
#  Outputs:
#
#  Exit Codes:
#
#      0:  Successful completion
#      1:  Fatal error occurred
#      2:  Non-fatal error occurred
#
#  Assumes:  Nothing
#
#  Notes:  None
#
###########################################################################

cd `dirname $0`/..

#
#
#  Verify the argument(s) to the shell script.
#
if [ $# -ne 0 ]
then
    echo "Usage: $0" | tee -a ${LOG_PROC}
    exit 1
fi

#
#  Verify and source the configuration file name.
#
CONFIG=`pwd`/egload.config
if [ ! -r ${CONFIG} ]
then
    echo "Cannot read configuration file: ${CONFIG}" | tee -a ${LOG_PROC}
    exit 1
fi
. ${CONFIG}

#
#  Source the common DLA functions script.
#
if [ "${DLAJOBSTREAMFUNC}" != "" ]
then
    if [ -r ${DLAJOBSTREAMFUNC} ]
    then
        . ${DLAJOBSTREAMFUNC}
    else
        echo "Cannot source DLA functions script: ${DLAJOBSTREAMFUNC}" | tee -a ${LOG_PROC}
        exit 1
    fi
else
    echo "Environment variable DLAJOBSTREAMFUNC has not been defined." | tee -a ${LOG_PROC}
    exit 1
fi

cd ${RPTDIR}

# sort text files
#
# note that "sort" sorts on blank space (including tabs)
# the AbstractCollection "toString" method (used by HashSet, used by Bucketizer) 
# uses ", " formatting.
# This extra space causes problems in sorting these files.
# If you need to sort on more than one column in the file, you'll have to sed the
# files first to replace ", " with ",".
#

if [ -f ${ONE_ONE_OUTFILE_NAME} ]
then
    sort ${ONE_ONE_SORT} ${ONE_ONE_OUTFILE_NAME} > ${ONE_ONE_OUTFILE_NAME}.tmp
    mv ${ONE_ONE_OUTFILE_NAME}.tmp ${ONE_ONE_OUTFILE_NAME}
else
   echo "Cannot open report file: ${ONE_ONE_OUTFILE_NAME}" >> ${LOG_PROC}
fi

if [ -f ${ONE_N_OUTFILE_NAME} ]
then
    sort ${ONE_N_SORT} ${ONE_N_OUTFILE_NAME} > ${ONE_N_OUTFILE_NAME}.tmp
    mv ${ONE_N_OUTFILE_NAME}.tmp ${ONE_N_OUTFILE_NAME}
else
   echo "Cannot open report file: ${ONE_N_OUTFILE_NAME}" >> ${LOG_PROC}
fi

if [ -f ${ONE_ZERO_OUTFILE_NAME} ]
then
    sort ${ONE_ZERO_SORT} ${ONE_ZERO_OUTFILE_NAME} > ${ONE_ZERO_OUTFILE_NAME}.tmp
    mv ${ONE_ZERO_OUTFILE_NAME}.tmp ${ONE_ZERO_OUTFILE_NAME}
else
   echo "Cannot open report file: ${ONE_ZERO_OUTFILE_NAME}" >> ${LOG_PROC}
fi

if [ -f ${ZERO_ONE_OUTFILE_NAME} ]
then
    sort ${ZERO_ONE_SORT} ${ZERO_ONE_OUTFILE_NAME} > ${ZERO_ONE_OUTFILE_NAME}.tmp
    mv ${ZERO_ONE_OUTFILE_NAME}.tmp ${ZERO_ONE_OUTFILE_NAME}
else
   echo "Cannot open report file: ${ZERO_ONE_OUTFILE_NAME}" >> ${LOG_PROC}
fi

if [ -f ${CHR_MIS_OUTFILE_NAME} ]
then
    sort ${CHR_MIS_SORT} ${CHR_MIS_OUTFILE_NAME} > ${CHR_MIS_OUTFILE_NAME}.tmp
    mv ${CHR_MIS_OUTFILE_NAME}.tmp ${CHR_MIS_OUTFILE_NAME}
else
   echo "Cannot open report file: ${CHR_MIS_OUTFILE_NAME}" >> ${LOG_PROC}
fi

# convert text files to html

#${EGLOAD}/bin/formatreports.py >> ${LOG_PROC}
${EGLOAD}/bin/formatreports.py
STAT=$?
if [ ${STAT} -ne 0 ]
then
    echo "EntrezGene Load report formatting failed.    Return status: ${STAT}" >> ${LOG_PROC}
    exit 1
fi

# for each text file, write the number of lines
for i in ${ONE_ONE_OUTFILE_NAME} ${ONE_N_OUTFILE_NAME} ${ONE_ZERO_OUTFILE_NAME} ${ZERO_ONE_OUTFILE_NAME} ${CHR_MIS_OUTFILE_NAME} ${GM_NOTIN_OUTFILE_NAME}
do
echo `wc -l $i` > $i.tmp
echo "\n" >> $i.tmp
cat $i >> $i.tmp
mv $i.tmp $i
done

echo "Entrez Gene Load report formatting completed successfully." >> ${LOG_PROC}

exit 0

