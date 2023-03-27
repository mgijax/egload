#!/bin/sh

#
# The purpose of this script is to run the paraccession python script
#

cd `dirname $0`/..

#
#  Verify and source the configuration file name.
#
CONFIG=`pwd`/egload.config
if [ ! -r ${CONFIG} ]
then
    echo "Cannot read configuration file: ${CONFIG}"
    exit 1
fi
. ${CONFIG}

LOG=${LOG_PARACCESSION}
rm -rf ${LOG}
>>${LOG}

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

${PYTHON} ${EGLOAD}/bin/paraccession.py >> ${LOG} 2>&1
STAT=$?
if [ ${STAT} -ne 0 ]
then
    echo "EntrezGene Load paraccession failed. Return status: ${STAT}" >> ${LOG}
    exit 1
fi

echo "Entrez Gene Load paracession completed successfully." >> ${LOG}

exit 0

