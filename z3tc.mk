#
# Copyright (C) 2013-2016 The CyanogenMod Project
#           (C) 2017-2019 The LineageOS Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Inherit shinano-common definitions
$(call inherit-product, device/sony/shinano-common/shinano.mk)

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/audio/mixer_paths.xml:$(TARGET_COPY_OUT_VENDOR)/etc/mixer_paths.xml \
    $(LOCAL_PATH)/configs/audio/mixer_paths_auxpcm.xml:$(TARGET_COPY_OUT_VENDOR)/etc/mixer_paths_auxpcm.xml

# Boot animation
TARGET_SCREEN_HEIGHT := 1280
TARGET_SCREEN_WIDTH := 720

# Hardware-specific permissions
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.sensor.barometer.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.sensor.barometer.xml \
    frameworks/native/data/etc/android.hardware.camera.autofocus.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.autofocus.xml \
    frameworks/native/data/etc/android.hardware.sensor.proximity.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.sensor.proximity.xml \
    frameworks/native/data/etc/android.hardware.telephony.gsm.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.telephony.gsm.xml \
    frameworks/native/data/etc/handheld_core_hardware.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/handheld_core_hardware.xml

# Overlays
DEVICE_PACKAGE_OVERLAYS += \
    $(LOCAL_PATH)/overlay \
    $(LOCAL_PATH)/overlay-lineage

# Wi-Fi config (Android 12 reads it only from RROs targeting com.android.wifi.resources)
PRODUCT_PACKAGES += \
    WifiOverlayZ3tc

# force sw keys
PRODUCT_PROPERTY_OVERRIDES += \
    qemu.hw.mainkeys=0

# Properties
include $(LOCAL_PATH)/system_prop.mk

# Recovery
PRODUCT_PACKAGES += \
    logo.rle

# Sensors

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/sensor_def_qcomdev.conf:system/etc/sensor_def_qcomdev.conf

# Screen density
PRODUCT_AAPT_CONFIG := normal
PRODUCT_AAPT_PREF_CONFIG := xhdpi

# Thermal manager
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/thermanager.xml:$(TARGET_COPY_OUT_VENDOR)/etc/thermanager.xml

# BRING-UP ONLY: adb without authorization from early boot (helps debug bootloops).
# The ifneq below is commented out, so this applies to ALL builds. Remove for a release.
#ifneq (,$(filter eng, $(TARGET_BUILD_VARIANT)))
  PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.usb.config=mtp,adb
#endif

# Call the proprietary setup
$(call inherit-product, vendor/sony/z3tc/z3tc-vendor.mk)
PRODUCT_ADB_KEYS := device/sony/z3tc/adb_keys

# BRING-UP DEBUG: keep booting after crashes and save logcat to /data/misc/logd
PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.disable_rescue=true \
    persist.logd.logpersistd=logcatd \
    persist.logd.logpersistd.buffer=all

# No JIT (no memfd on 3.4): compile everything ahead-of-time with "speed"
PRODUCT_DEX_PREOPT_DEFAULT_COMPILER_FILTER := speed
PRODUCT_SYSTEM_SERVER_COMPILER_FILTER := speed
PRODUCT_PROPERTY_OVERRIDES += \
    pm.dexopt.first-boot=speed \
    pm.dexopt.boot-after-ota=speed \
    pm.dexopt.install=speed \
    pm.dexopt.install-bulk=speed \
    pm.dexopt.bg-dexopt=speed

# BRING-UP DEBUG: early boot/perf logger (remove for release)
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/debug/z3tc_perfmon.sh:$(TARGET_COPY_OUT_VENDOR)/bin/z3tc_perfmon.sh \
    $(LOCAL_PATH)/debug/z3tc_perfmon.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/z3tc_perfmon.rc

# BRING-UP DEBUG: standby soak logger, started by init at boot (persist.vendor.z3tc.soak), PC-independent
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/debug/z3tc_soak.sh:$(TARGET_COPY_OUT_VENDOR)/bin/z3tc_soak.sh \
    $(LOCAL_PATH)/debug/z3tc_soak.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/z3tc_soak.rc

# Workaround: untrusted APEX system-server code in dalvik-cache (no fs-verity on 3.4)
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/init.z3tc.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.z3tc.rc

# Charge limiter (off by default; persist.sys.z3tc.charge_*, set by the Z3tcChargeLimit app:
# Quick Settings tile and Settings > Battery > Charge limit)
PRODUCT_PACKAGES += \
    Z3tcChargeLimit

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/charge/z3tc_charge_limit.sh:$(TARGET_COPY_OUT_VENDOR)/bin/z3tc_charge_limit.sh \
    $(LOCAL_PATH)/charge/init.z3tc.charge.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.z3tc.charge.rc

# BRING-UP DEBUG: adb over network on port 5555 (home network only! remove for release)
PRODUCT_PROPERTY_OVERRIDES += \
    service.adb.tcp.port=5555
