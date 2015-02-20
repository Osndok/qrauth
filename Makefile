
TOOLS=qrauth-ssh-keys

#ERSION=$(shell cat .version)
VERSION=$(shell bin/version_or_snapshot.sh)

MVN= TZ=UTC mvn -Drelease.version=$(VERSION)

tools: target/qrauth-ssh-keys target/qrauth-pubkey2ssh war

target/qrauth-ssh-keys: src/qrauth-ssh-keys.c
	rm -fv $@
	mkdir -p target
	gcc -Wall -Werror -Wfatal-errors $^ -lgit2 -o $@

target/qrauth-pubkey2ssh: src/qrauth-pubkey2ssh.c
	rm -fv $@
	mkdir -p target
	gcc -Wall -Werror -Wfatal-errors $^ -lcrypto -o $@

test: target/qrauth-ssh-keys
	DEBUG=1 target/qrauth-ssh-keys $(shell whoami)
	( cd java ; $(MVN) test )

prereqs:
	sudo yum install libgit2-devel json-c-devel

# NB: "java" is a directory, thus a bad make target..
war:
	( cd java ; $(MVN) clean package )

run:
	( cd java ; $(MVN) -pl qrauth-common install )
	# TODO: even with the above, sometimes this launches service with wrong git hash
	( cd java ; $(MVN) -pl qrauth-server jetty:run )

sql:
	( cd java ; $(MVN) -pl qrauth-common install )
	( cd java ; $(MVN) -pl qrauth-server clean compile -X hibernate3:hbm2ddl )

clean:
	rm -rf target java/target java/*/target

