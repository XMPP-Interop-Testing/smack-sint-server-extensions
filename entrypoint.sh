#!/bin/bash

DOMAIN="example.org"
HOST="127.0.0.1"
TIMEOUT=5000

usage() {
  cat <<EOF
Usage:
    --domain=DOMAIN                              XMPP domain name of server under test. (default: $DOMAIN)
    --host=HOST                                  IP address or DNS name of the XMPP service to run the tests on. (default: $HOST)
    --timeout=TIMEOUT                            Timeout in milliseconds for any XMPP action (default: $TIMEOUT)
    --adminAccountUsername=ADMINUSERNAME         Admin username for the service, to create test users
    --adminAccountPassword=ADMINPASSWORD         Admin password for the service, as above
    --accountOneUsername=ACCOUNTONEUSERNAME      The first account name of a set of three accounts used for testing.
    --accountOnePassword=ACCOUNTONEPASSWORD      The password of the accountOneUsername account.
    --accountTwoUsername=ACCOUNTTWOUSERNAME      The second account name of a set of three accounts used for testing.
    --accountTwoPassword=ACCOUNTTWOPASSWORD      The password of the accountTwoUsername account.
    --accountThreeUsername=ACCOUNTTHREEUSERNAME  The third account name of a set of three accounts used for testing.
    --accountThreePassword=ACCOUNTTHREEPASSWORD  The password of the accountThreeUsername account.
    --disabledTests=DISABLEDTESTS                Comma-separated list of tests to skip, e.g. EntityCapsTest,SoftwareInfoIntegrationTest
    --disabledSpecifications=DISABLEDSPECIFICATIONS
                                                 Comma-separated list of specifications to skip, e.g. XEP-0030,XEP-0199
    --enabledTests=ENABLEDTESTS                  Comma-separated list of the only tests to run, e.g. EntityCapsTest,SoftwareInfoIntegrationTest
    --enabledSpecifications=ENABLEDSPECIFICATIONS
                                                 Comma-separated list of the only specifications to run, e.g. XEP-0030,XEP-0199
    --help                                       This help message
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    --service*|--domain*)
      if [[ "$1" != *=* ]]; then shift; fi # Value is next arg if no `=`
      DOMAIN="${1#*=}"
      ;;
    --host*)
      if [[ "$1" != *=* ]]; then shift; fi
      HOST="${1#*=}"
      ;;
    --timeout*)
      if [[ "$1" != *=* ]]; then shift; fi
      TIMEOUT="${1#*=}"
      ;;
    --adminAccountUsername*)
      if [[ "$1" != *=* ]]; then shift; fi
      ADMINACCOUNTUSERNAME="${1#*=}"
      ;;
    --adminAccountPassword*)
      if [[ "$1" != *=* ]]; then shift; fi
      ADMINACCOUNTPASSWORD="${1#*=}"
      ;;
    --accountOneUsername*)
      if [[ "$1" != *=* ]]; then shift; fi
      ACCOUNTONEUSERNAME="${1#*=}"
      ;;
    --accountOnePassword*)
      if [[ "$1" != *=* ]]; then shift; fi
      ACCOUNTONEPASSWORD="${1#*=}"
      ;;
    --accountTwoUsername*)
      if [[ "$1" != *=* ]]; then shift; fi
      ACCOUNTTWOUSERNAME="${1#*=}"
      ;;
    --accountTwoPassword*)
      if [[ "$1" != *=* ]]; then shift; fi
      ACCOUNTTWOPASSWORD="${1#*=}"
      ;;
    --accountThreeUsername*)
      if [[ "$1" != *=* ]]; then shift; fi
      ACCOUNTTHREEUSERNAME="${1#*=}"
      ;;
    --accountThreePassword*)
      if [[ "$1" != *=* ]]; then shift; fi
      ACCOUNTTHREEPASSWORD="${1#*=}"
      ;;
    --disabledTests*)
      if [[ "$1" != *=* ]]; then shift; fi
      DISABLEDTESTS="${1#*=}"
      ;;
    --disabledSpecifications*)
      if [[ "$1" != *=* ]]; then shift; fi
      DISABLEDSPECIFICATIONS="${1#*=}"
      ;;
    --enabledTests*)
      if [[ "$1" != *=* ]]; then shift; fi
      ENABLEDTESTS="${1#*=}"
      ;;
    --enabledSpecifications*)
      if [[ "$1" != *=* ]]; then shift; fi
      ENABLEDSPECIFICATIONS="${1#*=}"
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
if [ ! -v DOMAIN ]; then echo "Domain is not set"; exit 1; fi
if [ ! -v ADMINACCOUNTUSERNAME ] && [ -v ADMINACCOUNTPASSWORD ]; then echo "Admin username is not set, but password is. Credentials must be specified as a pair"; exit 1; fi
if [ ! -v ADMINACCOUNTPASSWORD ] && [ -v ADMINACCOUNTUSERNAME ]; then echo "Admin password is not set, but username is. Credentials must be specified as a pair"; exit 1; fi
if [ ! -v $ACCOUNTONEUSERNAME ] && [ -v $ACCOUNTONEPASSWORD ]; then echo "Test account 'one' username is not set, but password is. Credentials must be specified as a pair"; exit 1; fi
if [ ! -v $ACCOUNTONEPASSWORD ] && [ -v $ACCOUNTONEUSERNAME ]; then echo "Test account 'one' password is not set, but username is. Credentials must be specified as a pair"; exit 1; fi
if [ ! -v $ACCOUNTTWOUSERNAME ] && [ -v $ACCOUNTTWOPASSWORD ]; then echo "Test account 'two' username is not set, but password is. Credentials must be specified as a pair"; exit 1; fi
if [ ! -v $ACCOUNTTWOPASSWORD ] && [ -v $ACCOUNTTWOUSERNAME ]; then echo "Test account 'two' password is not set, but username is. Credentials must be specified as a pair"; exit 1; fi
if [ ! -v $ACCOUNTTHREEUSERNAME ] && [ -v $ACCOUNTTHREEPASSWORD ]; then echo "Test account 'three' username is not set, but password is. Credentials must be specified as a pair"; exit 1; fi
if [ ! -v $ACCOUNTTHREEPASSWORD ] && [ -v $ACCOUNTTHREEUSERNAME ]; then echo "Test account 'three' password is not set, but username is. Credentials must be specified as a pair"; exit 1; fi

# Check if _all three_ accounts are provisioned (or none at all).
ACCOUNT_COUNT=0
if [[ -n "$ACCOUNTONEUSERNAME" ]]; then ((ACCOUNT_COUNT++)); fi
if [[ -n "$ACCOUNTTWOUSERNAME" ]]; then ((ACCOUNT_COUNT++)); fi
if [[ -n "$ACCOUNTTHREEUSERNAME" ]]; then ((ACCOUNT_COUNT++)); fi

if [[ $ACCOUNT_COUNT -ne 0 ]] && [[ $ACCOUNT_COUNT -ne 3 ]]; then
    echo "You must specify three test accounts, or none at all"
    exit 1
fi

JAVACMD=()
JAVACMD+=("java")
JAVACMD+=("-Dsinttest.service=$DOMAIN")
JAVACMD+=("-Dsinttest.host=$HOST")
JAVACMD+=("-Dsinttest.securityMode=disabled")
JAVACMD+=("-Dsinttest.replyTimeout=$TIMEOUT")
if [ "$ADMINACCOUNTUSERNAME" != "" ]; then
  JAVACMD+=("-Dsinttest.adminAccountUsername=$ADMINACCOUNTUSERNAME")
fi
if [ "$ADMINACCOUNTPASSWORD" != "" ]; then
  JAVACMD+=("-Dsinttest.adminAccountPassword=$ADMINACCOUNTPASSWORD")
fi
if [ "$ACCOUNTONEUSERNAME" != "" ]; then
  JAVACMD+=("-Dsinttest.accountOneUsername=$ACCOUNTONEUSERNAME")
fi
if [ "$ACCOUNTONEPASSWORD" != "" ]; then
  JAVACMD+=("-Dsinttest.accountOnePassword=$ACCOUNTONEPASSWORD")
fi
if [ "$ACCOUNTTWOUSERNAME" != "" ]; then
  JAVACMD+=("-Dsinttest.accountTwoUsername=$ACCOUNTTWOUSERNAME")
fi
if [ "$ACCOUNTTWOPASSWORD" != "" ]; then
  JAVACMD+=("-Dsinttest.accountTwoPassword=$ACCOUNTTWOPASSWORD")
fi
if [ "$ACCOUNTTHREEUSERNAME" != "" ]; then
  JAVACMD+=("-Dsinttest.accountThreeUsername=$ACCOUNTTHREEUSERNAME")
fi
if [ "$ACCOUNTTHREEPASSWORD" != "" ]; then
  JAVACMD+=("-Dsinttest.accountThreePassword=$ACCOUNTTHREEPASSWORD")
fi
JAVACMD+=("-Dsinttest.enabledConnections=tcp")
JAVACMD+=("-Dsinttest.dnsResolver=javax")
if [ "$DISABLEDSPECIFICATIONS" != "" ]; then
    JAVACMD+=("-Dsinttest.disabledSpecifications=$DISABLEDSPECIFICATIONS")
fi
if [ "$DISABLEDTESTS" != "" ]; then
    JAVACMD+=("-Dsinttest.disabledTests=$DISABLEDTESTS")
fi
if [ "$ENABLEDSPECIFICATIONS" != "" ]; then
    JAVACMD+=("-Dsinttest.enabledSpecifications=$ENABLEDSPECIFICATIONS")
fi
if [ "$ENABLEDTESTS" != "" ]; then
    JAVACMD+=("-Dsinttest.enabledTests=$ENABLEDTESTS")
fi
JAVACMD+=("-Dsinttest.testRunResultProcessors=org.igniterealtime.smack.inttest.util.StdOutTestRunResultProcessor,org.igniterealtime.smack.inttest.util.JUnitXmlTestRunResultProcessor")
# JAVACMD+=("-Dsinttest.debugger=standard,dir=./logs,console=off")
JAVACMD+=("-Dsinttest.debugger=org.igniterealtime.smack.inttest.util.ModifiedStandardSinttestDebuggerMetaFactory")
JAVACMD+=("-DlogDir=./logs")
JAVACMD+=("-jar")
JAVACMD+=("/usr/local/sintse/sintse.jar")

echo "Running: ${JAVACMD[@]}"

"${JAVACMD[@]}"
