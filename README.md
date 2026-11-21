Device configuration for Sony Xperia Z3 Tablet Compact (LineageOS 19.1)
=======================================================================

Codename: scorpion / z3tc (SGP621, SGP641 - LTE)

Status: UNTESTED bring-up tree.

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

Note: z3tc.mk enables insecure adb (ro.adb.secure=0) for bring-up.
