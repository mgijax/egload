#!/bin/sh

#
# The purpose of this script is to run the paraccession python script
#


cd `dirname $0`

COMMON_CONFIG=../egload.config

#
# Make sure the common configuration file exists and source it.
#
if [ -f ${COMMON_CONFIG} ]
then
    . ${COMMON_CONFIG}
else
    echo "Missing configuration file: ${COMMON_CONFIG}"
    exit 1
fi

#
# Initialize the log file.
#
LOG=${LOG_PARACCESSION}
rm -rf ${LOG}
>>${LOG}

date >> ${LOG} 2>&1
${PYTHON} ${EGLOAD}/bin/paraccession.py >> ${LOG} 2>&1

date |tee -a $LOG

