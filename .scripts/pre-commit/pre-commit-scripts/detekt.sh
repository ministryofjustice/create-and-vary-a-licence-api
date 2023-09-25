#!/bin/sh

echo "Running detekt check..."

OUTPUT="/tmp/detekt-$(date +%s)"

./gradlew detekt > "$OUTPUT"

EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
   cat "$OUTPUT"
   echo "***********************************************"
   echo "                 detekt failed                 "
   echo " Please fix the above issues before committing "
   echo "***********************************************"

else
   cat "$OUTPUT"
   echo "***********************************************"
   echo "                 detekt passed                 "
   echo "    No issues need fixing before committing    "
   echo "***********************************************"
fi

rm "$OUTPUT"
exit $EXIT_CODE
