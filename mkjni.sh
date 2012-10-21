( cd jni && ~/android-ndk-r8b/ndk-build )
cd libs
for arch in armeabi mips x86; do
	mv "${arch}/init" "${arch}/libinit.so"
done
