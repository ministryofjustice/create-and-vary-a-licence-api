#!/bin/sh

echo "Running ktlint check..."

OUTPUT="/tmp/ktlint-$(date +%s)"

./gradlew ktlintFormat > "$OUTPUT"

EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
   cat "$OUTPUT"
   echo "***********************************************"
   echo "                 ktlint failed                 "
   echo " Please fix the above issues before committing "
   echo "***********************************************"
else
   cat "$OUTPUT"
   echo "***********************************************"
   echo "                 ktlint passed                 "
   echo "    No issues need fixing before committing    "
   echo "***********************************************"
fi

rm "$OUTPUT"
exit $EXIT_CODE
