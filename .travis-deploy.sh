#!/bin/sh

#tox -c tox.ini -e buildexe
aws s3 cp --acl public-read --recursive /home/travis/build/xbr/xbr-is-gold/app/build/outputs/apk/release/ s3://${AWS_S3_BUCKET_NAME}/xbr-is-gold/android/
#aws s3api copy-object --acl public-read --copy-source ${AWS_S3_BUCKET_NAME}/crossbarfx/linux-amd64/crossbarfx-${CROSSBAR_BUILD_ID} --bucket ${AWS_S3_BUCKET_NAME} --key crossbarfx/linux-amd64/crossbarfx-latest
