# Copyright (C) 2026 codfx
# SPDX-License-Identifier: Apache-2.0
#
# Leave AOSP packages out of the build that base_system.mk adds (there is no PRODUCT_PACKAGES -=): this
# uninstallable placeholder overrides them, so they are not installed.
# CtsShimPrebuilt / CtsShimPrivPrebuilt: CTS placeholders presigned with the public AOSP testkey (the priv one is a
# privileged app), so anyone could sign an "update" for them; CTS is not run on this ROM.
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := Z3tcRemovePackages
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_TAGS := optional
LOCAL_OVERRIDES_PACKAGES := CtsShimPrebuilt CtsShimPrivPrebuilt
LOCAL_UNINSTALLABLE_MODULE := true
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_SRC_FILES := /dev/null
include $(BUILD_PREBUILT)
