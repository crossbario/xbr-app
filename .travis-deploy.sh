#!/bin/sh

# ./mobile/build/generated/res/microapk/release/raw/android_wear_micro_apk.apk
# ./mobile/build/outputs/apk/debug/mobile-debug.apk
# ./mobile/build/outputs/apk/androidTest/debug/mobile-debug-androidTest.apk
# ./mobile/build/outputs/apk/release/mobile-release-unsigned.apk
# ./wear/build/outputs/apk/debug/wear-debug.apk
# ./wear/build/outputs/apk/androidTest/debug/wear-debug-androidTest.apk
# ./wear/build/outputs/apk/release/wear-release-unsigned.apk

# android mobile
#
aws s3 cp --acl public-read ./mobile/build/outputs/apk/debug/mobile-debug.apk s3://${AWS_S3_BUCKET_NAME}/xbr-is-gold/android-mobile/xbr-is-gold-${CROSSBAR_BUILD_ID}.apk
aws s3api copy-object --acl public-read --copy-source ${AWS_S3_BUCKET_NAME}/xbr-is-gold/android-mobile/xbr-is-gold-${CROSSBAR_BUILD_ID}.apk --bucket ${AWS_S3_BUCKET_NAME} --key xbr-is-gold/android-mobile/xbr-is-gold-latest.apk

# android wear
#
aws s3 cp --acl public-read ./wear/build/outputs/apk/debug/wear-debug.apk s3://${AWS_S3_BUCKET_NAME}/xbr-is-gold/android-wear/xbr-is-gold-${CROSSBAR_BUILD_ID}.apk
aws s3api copy-object --acl public-read --copy-source ${AWS_S3_BUCKET_NAME}/xbr-is-gold/android-wear/xbr-is-gold-${CROSSBAR_BUILD_ID}.apk --bucket ${AWS_S3_BUCKET_NAME} --key xbr-is-gold/android-wear/xbr-is-gold-latest.apk
