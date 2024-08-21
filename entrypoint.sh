#!/bin/bash

SERVICE="example.org"
HOST="127.0.0.1"
TIMEOUT=5000

usage() {
  cat <<EOF
Usage:
    --service=SERVICE                   Service name, e.g. example.org (default: $SERVICE)
    --host=HOST                         IP address or DNS name of the XMPP service to run the tests on. (default: $HOST)
    --timeout=TIMEOUT                   Timeout in milliseconds for any XMPP action (default: $TIMEOUT)
    --adminUsername=ADMINUSERNAME       Admin username for the service, to create test users (if not using IBR / XEP-0077)
    --adminPassword=ADMINPASSWORD       Admin password for the service, as above
    --disabledTests=DISABLEDTESTS       Comma-separated list of tests to skip, e.g. EntityCapsTest,SoftwareInfoIntegrationTest
    --disabledSpecifications=DISABLEDSPECIFICATIONS
                                        Comma-separated list of specifications to skip, e.g. XEP-0030,XEP-0199
    --help                              This help message
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    --service*|--domain*)
      if [[ "$1" != *=* ]]; then shift; fi # Value is next arg if no `=`
      SERVICE="${1#*=}"
      ;;
    --host*)
      if [[ "$1" != *=* ]]; then shift; fi
      HOST="${1#*=}"
      ;;
    --timeout*)
      if [[ "$1" != *=* ]]; then shift; fi
      TIMEOUT="${1#*=}"
      ;;
    --adminUsername*)
      if [[ "$1" != *=* ]]; then shift; fi
      ADMINUSERNAME="${1#*=}"
      ;;
    --adminPassword*)
      if [[ "$1" != *=* ]]; then shift; fi
      ADMINPASSWORD="${1#*=}"
      ;;
    --disabledTests*)
      if [[ "$1" != *=* ]]; then shift; fi
      DISABLEDTESTS="${1#*=}"
      ;;
    --disabledSpecifications*)
      if [[ "$1" != *=* ]]; then shift; fi
      DISABLEDSPECIFICATIONS="${1#*=}"
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      >&2 printf "Error: Invalid argument %s\n" "$1"
      exit 1
      ;;
  esac
  shift
done

# Deal with errors
if [ ! -v SERVICE ]; then echo "Service is not set"; exit 1; fi
if [ ! -v ADMINUSERNAME ] && [ -v ADMINPASSWORD ]; then echo "Admin username is not set, but password is. Credentials must be specified as a pair"; exit 1; fi
if [ ! -v ADMINPASSWORD ] && [ -v ADMINUSERNAME ]; then echo "Admin password is not set, but username is. Credentials must be specified as a pair"; exit 1; fi

# If host is local, replace with host.docker.internal
#if [ "$HOST" == "127.0.0.1" ] || [ "$HOST" == "localhost" ]; then
#  HOST="host.docker.internal"
#fi

java \
  -Dsinttest.service="$SERVICE" \
  -Dsinttest.host="$HOST" \
  -Dsinttest.securityMode=disabled \
  -Dsinttest.replyTimeout=$TIMEOUT \
  -Dsinttest.adminAccountUsername="$ADMINUSERNAME" \
  -Dsinttest.adminAccountPassword="$ADMINPASSWORD" \
  -Dsinttest.enabledConnections=tcp \
  -Dsinttest.dnsResolver=javax \
  -Dsinttest.disabledSpecifications="$DISABLEDSPECIFICATIONS" \
  -Dsinttest.disabledTests="$DISABLEDTESTS" \
  -Dsinttest.testRunResultProcessors=org.igniterealtime.smack.inttest.util.StdOutTestRunResultProcessor,org.igniterealtime.smack.inttest.util.JUnitXmlTestRunResultProcessor \
  -Dsinttest.debugger="org.igniterealtime.smack.inttest.util.FileLoggerFactory" \
  -DlogDir="logs" \
  -jar /usr/local/sintse/sintse.jar
