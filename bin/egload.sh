#!/bin/sh
#
#  egload.sh
###########################################################################
#
#  Purpose:  This script controls the execution of the entrezgene load.
#
#  Usage:
#
#      egload.sh
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
LOG=`pwd`/egload.log
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
CONFIG=`pwd`/egload.config
if [ ! -r ${CONFIG} ]
then
    echo "Cannot read configuration file: ${CONFIG}" | tee -a ${LOG}
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
        echo "Cannot source DLA functions script: ${DLAJOBSTREAMFUNC}" | tee -a ${LOG}
        exit 1
    fi
else
    echo "Environment variable DLAJOBSTREAMFUNC has not been defined." | tee -a ${LOG}
    exit 1
fi

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
#  Perform pre-load tasks.
#
preload ${OUTPUTDIR}
cleanDir ${OUTPUTDIR}

#
# run Radar Preprocessor
#
echo "" >> ${LOG_PROC}
echo "`date`" >> ${LOG_PROC}
echo "Run the Radar Preprocess application" >> ${LOG_PROC} 
${EGLOAD}/bin/radarPreprocess.sh 
STAT=$?
checkStatus ${STAT} "radarPreprocess.sh"

#
#  Run the load application.
#
echo "" >> ${LOG_PROC}
echo "`date`" >> ${LOG_PROC}
echo "Run the EntrezGene Load application" >> ${LOG_PROC}
${JAVA} ${JAVARUNTIMEOPTS} -classpath ${CLASSPATH} \
        -DCONFIG=${CONFIG_MASTER},${CONFIG} \
        -DJOBKEY=${JOBKEY} ${DLA_START}
STAT=$?
checkStatus ${STAT} ${EGLOAD}

#
# post format reports
#
echo "" >> ${LOG_PROC}
echo "`date`" >> ${LOG_PROC}
echo "Run the EntrezGene Load output formatting" >> ${LOG_PROC}
${EGLOAD}/bin/formatreports.sh
STAT=$?
checkStatus ${STAT} "formatreports.sh"

#
# run qc reports
#
${APP_QCRPT} ${RPTDIR} ${RADAR_DBSERVER} ${RADAR_DBNAME} ${JOBKEY}
STAT=$?
checkStatus ${STAT} ${APP_QCRPT}

echo "EntrezGene Load application completed successfully" >> ${LOG_PROC}

postload

exit 0
