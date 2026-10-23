# ArcOS Versioning
ARCOS_VERSION_MAJOR := V0
ARCOS_VERSION_CODENAME := ALPHA
ARCOS_VERSION := $(ARCOS_VERSION_MAJOR)-$(ARCOS_VERSION_CODENAME)

PRODUCT_SYSTEM_PROPERTIES += \
    ro.arcos.version=$(ARCOS_VERSION) \
    ro.arcos.build.date=$(shell date -u +%Y%m%d) \
    ro.arcos.device=gsi_arm64 \
    ro.build.display.id=ArcOS-$(ARCOS_VERSION)-Android16

# Include GApps (Assuming MindTheGapps or NikGApps vendor tree)
$(call inherit-product-if-exists, vendor/gapps/arm64/arm64-vendor.mk)
