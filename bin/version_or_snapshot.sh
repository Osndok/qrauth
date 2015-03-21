#!/bin/bash

if ./release.sh --build-needed > /dev/null ; then
	echo $(cat .version)${1:--}snapshot
else
	exec cat .version
fi

