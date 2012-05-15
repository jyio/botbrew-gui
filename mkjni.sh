( cd jni && ~/android-ndk-r8/ndk-build )
cd libs
for item in armeabi mips x86; do
	mv "${item}/init" "${item}/libinit.so"
done
