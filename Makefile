
#ERSION=$(shell cat .version)
VERSION=$(shell bin/version_or_snapshot.sh)

MVN= TZ=UTC mvn -Drelease.version=$(VERSION)

tools: target/qrauth-ssh-keys target/qrauth-pubkey2ssh target/qrauth-ed25519-verify war

target/qrauth-ssh-keys: src/qrauth-ssh-keys.c
	rm -fv $@
	mkdir -p target
	gcc -Wall -Werror -Wfatal-errors $^ -lgit2 -o $@

target/qrauth-pubkey2ssh: src/qrauth-pubkey2ssh.c
	rm -fv $@
	mkdir -p target
	gcc -Wall -Werror -Wfatal-errors $^ -lcrypto -o $@

target/qrauth-ed25519-verify: src/qrauth-ed25519-verify.c target/ed25519.o
	rm -fv $@
	gcc target/ed25519.o -lcrypto -Wall -o $@ src/qrauth-ed25519-verify.c -Iext/ed25519-donna

target/ed25519.o: ext/ed25519-donna/*.c ext/ed25519-donna/*.h
	mkdir -p target
	cd ext/ed25519-donna ; gcc ed25519.c -m64 -O3 -c -Wall -DED25519_SSE2 -o "../../$@"

test: target/qrauth-ssh-keys target/qrauth-ed25519-verify
	DEBUG=1 target/qrauth-ssh-keys $(shell whoami)
	DEBUG=1 target/qrauth-ed25519-verify < test/ed25519-verify-test.data
	( cd java ; $(MVN) test )

prereqs:
	sudo yum install libgit2-devel json-c-devel libcrypto-devel
	git submodule init

# NB: "java" is a directory, thus a bad make target..
war:
	( cd java ; $(MVN) clean )
	( cd java ; $(MVN) -pl qrauth-common install )
	( cd java ; $(MVN) -pl qrauth-server package )

run:
	( cd java ; $(MVN) -pl qrauth-common install )
	# TODO: even with the above, sometimes this launches service with wrong git hash
	( cd java ; $(MVN) -pl qrauth-server jetty:run )

sql:
	( cd java ; $(MVN) compile )
	( cd java/qrauth-server ; $(MVN) -f pom.hibernate.xml -X hibernate3:hbm2ddl )

clean:
	rm -rf target java/target java/*/target

