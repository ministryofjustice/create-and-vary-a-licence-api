#!/bin/sh

echo "Running ktlint check..."

OUTPUT="/tmp/ktlint-$(date +%s)"

./gradlew ktlintFormat > "$OUTPUT"

EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
   cat "$OUTPUT"
   rm "$OUTPUT"

   echo "***********************************************"
   echo "                 ktlint failed                 "
   echo " Please fix the above issues before committing "
   echo "***********************************************"

   exit $EXIT_CODE
else
   cat "$OUTPUT"
   rm "$OUTPUT"

   echo "***********************************************"
   echo "                 ktlint passed                 "
   echo "    No issues need fixing before committing    "
   echo "***********************************************"

   exit $EXIT_CODE
fi

rm "$OUTPUT"
