import re
import sys
import os

if len(sys.argv) != 2:
    print "Usage: python run-check.py run_log"

file = sys.argv[1]

with open(file, 'r') as f:
    # skip first line
    f.readline()

    content = f.read()
    match = re.match('(.* \d+ \d+... (\d+ab)+\nskip sync results to remote\n?)+', content)
    if match and match.group() == content:
        print "Success run"
        sys.exit(0)
    else:
        print "Failed run"
        sys.exit(1)