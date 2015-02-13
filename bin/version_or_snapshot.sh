#!/bin/bash

if ./release.sh --build-needed > /dev/null ; then
	echo $(cat .version)-snapshot
else
	exec cat .version
fi

