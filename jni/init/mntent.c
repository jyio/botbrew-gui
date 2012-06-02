/* from musl <http://www.etalabs.net/musl/> */

#include <stdio.h>
#include <string.h>
#include <mntent.h>
#include <errno.h>

struct mntent *getmntent_r(FILE *f, struct mntent *mnt, char *linebuf, int buflen)
{
	int cnt, n[8];

	mnt->mnt_freq = 0;
	mnt->mnt_passno = 0;

	do {
		fgets(linebuf, buflen, f);
		if (feof(f) || ferror(f)) return 0;
		if (!strchr(linebuf, '\n')) {
			fscanf(f, "%*[^\n]%*[\n]");
			errno = ERANGE;
			return 0;
		}
		cnt = sscanf(linebuf, " %n%*s%n %n%*s%n %n%*s%n %n%*s%n %d %d",
			n, n+1, n+2, n+3, n+4, n+5, n+6, n+7,
			&mnt->mnt_freq, &mnt->mnt_passno);
	} while (cnt < 2 || linebuf[n[0]] == '#');

	linebuf[n[1]] = 0;
	linebuf[n[3]] = 0;
	linebuf[n[5]] = 0;
	linebuf[n[7]] = 0;

	mnt->mnt_fsname = linebuf+n[0];
	mnt->mnt_dir = linebuf+n[2];
	mnt->mnt_type = linebuf+n[4];
	mnt->mnt_opts = linebuf+n[6];

	return mnt;
}

struct mntent *getmntent(FILE *f)
{
	static char linebuf[256];
	static struct mntent mnt;
	return getmntent_r(f, &mnt, linebuf, sizeof linebuf);
}
