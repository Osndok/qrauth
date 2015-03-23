#!/bin/bash
#
# Creates the SPEC and TARBALL required for the SPEC.
#
#

set -eu

PROJECT=qrauth-native

# ----------------------------------------------

SPEC_IN=src/$PROJECT.spec.in
SPEC_OUT=target/$PROJECT.spec

test -d target || mkdir target

# ----------------------------------------------

VERSION=$(cat .version)
echo "VERSION=$VERSION (.version)"

# http://semver.org/
MAJOR=$(echo $VERSION | cut -f1 -d.)
MINOR=$(echo $VERSION | cut -f2 -d.)
PATCH=$(echo $VERSION | cut -f3 -d.)
BUILD=${BUILD_NUMBER:-0}

echo "MAJOR=$MAJOR"
echo "MINOR=$MINOR"
echo "PATCH=$PATCH"
echo "BUILD=$BUILD"

REPLACEMENTS="-e s/@MAJOR@/$MAJOR/g -e s/@MINOR@/$MINOR/g -e s/@PATCH@/$PATCH/g -e s/@BUILD@/$BUILD/g"

if ./release.sh --build-needed > /dev/null ; then

	REPLACEMENTS="$REPLACEMENTS -e s/@MUTEX@/snapshot/g -e s/@-SNAPSHOT@/-snapshot/g -e s/@_SNAPSHOT@/_snapshot/g -e s/@.SNAPSHOT@/.snapshot/g"

	SELF_SOURCE_DIR="$PROJECT-$VERSION.snapshot/"
	SELF_SOURCE_REFERENCE="$PROJECT-$VERSION.snapshot.tar.gz"

else

	REPLACEMENTS="$REPLACEMENTS -e s/@MUTEX@/v${MAJOR}/g -e s/@-SNAPSHOT@//g -e s/@_SNAPSHOT@//g -e s/@.SNAPSHOT@//g"

	SELF_SOURCE_DIR="$PROJECT-$VERSION/"
	SELF_SOURCE_REFERENCE="$PROJECT-$VERSION.tar.gz"

fi

sed $REPLACEMENTS "$SPEC_IN" > "$SPEC_OUT"

echo "SSD=$SELF_SOURCE_DIR (root of git-archive)"
echo "SSR=$SELF_SOURCE_REFERENCE (git-archive filename)"


#if which spectool ; then
#	cat $SPEC_OUT | spectool - | tr -s ' :' ':' | cut -f2- -d: > $SOURCES_LIST
#else
#	cat $SPEC_OUT | ssh "$BUILD_MACHINE" spectool - | tr -s ' :' ':' | cut -f2- -d: > $SOURCES_LIST
#fi

