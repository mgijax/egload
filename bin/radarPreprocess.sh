#!/bin/sh
#
#  radarPreprocess.sh
###########################################################################
#
#  Purpose:  This script controls the execution of radar preprocessing tasks
#
#  Usage:
#
#      radarPreprocess.sh
#
#  Env Vars:
#
#      See the configuration file
#
#  Inputs:
#
#      - Common configuration file (common.config.sh)
#      - EntrezGene load configuration file (config)
#
#  Outputs:
#
#      - An archive file
#      - Log files defined by the environment variables ${LOG_PROC},
#        ${LOG_DIAG}, ${LOG_CUR} and ${LOG_VAL}
#      - BCP files for each database table to be loaded
#      - Records written to the database tables
#      - Exceptions written to standard error
#      - Configuration and initialization errors are written to a log file
#        for the shell script
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

#
#  Set up a log file for the shell script in case there is an error
#  during configuration and initialization.
#
cd `dirname $0`/..
LOG=`pwd`/preprocess.log
rm -f ${LOG}

#
#  Verify the argument(s) to the shell script.
#
if [ $# -ne 0 ]
then
    echo "Usage: $0" | tee -a ${LOG}
    exit 1
fi

#
#  Verify and source the configuration file name.
#
CONFIG=egload.config
if [ ! -r ${CONFIG} ]
then
    echo "Cannot read configuration file: ${CONFIG}" | tee -a ${LOG}
    exit 1
fi
. ${CONFIG}

#
# Set and verify the master configuration file name
#
CONFIG_MASTER=${MGICONFIG}/master.config.sh
if [ ! -r ${CONFIG_MASTER} ]
then
    echo "Cannot read configuration file: ${CONFIG_MASTER}" | tee -a ${LOG}
    exit 1
fi

#
#  Run the preprocessor
#
echo "" 
echo "`date`" 
${EGLOAD}/bin/radarPreprocess.py
STAT=$?
if [ ${STAT} -ne 0 ]
then
    echo "Preprocessor failed.  Return status: ${STAT}"
    exit 1
fi

#
# run the updates
#
server=${RADAR_DBSERVER}
db=${RADAR_DBNAME}
pwFile=${RADAR_DBPASSWORDFILE}
user=${RADAR_DBUSER}
inFile=${UPDATE_FILE}
outFile=${inFile}.log

if [ ! -s ${inFile} ]
then
    echo "The update file is empty" 
else
    cat ${pwFile} | isql -U ${user} -S ${server} -D ${db} -i ${inFile} -o  ${outFile} -e
fi
exit 0

