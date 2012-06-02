# libjackpal-androidterm4

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# This is the target being built.
LOCAL_MODULE:= libjackpal-androidterm4

# All of the source files that we will compile.
LOCAL_SRC_FILES:= \
  common.cpp \
  termExec.cpp \
  fileCompat.cpp

LOCAL_LDLIBS := -ldl -llog

include $(BUILD_SHARED_LIBRARY)

# init

include $(CLEAR_VARS)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/init
LOCAL_MODULE:= init
LOCAL_SRC_FILES:= \
  init/init.c \
  init/strnstr.c \
  init/mntent.c
LOCAL_LDLIBS :=
include $(BUILD_EXECUTABLE)
