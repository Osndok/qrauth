
/**
 * 
 * A small bit of glue code to hand the math-intensive ed25519 verification off to some much-more performant
 * code. Due largely to the java being... java? or simply the adaptive BigIntegers used, this utility will
 * verify a signature in about 2 milliseconds that takes the pure java implementation 2000 milliseconds.
 * Nonetheless, the the pure-java implementation is kept as a fallback.
 *
 * Example compile command:
 * gcc ed25519.o -lcrypto -Wall -o qrauth-ed25519-verify{,.c}
 * 
 */

#include <stdio.h>

/*
 * See:
 * https://github.com/floodyberry/ed25519-donna
 */
#include "ed25519.h"

#define MAX_MESSAGE_SIZE (4096*10)

int main (int argc, char *argv[])
{
	ed25519_public_key publicKey;
	ed25519_signature signature;
	size_t size=0;
	unsigned char message[MAX_MESSAGE_SIZE];

	int i;

	for(i=0; i<sizeof(ed25519_public_key); i++)
	{
		publicKey[i]=(unsigned char)getc(stdin);
	}

	for(i=0; i<sizeof(ed25519_signature); i++)
	{
		signature[i]=(unsigned char)getc(stdin);
	}

	int c=getc(stdin);

	while (c!=EOF)
	{
		message[size]=(unsigned char)c;

		++size;

		if (size >= MAX_MESSAGE_SIZE)
		{
			fprintf(stderr, "message is too big\n");
			return 2;
		}

		//fprintf(stderr, "message size is %zu bytes, read: 0x%2x\n", size, c);

		c=getc(stdin);
	}

	if (size<=0)
	{
		fprintf(stderr, "no message received (input too short?)\n");
		return 3;
	}

	fprintf(stderr, "message size is %zu bytes\n", size);

	if (ed25519_sign_open(message, size, publicKey, signature))
	{
		fprintf(stderr, "verification failed\n");
		return 1;
	}
	else
	{
		fprintf(stderr, "verification successful\n");
		return 0;
	}
}
