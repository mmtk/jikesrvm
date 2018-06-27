#!/usr/bin/env python
import json
import logging, sys
import glob
from pathlib import Path
import os

# Set logging level. Levels are debug, info, warning, error, critical
logging.basicConfig(stream=sys.stderr, level=logging.INFO)

##################
# Base arguments #
##################

abs_dir   = Path(os.path.abspath(os.path.dirname(__file__)))
json_dir  = abs_dir / ".." / "results" / "json"

################################
# Get all build configurations #
################################

build_dirs = os.listdir(json_dir)
logging.info(("Build configurations available are: {}").format(build_dirs))

###########################################
# Specify the json directories to compare #
###########################################

while True:
    first_build = input("Enter a build configuration: ")
    first_build.replace("/", "")
    if first_build not in build_dirs:
        print("Incorrect build configuration")
    else:
        logging.info("First config set to: {}".format(first_build))
        break

# Note should we just make this append R cos im lazy
while True:
    second_build = input("Enter a build configuration: ")
    second_build.replace("/", "")
    if second_build not in build_dirs or first_build == second_build:
        print("Incorrect build configuration")
    else:
        logging.info("Second config set to: {}".format(second_build))
        break

##############################################
# Retrieve files associated with each config #
##############################################

first_dir  = json_dir / first_build
second_dir = json_dir / second_build

first_files = list(first_dir.glob("*.json"))
second_files = list(second_dir.glob("*.json"))
logging.debug("First configuration files found: {}".format(first_files))
logging.debug("Second configuration files found: {}".format(second_files))

####################################
# Compare both configuration files #
####################################

logging.debug("Intersection: {}".format(list(set(first_files) & set(second_files))))
logging.debug("Union: {}".format(list(set(first_files) | set(second_files))))

for first_file in first_files:
    logging.debug(("{}: Processing from {}").format(first_build, first_file))
    # Get equivalent second file
    second_file = Path(str(first_file).replace(first_build, second_build))
    if not second_file.is_file():
        logging.info(("File {} not found").format(second_file))
        continue
    else:
        logging.info(("File {} found").format(second_file))
        with open(first_file) as f1, open(second_file) as f2:
            d1 = json.load(f1)
            d2 = json.load(f2)
            for (t1, r1), (t2, r2) in zip(d1.items(), d2.items()):
                logging.debug(("test1:{}, res1:{}, test2:{}, res2:{}").format(t1, r1, t2, r2))
                assert t1 == t2
                if (r2["exit-code"] != r1["exit-code"]):
                    print("=================",t1,"=================")
                    print("------ {} succeeded: {}, returned exit code {} ------".format(first_build, r1["result"], r1["exit-code"]))
                    print(r1["output"])
                    print("------ {} succeeded: {}, returned exit code {} ------".format(first_build, r2["result"], r2["exit-code"]))
                    print(r2["output"])
                    