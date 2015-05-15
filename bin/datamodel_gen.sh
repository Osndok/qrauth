#!/bin/bash
#
# For your convenience, if any hibernate-related changes are made to the entities classes; this script
# can be run to generate the new datamodel and remind you of the following commands to build a migration
# record.
#

MESSAGE="$*"

SQL_OUT=java/qrauth-server/target/sql/hibernate3/structure.sql
MIGRATIONS=java/qrauth-server/src/main/resources/db/migration

VERSION=$(bin/version_or_snapshot.sh)
MVN="mvn -Drelease.version=${VERSION}"

export TZ=UTC

set -vexu

unset CDPATH

( cd java ; $MVN install );
( cd java/qrauth-server ; $MVN -f pom.hibernate.xml -X hibernate3:hbm2ddl );

LAST=$(cd ${MIGRATIONS} ; ls *.ddl | tail -n 1 )

NUM=$(echo $LAST | cut -c2-4 | sed 's/^0*//')

let NUM=$NUM+1

NUM=$(printf "%03d" $NUM)

set +vx
echo
echo
echo
echo "To continue, you will probably need to:"
echo "---------------------------------------------"
if [ -z "$MESSAGE" ];
then

echo 'MESSAGE="my message goes here"'
echo "MSG=\$(echo -n \$MESSAGE | tr -s '[:space:]' '_')"
echo cp $SQL_OUT ${MIGRATIONS}/V${NUM}__'${MSG}'.ddl
echo diff -wub '${MIGRATIONS}/{'$LAST',V'$NUM'__${MSG}.ddl}'
echo vi '${MIGRATIONS}/V'$NUM'__${MSG}.sql'

echo
echo "NB: if you provided a message from the command line, I would have done the first two steps for you..."

else

MSG=$(echo -n $MESSAGE | tr -s '[:space:]' '_')
echo cp $SQL_OUT ${MIGRATIONS}/V${NUM}__${MSG}.ddl
echo diff -wub ${MIGRATIONS}/'{'$LAST',V'$NUM'__'${MSG}'.ddl}'
echo vi ${MIGRATIONS}/V$NUM'__'${MSG}'.sql'

fi

echo

