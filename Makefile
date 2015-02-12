
TOOLS=qrauth-ssh-keys

target/qrauth-ssh-keys: src/qrauth-ssh-keys.c
	rm -fv $@
	gcc -Wall -Werror -Wfatal-errors $^ -lgit2 -o $@

test: target/qrauth-ssh-keys
	DEBUG=1 target/qrauth-ssh-keys $(shell whoami)

prereqs:
	sudo yum install libgit2-devel json-c-devel

