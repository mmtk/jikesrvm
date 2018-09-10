#!/usr/bin/env python
import json
import logging, sys
import glob
from pathlib import Path
import os
import helper
import argparse
import shutil

# Set logging level. Levels are debug, info, warning, error, critical
logging.basicConfig(stream=sys.stderr, level=logging.ERROR)

##################
# Base arguments #
##################

abs_dir   = Path(os.path.abspath(os.path.dirname(__file__)))
config_file = abs_dir / "config.txt"
results_dir = abs_dir / ".." / "results" 
json_dir  = results_dir / "json"

#############
# Arguments #
#############

parser = argparse.ArgumentParser(description="Generate differences of two files")
parser.add_argument("--g1", dest="c1", default="", help="Specifies the first garbage collector to use", type=str)
parser.add_argument("--g2", dest="c2", default="", help="Specifies the second garbage collector to use", type=str)
args = parser.parse_args()

################################
# Get all build configurations #
################################

build_dirs = os.listdir(json_dir)
logging.info(("Build configurations available are: {}").format(build_dirs))

###########################################
# Specify the json directories to compare #
###########################################

if args.c1 != "" and args.c2 != "":
    first_build = args.c1
    second_build = args.c2
else:
    first_build = helper.parse_input(build_dirs, input_message="Enter a build configuration: ",
                    output_message="Incorrect build configuration")
    second_build = helper.parse_input(build_dirs, input_message="Enter a build configuration: ",
                    output_message="Incorrect build configuration", invalid_inputs=[first_build],
                    null_value=("R" + first_build))

################################
# Order configs alphabetically #
################################
if second_build < first_build:
    temp_build = first_build
    first_build = second_build
    second_build = temp_build
    logging.info("First: {} | Second: {}".format(first_build, second_build))

##############################################
# Retrieve files associated with each config #
##############################################

first_dir  = json_dir / first_build
second_dir = json_dir / second_build
first_files = list(first_dir.glob("*.json"))
second_files = list(second_dir.glob("*.json"))

logging.debug("First configuration files found: {}".format(first_files))
logging.debug("Second configuration files found: {}".format(second_files))
logging.debug("Intersection: {}".format(list(set(first_files) & set(second_files))))
logging.debug("Union: {}".format(list(set(first_files) | set(second_files))))

############################
# Create results directory #
############################

diff_dir = results_dir / "diff" / (first_build + "_" + second_build)
shutil.rmtree(diff_dir)
diff_dir.mkdir(parents=True, exist_ok=False) 

################################
# Load unique cases for builds #
################################

with open(config_file) as special_cases:
    special = json.load(special_cases)

############################################
# Compare each test group from each config #
############################################

for first_file in first_files:
    logging.info(("{}: Processing from {}").format(first_build, first_file))

    #####################################
    # Test if corresponding file exists #
    #####################################

    second_file = Path(str(first_file).replace(first_build, second_build))
    if not second_file.is_file():
        logging.info(("File {} not found").format(second_file))
        continue
    else:
        logging.info(("File {} found").format(second_file))
        output_file  = str(diff_dir / (os.path.splitext(os.path.basename(str(first_file)))[0]))
        output_file1 = output_file + "-" + first_build + ".txt"
        output_file2 = output_file + "-" + second_build + ".txt"

        results1 = {}
        results2 = {}

        with open(first_file) as f1, open(second_file) as f2:

            ################################
            # Compare each individual test #
            ################################

            d1 = json.load(f1)
            d2 = json.load(f2)
            for (t1, r1), (t2, r2) in zip(d1.items(), d2.items()):
                logging.debug(("test1: {}, res1: {}, test2: {}, res2: {}").format(t1, r1, t2, r2))
                assert t1 == t2
                if (r1["result"] == special.get(first_build, {}).get(t1, {}).get("result")) and r1["exit-code"] == special.get(first_build, {}).get(t1, {}).get("exit-code"):
                    continue
                if (r2["result"] == special.get(second_build, {}).get(t2, {}).get("result")) and (r2["exit-code"] == str(special.get(second_build, {}).get(t2, {}).get("exit-code"))):
                    continue
                if (r2["result"] != r1["result"] or r2["exit-code"] != r1["exit-code"]):
                    results1[t1] = r1
                    results2[t2] = r2                    

        assert len(results1) == len(results2)

        if len(results1) > 0:

            with open(output_file1, "w+") as o1, open(output_file2, 'w+') as o2:
                
                for (t1, r1), (t2, r2) in zip(results1.items(), results2.items()):
                    name_output1, info_output1 = helper.get_header(t1, r1, first_build)
                    name_output2, info_output2 = helper.get_header(t2, r2, second_build)
                    o1.write(name_output1 + "\n")
                    o1.write(info_output1 + "\n\n")
                    o1.write(r1["output"] + "\n")
                    o2.write(name_output2 + "\n")
                    o2.write(info_output2 + "\n\n")
                    o2.write(r2["output"] + "\n")

