LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := Changelog
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := APPS
LOCAL_PRIVILIGED_MODULE := true

ifeq ($(COMPILE_PREBUILT_APKS),true)
changelog_apk := $(LOCAL_PATH)/app/build/outputs/apk/release/app-release-unsigned.apk

$(changelog_apk):
	@echo "Building Changelog"
	$(shell) packages/apps/Changelog/build.sh

LOCAL_PREBUILT_MODULE_FILE := $(changelog_apk)
else
LOCAL_PREBUILT_MODULE_FILE := $(LOCAL_PATH)/prebuilt/app-debug.apk
endif
LOCAL_CERTIFICATE := platform

include $(BUILD_PREBUILT)
