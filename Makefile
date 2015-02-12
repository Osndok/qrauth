
TOOLS=qrauth-ssh-keys

#TODO: append '-snapshot' if not sitting on a release commit (or not in a git directory)
VERSION=$(shell cat .version)

target/qrauth-ssh-keys: src/qrauth-ssh-keys.c
	rm -fv $@
	gcc -Wall -Werror -Wfatal-errors $^ -lgit2 -o $@

test: target/qrauth-ssh-keys
	DEBUG=1 target/qrauth-ssh-keys $(shell whoami)

prereqs:
	sudo yum install libgit2-devel json-c-devel

# NB: "java" is a directory, thus a bad make target..
war:
	( cd java ; TZ=UTC mvn -Drelease.version=$(VERSION) clean package )

run:
	( cd java ; TZ=UTC mvn -Drelease.version=$(VERSION) -pl qrauth-common install )
	# TODO: even with the above, sometimes this launches service with wrong git hash
	( cd java ; TZ=UTC mvn -Drelease.version=$(VERSION) -pl qrauth-server jetty:run )

