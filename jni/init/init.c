#include <libgen.h>
#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>
#include <alloca.h>
#include <malloc.h>
#include <unistd.h>
#include <sched.h>
#include <errno.h>
#include <sys/stat.h>
#include <sys/mount.h>
#include <sys/types.h>
#include <sys/syscall.h>
#include <sys/prctl.h>
#include <sys/wait.h>

#include "strnstr.h"

#define ENV_PATH	"/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games:/botbrew/bin:/usr/lib/busybox"

struct config {
	char *target;
	char *cwd;
	char *const *argv;
};

struct mountspec {
	const char *src;
	const char *dst;
	const char *type;
	unsigned long flags;
	const void *data;
	unsigned long remount_flags;
};

static struct mountspec foreign_mounts[] = {
	{NULL,"/proc","proc",0,NULL,0},
	{"/dev","/dev",NULL,MS_BIND,NULL,0},
	{"/dev/pts","/dev/pts",NULL,MS_BIND,NULL,0},
	{NULL,"/sys","sysfs",0,NULL,0},
	{NULL,"/android","tmpfs",MS_NODEV|MS_NOEXEC|MS_NOATIME,"size=1M,mode=0755",0},
	{"/cache","/android/cache",NULL,MS_BIND,NULL,0},
	{"/data","/android/data",NULL,MS_BIND,NULL,0},
	{"/datadata","/android/datadata",NULL,MS_BIND,NULL,0},
	{"/emmc","/android/emmc",NULL,MS_BIND,NULL,0},
	{"/sd-ext","/android/sd-ext",NULL,MS_BIND,NULL,0},
	{"/sdcard","/android/sdcard",NULL,MS_BIND,NULL,0},
	{"/system","/android/system",NULL,MS_BIND,NULL,MS_REMOUNT|MS_NODEV|MS_NOATIME},
	{"/usbdisk","/android/usbdisk",NULL,MS_BIND,NULL,0},
	{"/system/xbin","/android/system/xbin",NULL,MS_BIND,NULL,MS_REMOUNT|MS_NODEV|MS_NOATIME}
};
static int n_foreign_mounts = sizeof(foreign_mounts)/sizeof(foreign_mounts[0]);

static void usage(char *progname) {
	fprintf(stderr,
		"Usage: %s [options] [--] [<command>...]\n"
		"\n"
		"Available options:\n"
		"\t-d <directory>\t| --dir=<directory>\tSpecify chroot directory\n"
		"\t-r\t\t| --remount\t\tRemount chroot directory\n"
		"\t-u\t\t| --unmount\t\tUnmount chroot directory and exit\n",
	progname);
	exit(EXIT_FAILURE);
}

static void privdrop(void) {
	gid_t gid = getgid();
	uid_t uid = getuid();
	setgroups(1,&gid);
#ifdef linux
	setregid(gid,gid);
	setreuid(uid,uid);
#else
	setegid(gid);
	setgid(gid);
	seteuid(uid);
	setuid(uid);
#endif
}

static pid_t child_pid = 0;
static void sighandler(int signo) {
	if(child_pid != 0) kill(child_pid,signo);
}

static char *strconcat(const char *a, const char *b) {
	size_t len_a = strlen(a);
	size_t len_b = strlen(b);
	char *res = (char*)malloc(len_a+len_b+1);
	memcpy(res,a,len_a);
	memcpy(res+len_a,b,len_b+1);	// includes null terminator
	return res;
}

static void mount_setup(char *target, int selfmount) {
	if(selfmount) mount(target,target,NULL,MS_BIND,NULL);
	mount(target,target,NULL,MS_REMOUNT|MS_NODEV|MS_NOATIME,NULL);
	char *fs = strconcat(target,"/etc");
	char *fs_dst = strconcat(target,"/botbrew/etc");
	mkdir(fs_dst,0755);
	mount(fs,fs_dst,NULL,MS_BIND,NULL);
	free(fs_dst);
	free(fs);
	fs = strconcat(target,"/home");
	fs_dst = strconcat(target,"/botbrew/home");
	mkdir(fs_dst,0755);
	mount(fs,fs_dst,NULL,MS_BIND,NULL);
	free(fs_dst);
	free(fs);
	fs = strconcat(target,"/root");
	fs_dst = strconcat(target,"/botbrew/root");
	mkdir(fs_dst,0755);
	mount(fs,fs_dst,NULL,MS_BIND,NULL);
	free(fs_dst);
	free(fs);
	fs = strconcat(target,"/var");
	fs_dst = strconcat(target,"/botbrew/var");
	mkdir(fs_dst,0755);
	mount(fs,fs_dst,NULL,MS_BIND,NULL);
	free(fs_dst);
	free(fs);
	fs = strconcat(target,"/usr/share");
	fs_dst = strconcat(target,"/botbrew/share");
	mkdir(fs_dst,0755);
	mount(fs,fs_dst,NULL,MS_BIND,NULL);
	free(fs_dst);
	free(fs);
	fs = strconcat(target,"/run");
	mount(NULL,fs,"tmpfs",0,"size=10%,mode=0755");
	fs_dst = strconcat(target,"/botbrew/run");
	mkdir(fs_dst,0755);
	mount(fs,fs_dst,NULL,MS_BIND,NULL);
	free(fs_dst);
	free(fs);
	fs = strconcat(target,"/run/tmp");
	mkdir(fs,01777);
	free(fs);
	fs = strconcat(target,"/run/lock");
	mkdir(fs,01777);
	free(fs);
}

static void mount_teardown(char *target, int selfmount) {
	char *fs = strconcat(target,"/botbrew/etc");
	umount2(fs,MNT_DETACH);
	free(fs);
	fs = strconcat(target,"/botbrew/home");
	umount2(fs,MNT_DETACH);
	free(fs);
	fs = strconcat(target,"/botbrew/root");
	umount2(fs,MNT_DETACH);
	free(fs);
	fs = strconcat(target,"/botbrew/var");
	umount2(fs,MNT_DETACH);
	free(fs);
	fs = strconcat(target,"/botbrew/share");
	umount2(fs,MNT_DETACH);
	free(fs);
	fs = strconcat(target,"/botbrew/run");
	umount2(fs,MNT_DETACH);
	free(fs);
	fs = strconcat(target,"/run");
	umount2(fs,MNT_DETACH);
	free(fs);
	if(selfmount) umount2(target,MNT_DETACH);
}

static int main_clone(struct config *config) {
	char cwd[PATH_MAX];
	if(getcwd(cwd,sizeof(cwd)) == NULL) {
		fprintf(stderr,"whoops: cannot get working directory\n");
		return EXIT_FAILURE;
	}
	if(chdir(config->target)) {
		fprintf(stderr,"whoops: cannot chdir to namespace\n");
		return EXIT_FAILURE;
	}
	size_t target_len = strlen(config->target);
	int i;
	struct stat st;
	for(i = 0; i < n_foreign_mounts; i++) {
		struct mountspec m = foreign_mounts[i];
		if((m.src)&&(stat(m.src,&st) != 0)) continue;
		size_t dst_len = strlen(m.dst);
		char *dst = (char*)malloc(target_len+dst_len+1);
		memcpy(dst,config->target,target_len);
		memcpy(dst+target_len,m.dst,dst_len+1);	// includes null terminator
		mkdir(dst,0755);
		if(mount(m.src,dst,m.type,m.flags,m.data)) rmdir(dst);
		else if(m.remount_flags) mount(dst,dst,NULL,m.remount_flags|MS_REMOUNT,NULL);
		free(dst);
	}
	if(chroot(".")) {
		fprintf(stderr,"whoops: cannot chroot\n");
		return EXIT_FAILURE;
	}
	if((chdir(cwd))&&(chdir("/"))) {
		fprintf(stderr,"whoops: cannot chdir to chroot\n");
		return EXIT_FAILURE;
	}
	privdrop();
	char *env_path = getenv("PATH");
	if((env_path)&&(env_path[0])) {
		size_t env_path_len = strlen(env_path);
		char *newpath = (char*)malloc(sizeof(ENV_PATH)+env_path_len);
		char *append = strstr(env_path,"::");
		if(append) {	// break string at :: and insert
			append++;
			size_t prepend_len = append-env_path;
			memcpy(newpath,env_path,prepend_len);
			memcpy(newpath+prepend_len,ENV_PATH,sizeof(ENV_PATH)-1);
			memcpy(newpath+prepend_len+sizeof(ENV_PATH)-1,append,env_path_len-prepend_len+1);
		} else {
			memcpy(newpath,ENV_PATH":",sizeof(ENV_PATH":")-1);
			memcpy(newpath+sizeof(ENV_PATH":")-1,env_path,env_path_len+1);	// includes null terminator
		}
		setenv("PATH",newpath,1);
		free(newpath);
	} else setenv("PATH",ENV_PATH":/android/sbin:/system/sbin:/system/bin:/system/xbin",1);
	unsetenv("LD_LIBRARY_PATH");
	setenv("BOTBREW_PREFIX",config->target,1);
	if(config->argv == NULL) {
		const char *argv0[2];
		argv0[0] = "/init.sh";
		argv0[1] = 0;
		config->argv = (char**)&argv0;
	}
	if(execvp(config->argv[0],config->argv)) {
		int i = 1;
		fprintf(stderr,"whoops: cannot run `%s",config->argv[0]);
		while(config->argv[i]) fprintf(stderr," %s",config->argv[i++]);
		fprintf(stderr,"'\n");
		return errno;
	}
	return 0;	// wtf
}

int main(int argc, char *argv[]) {
	struct config config;
	struct stat st;
	char apath[PATH_MAX];
	int remount = 0;
	int unmount = 0;
	// get absolute path
	config.target = realpath(dirname(argv[0]),apath);
	uid_t uid = getuid();
	int c;
	while(1) {
		static struct option long_options[] = {
			{"dir",required_argument,0,'d'},
			{"remount",no_argument,0,'r'},
			{"unmount",no_argument,0,'u'},
			{0,0,0,0}
		};
		int option_index = 0;
		c = getopt_long(argc,argv,"d:ru",long_options,&option_index);
		if(c == -1) break;
		switch(c) {
			case 'd':
				// prevent privilege escalation: only superuser can chroot to arbitrary directories
				if(uid) {
					fprintf(stderr,"whoops: --dir is only available for uid=0\n");
					return EXIT_FAILURE;
				}
				config.target = realpath(optarg,apath);
				break;
			case 'r':
				remount = 1;
			case 'u':
				unmount = 1;
				break;
			default:
				usage(argv[0]);
		}
	}
	config.argv = (optind==argc)?NULL:(argv+optind);
	// prevent privilege escalation: fail if link/symlink is not owned by superuser
	if(uid) {
		if(lstat(argv[0],&st)) {
			fprintf(stderr,"whoops: cannot stat `%s'\n",argv[0]);
			return EXIT_FAILURE;
		}
		if(st.st_uid) {
			fprintf(stderr,"whoops: `%s' is not owned by uid=0\n",argv[0]);
			return EXIT_FAILURE;
		}
	}
	// check if directory exists
	if((stat(config.target,&st))||(!S_ISDIR(st.st_mode))) {
		fprintf(stderr,"whoops: `%s' is not a directory\n",config.target);
		return EXIT_FAILURE;
	}
	if((st.st_uid)||(st.st_gid)) chown(config.target,0,0);
	if((st.st_mode&S_IWGRP)||(st.st_mode&S_IWOTH)) chmod(config.target,0755);
	// check if directory mounted
	FILE *fp;
	char *haystack;
	size_t len;
	size_t target_len = strlen(config.target);
	char *needle = (char*)malloc(target_len+3);
	needle[0] = needle[target_len+1] = ' ';
	memcpy(needle+1,config.target,target_len+1);	// includes null terminator
	int mounted = 0;
	int loopmounted = 0;
	if(fp = fopen("/proc/self/mounts","r")) while(haystack = fgetln(fp,&len)) if(strnstr(haystack,needle,len)) {
		if(strncmp(haystack,"/dev/block/loop",sizeof("/dev/block/loop")-1) == 0) loopmounted = 1;
		else if(strncmp(haystack,"/dev/loop",sizeof("/dev/loop")-1) == 0) loopmounted = 1;
		fclose(fp);
		if(loopmounted) {
			char *needle2 = (char*)malloc(snprintf(NULL,0," %s/run ",config.target)+1);
			sprintf(needle2," %s/run ",config.target);
			if(fp = fopen("/proc/self/mounts","r")) while(haystack = fgetln(fp,&len)) if(strnstr(haystack,needle2,len)) {
				mounted = 1;
				break;
			}
			fclose(fp);
			free(needle2);
		} else mounted = 1;
		break;
	}
	free(needle);
	// check if directory needs to be unmounted
	if(unmount) {
		mount_teardown(config.target,!loopmounted);
		mounted = 0;
		if(!remount) return EXIT_SUCCESS;
	}
	if(!mounted) {
		if(geteuid()) {
			fprintf(stderr,"whoops: superuser privileges required for first invocation of `%s'\n",argv[0]);
			return EXIT_FAILURE;
		}
		mount_setup(config.target,!loopmounted);
		if(!stat(argv[0],&st)) {
			// setuid
			if((st.st_uid)||(st.st_gid)) chown(argv[0],0,0);
			if((st.st_mode&S_IWGRP)||(st.st_mode&S_IWOTH)||!(st.st_mode&S_ISUID)) chmod(argv[0],04755);
		}
	}
	// clone with new namespace
	long stacksz = sysconf(_SC_PAGESIZE);
	void *stack = (char*)alloca(stacksz)+stacksz;
#ifdef __i386__
	pid_t pid = syscall(__NR_clone,SIGCHLD|CLONE_NEWNS|CLONE_FILES,stack);
	if(pid == 0) return main_clone(&config);
#else
	pid_t pid = clone((int (*)(void*))main_clone,stack,SIGCHLD|CLONE_NEWNS|CLONE_FILES,(void*)&config);
#endif
	if(pid < 0) {
		fprintf(stderr,"whoops: cannot clone\n");
		return EXIT_FAILURE;
	} else {
		char *pr_name = (char*)malloc(snprintf(NULL,0,"BotBrew(%d)",pid)+1);
		sprintf(pr_name,"BotBrew(%d)",pid);
		prctl(PR_SET_NAME,pr_name);
		free(pr_name);
		struct sigaction act;
		int ret;
		privdrop();
		memset(&act,0,sizeof(act));
		act.sa_handler = sighandler;
		child_pid = pid;
		sigaction(SIGABRT,&act,0);
		sigaction(SIGALRM,&act,0);
		sigaction(SIGHUP,&act,0);
		sigaction(SIGINT,&act,0);
		sigaction(SIGQUIT,&act,0);
		sigaction(SIGTERM,&act,0);
		sigaction(SIGUSR1,&act,0);
		sigaction(SIGUSR2,&act,0);
		sigaction(SIGCONT,&act,0);
		sigaction(SIGSTOP,&act,0);
		sigaction(SIGTSTP,&act,0);
		sigaction(SIGPOLL,&act,0);
		sigaction(SIGPROF,&act,0);
		sigaction(SIGURG,&act,0);
		sigaction(SIGVTALRM,&act,0);
		sigaction(SIGXCPU,&act,0);
		while((waitpid(pid,&ret,0)<0)&&(errno == EINTR));
		child_pid = 0;
		return WIFEXITED(ret)?WEXITSTATUS(ret):EXIT_FAILURE;
	}
}
