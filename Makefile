
TOOLS=qrauth-ssh-keys

bin/qrauth-ssh-keys: src/qrauth-ssh-keys.c
	rm -fv $@
	gcc -Wall -Werror -Wfatal-errors $^ -lgit2 -o $@

test: bin/qrauth-ssh-keys
	DEBUG=1 bin/qrauth-ssh-keys $(shell whoami)

prereqs:
	sudo yum install libgit2-devel json-c-devel

