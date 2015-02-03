
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <git2.h>

struct opts
{
	const char *username;
};

int DEBUG=0;

static
void parse_opts(struct opts *o, int argc, char *argv[]);

static
void check_lg2(int error, const char *message, const char *extra);

static
git_tree *sub_tree(git_repository *repo, const git_tree *tree, const char *entry_name);

static
void list_public_keys(git_repository *repo, const git_tree *tree);

static
void print_public_key(git_repository *repo, const git_oid *oid);

/**
 * Given a username, lists any public keys embedded in the privy qrauth git repo.
 */
int main(int argc, char *argv[])
{
	const
	char *gitdir="/var/qrauth/privy.git";

	const
	char *rev="refs/heads/master";

	struct opts opts = { "username" };

	//???: git_libgit2_init();

	parse_opts(&opts, argc, argv);

	git_repository *repo;

	check_lg2(git_repository_open_ext(&repo, gitdir, 0, NULL), "Could not open repository", NULL);

	git_tree *root;
	{
		git_object *obj = NULL;

		check_lg2(git_revparse_single(&obj, repo, rev), "Could not resolve", rev);

		git_otype obj_type=git_object_type(obj);

		if (obj_type!=GIT_OBJ_COMMIT)
		{
			fprintf(stderr, "%s|%s points to a %s, expecting a commit\n",gitdir,rev,git_object_type2string(obj_type));
			exit(1);
		}

		if (DEBUG)
		{
			char oidstr[GIT_OID_HEXSZ + 1];
			git_oid_tostr(oidstr, sizeof(oidstr), git_object_id(obj));
			fprintf(stderr, "# %s %s\n", oidstr, rev);
		}

		git_commit *commit=(git_commit*)obj;

		git_commit_tree(&root, commit);

		if (DEBUG)
		{
			const git_oid *oid = git_commit_tree_id(commit);
			char oidstr[GIT_OID_HEXSZ + 1];
			git_oid_tostr(oidstr, sizeof(oidstr), oid);
			fprintf(stderr, "# %s root-tree\n", oidstr);
		}

		//TODO: docs are unclear asto if freeing the commit will also destroy the tree?
		git_object_free(obj);
	}

	git_tree *users=sub_tree(repo, root, "users");

	if (users==NULL)
	{
		fprintf(stderr, "# Unable to find users tree entry\n");
		exit(1);
	}
	else
	if (DEBUG)
	{
		const git_oid *oid = git_tree_id(users);
		char oidstr[GIT_OID_HEXSZ + 1];
		git_oid_tostr(oidstr, sizeof(oidstr), oid);
		fprintf(stderr, "# %s users\n", oidstr);
	}

	git_tree *this_user=sub_tree(repo, users, opts.username);

	if (this_user==NULL)
	{
		fprintf(stderr, "# Unable to find '%s' user information\n", opts.username);
		exit(1);
	}
	else
	if (DEBUG)
	{
		const git_oid *oid = git_tree_id(this_user);
		char oidstr[GIT_OID_HEXSZ + 1];
		git_oid_tostr(oidstr, sizeof(oidstr), oid);
		fprintf(stderr, "# %s %s\n", oidstr, opts.username);
	}
	
	list_public_keys(repo, this_user);

	/*
	char *prefix;
	{
		const
		char *template="users/%s/pubkey-";

		const
		int limit=strlen(template)+strlen(opts.username);

		prefix=(char*)malloc(limit);

		sprintf(prefix, template, opts.username);

		if (DEBUG)
		{
			fprintf(stderr, "# prefix %s\n", prefix);
		}
	}

	scan_tree(tree, prefix);

	free(prefix); prefix=NULL;
	*/

	git_tree_free(root);
	git_tree_free(users);
	git_tree_free(this_user);

	git_repository_free(repo);

	//???: git_libgit2_shutdown();
	//???: git_threads_shutdown();
	return 0;
}

static
git_tree *sub_tree(git_repository *repo, const git_tree *tree, const char *desired_entry_name)
{
	size_t i, max_i = (int)git_tree_entrycount(tree);
	const git_tree_entry *te;
	
	for (i = 0; i < max_i; ++i)
	{
		te = git_tree_entry_byindex(tree, i);

		const
		char *this_entry_name=git_tree_entry_name(te);

		if (strcmp(this_entry_name, desired_entry_name)==0)
		{
			const
			git_oid *oid=git_tree_entry_id(te);

			git_tree *tree;

			git_tree_lookup(&tree, repo, oid);

			return tree;
		}
	}

	return NULL;
}

static
void list_public_keys(git_repository *repo, const git_tree *tree)
{
	const
	char *prefix="pubkey-";

	const
	int l=strlen(prefix);

	size_t i, max_i = (int)git_tree_entrycount(tree);

	const git_tree_entry *te;
	
	for (i = 0; i < max_i; ++i)
	{
		te = git_tree_entry_byindex(tree, i);

		const
		char *entry_name=git_tree_entry_name(te);

		if (strncmp(prefix, entry_name, l)==0)
		{
			const
			git_oid *oid=git_tree_entry_id(te);

			if (DEBUG)
			{
				char oidstr[GIT_OID_HEXSZ + 1];
				git_oid_tostr(oidstr, sizeof(oidstr), oid);
				
				printf("\n# %s %s\n", oidstr, entry_name);
			}

			print_public_key(repo, oid);
		}
	}
}

static
void print_public_key(git_repository *repo, const git_oid *oid)
{
	git_blob *blob;

	git_blob_lookup(&blob, repo, oid);

	/* ? Does this need crlf filtering? */
	fwrite(git_blob_rawcontent(blob), (size_t)git_blob_rawsize(blob), 1, stdout);
	fputc('\n', stdout);

	git_blob_free(blob);
}

static
void parse_opts(struct opts *o, int argc, char *argv[])
{
	if (argc!=2)
	{
		fprintf(stderr, "usage: %s <username>\n", argv[0]);
		exit(1);
	}

	o->username=argv[1];

	if (getenv("DEBUG"))
	{
		DEBUG=1;
	}
}

static
void check_lg2(int error, const char *message, const char *extra)
{
	const git_error *lg2err;
	const char *lg2msg = "", *lg2spacer = "";
	
	if (!error)
		return;
	
	if ((lg2err = giterr_last()) != NULL && lg2err->message != NULL) {
		lg2msg = lg2err->message;
		lg2spacer = " - ";
	}
	
	if (extra)
		fprintf(stderr, "%s '%s' [%d]%s%s\n",
				message, extra, error, lg2spacer, lg2msg);
		else
			fprintf(stderr, "%s [%d]%s%s\n",
					message, error, lg2spacer, lg2msg);
			
			exit(1);
}


