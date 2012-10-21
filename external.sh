#!/bin/sh

set -x

fetch_git() {
	if [ -e "$2" ]; then
		cd "$2"
		git pull
		cd -
	else
		git clone "$1" "$2"
	fi
}

installpackage() {
	cd "$1"
	rm -f .apklib
	zip -r9 .apklib *
	VERSION=`cat AndroidManifest.xml | grep versionName | sed -e 's/.*="//' -e 's/".*//'`
	mvn install:install-file -Dfile=".apklib" -DgroupId="$2" -DartifactId="$3" -Dversion="${VERSION}" -Dpackaging=apklib
	cd -
}

mkdir -p external
cd external

fetch_git https://github.com/jackpal/Android-Terminal-Emulator.git jackpal-android-terminal-emulator
installpackage jackpal-android-terminal-emulator/libraries/emulatorview jackpal.androidterm emulatorview
