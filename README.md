Device configuration for Sony Xperia Z3 Tablet Compact (LineageOS 19.1)
=======================================================================

Codename: scorpion / z3tc (SGP621, SGP641 - LTE)

Status: in testing, not released. Boots and runs daily; see the project site
for what works and the known issues.

Project site (status, known issues, changelog, technical notes):
https://codfx.github.io/z3tc-lineage/

Based on:
- sony_scorpion/android_device_sony_z3tc (lineage-18.1, GitLab) for all
  tablet-specific files (audio, sensors, overlays, props, kernel config name)
- marcogiannetta-pi/android_device_sony_z3c (lineage-19.1) for the
  19.1-specific changes (extract utils scripts, OTA asserts,
  BOARD_KERNEL_SEPARATED_DTB)

Depends on marcogiannetta-pi shinano-common / msm8974-common / kernel
(lineage-19.1 branches).

Note: z3tc.mk still carries bring-up debug options, to be removed before a
release: adb from early boot on all builds (persist.sys.usb.config=mtp,adb),
a pre-authorised adb key (PRODUCT_ADB_KEYS), adb over the network on port 5555,
Rescue Party off, persistent logcat (logpersistd) and the z3tc_perfmon logger.
