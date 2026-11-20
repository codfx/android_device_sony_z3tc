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

# inherit from the shinano-common
include device/sony/shinano-common/BoardConfigCommon.mk

DEVICE_PATH := device/sony/z3tc

# Assert
TARGET_OTA_ASSERT_DEVICE := z3tc,scorpion,SGP621,SGP641

#Audio
# Include path
TARGET_SPECIFIC_HEADER_PATH += $(DEVICE_PATH)/include

# Kernel properties
TARGET_KERNEL_CONFIG := lineageos_shinano_z3tc_defconfig

# Partition information
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 2671771648
# 12656259072 - 16384
BOARD_USERDATAIMAGE_PARTITION_SIZE := 12656242688

# Power
TARGET_TAP_TO_WAKE_NODE := "/sys/devices/virtual/input/clearpad/wakeup_gesture"

# fstab: the SD card slot is on msm_sdcc.3 here (msm_sdcc.2 on the other shinano devices)
TARGET_SHINANO_FSTAB := $(DEVICE_PATH)/rootdir/etc/fstab.qcom
TARGET_RECOVERY_FSTAB := $(DEVICE_PATH)/rootdir/etc/fstab.qcom

# SELinux
BOARD_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy
SYSTEM_EXT_PUBLIC_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy-system_ext/public
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy-system_ext/private

# Inherit from proprietary files
include vendor/sony/z3tc/BoardConfigVendor.mk
BOARD_KERNEL_SEPARATED_DTB := false

# Camera: Android 12 SensorManager::createEventQueue signature shim
TARGET_LD_SHIM_LIBS += \
    /system/lib/libcammw.so|/system/lib/libshim_sensor.so

# No JIT on 3.4 kernel (no memfd): fully AOT-compile at build time
WITH_DEXPREOPT := true
