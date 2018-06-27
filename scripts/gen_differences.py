#!/usr/bin/env python
import json
import logging, sys
import glob
from pathlib import Path
import os
import helper

# Set logging level. Levels are debug, info, warning, error, critical
logging.basicConfig(stream=sys.stderr, level=logging.ERROR)

##################
# Base arguments #
##################

abs_dir   = Path(os.path.abspath(os.path.dirname(__file__)))
results_dir = abs_dir / ".." / "results" 
json_dir  = results_dir / "json"

################################
# Get all build configurations #
################################

build_dirs = os.listdir(json_dir)
logging.info(("Build configurations available are: {}").format(build_dirs))

###########################################
# Specify the json directories to compare #
###########################################

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

diff_dir = results_dir / (first_build + "_" + second_build)
diff_dir.mkdir(parents=True, exist_ok=True) 

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
        with open(first_file) as f1, open(second_file) as f2, open(output_file1, 'w+') as o1, open(output_file2, 'w+') as o2:
            
            ################################
            # Compare each individual test #
            ################################
        
            d1 = json.load(f1)
            d2 = json.load(f2)
            for (t1, r1), (t2, r2) in zip(d1.items(), d2.items()):
                logging.debug(("test1: {}, res1: {}, test2: {}, res2: {}").format(t1, r1, t2, r2))
                assert t1 == t2
                if (r2["result"] != r1["result"] or r2["exit-code"] != r1["exit-code"]):
                    helper.write_result(o1, t1, r1, first_build)
                    helper.write_result(o2, t2, r2, second_build)
                    