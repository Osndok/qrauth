
#ERSION=$(shell cat .version)
VERSION=$(shell bin/version_or_snapshot.sh)
VERSION2=$(shell bin/version_or_snapshot.sh .)

MVN= TZ=UTC mvn -Drelease.version=$(VERSION)

SPEC=target/qrauth-native.spec

all: tools war

tools: target/qrauth-ssh-keys target/qrauth-pubkey2ssh target/qrauth-ed25519-verify

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

spec: $(SPEC)
	@echo $^

$(SPEC): .version src/qrauth-native.spec.in bin/spec-vars.sh bin/version_or_snapshot.sh
	bin/spec-vars.sh

ALWAYS:

tgz: target/qrauth-native-$(VERSION2).tgz
	@echo $^

target/qrauth-native-$(VERSION2).tgz: ALWAYS
	mkdir -p target
	#git archive --format=tar --prefix=qrauth-native-$(VERSION2)/ HEAD | gzip > target/qrauth-native-$(VERSION2).tgz
	tar --transform "s|^|qrauth-native-$(VERSION2)/|" --exclude .git -czf target/qrauth-native-$(VERSION2).tgz Makefile src ext

srpm: $(SPEC) target/qrauth-native-$(VERSION2).tgz
	#/usr/bin/mock --buildsrpm --spec "$(SPEC)" --sources "$(shell pwd)/target" --resultdir "$(shell pwd)/target"
	rpmbuild --nodeps --define '_srcrpmdir '"$(shell pwd)/target" --define '_sourcedir '"$(shell pwd)/target" -bs $(SPEC)

rpms: $(SPEC) target/qrauth-native-$(VERSION2).tgz
	#/usr/bin/mock $(SRPM) --resultdir "$(shell pwd)/target"
	rpmbuild -ba --nodeps --define '_rpmdir '"$(shell pwd)/target" --define '_srcrpmdir '"$(shell pwd)/target" --define '_sourcedir '"$(shell pwd)/target" -bs $(SPEC)

clean:
	rm -rf target java/target java/*/target

