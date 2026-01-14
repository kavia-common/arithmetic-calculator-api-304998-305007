#!/bin/bash
cd /home/kavia/workspace/code-generation/arithmetic-calculator-api-304998-305007/arithmetic_calculator_api_backend
./gradlew checkstyleMain
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

