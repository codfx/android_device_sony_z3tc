#
# Copyright (C) 2019 The LineageOS Project
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

# Dalvik
PRODUCT_PROPERTY_OVERRIDES += \
    dalvik.vm.heapstartsize=16m \
    dalvik.vm.heapgrowthlimit=192m \
    dalvik.vm.heapsize=512m \
    dalvik.vm.heaptargetutilization=0.75 \
    dalvik.vm.heapminfree=2m \
    dalvik.vm.heapmaxfree=8m

# Semc
PRODUCT_PROPERTY_OVERRIDES += \
    ro.semc.version.sw=23.5.A.1.291 \
    ro.semc.version.cust=1289-4696 \
    ro.semc.version.cust_revision=23.5.A.1.291 \
    ro.semc.product.model=SGP621 \
    ro.semc.ms_type_id=PM-0810-BV

# LCD: 320 dpi = 600 dp smallest width, so the launcher and SystemUI use the tablet layout
# (12L taskbar, dock at the bottom in landscape). At 340 it was 565 dp (phone layout).
PRODUCT_PROPERTY_OVERRIDES += \
    ro.sf.lcd_density=320

# Settings: single pane by default. The 12L two-pane layout (activity embedding) works in landscape, but each page
# switch starts a new activity in the right pane: ~640 ms vs ~510 ms single pane on this SoC. Users can turn it
# back on with: setprop persist.sys.fflag.override.settings_support_large_screen true
PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.fflag.override.settings_support_large_screen=false

# USB path
PRODUCT_PROPERTY_OVERRIDES += \
    ro.usb.pid_suffix=1BB

# Bluetooth: allow 8 s (instead of 2.9 s) for the controller start-up incl. the firmware patch download
# over the Broadcom transport, so a slow enable does not abort the Bluetooth stack (normally ~1.1 s).
PRODUCT_PROPERTY_OVERRIDES += \
    bluetooth.enable_timeout_ms=8000
