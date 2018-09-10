#!/usr/bin/env python
from bs4 import BeautifulSoup
import json
import logging, sys
import pathlib
from pathlib import Path
import os
import helper
import argparse  # parsing arguments to python

# Set logging level. Levels are debug, info, warning, error, critical
logging.basicConfig(stream=sys.stderr, level=logging.ERROR)

##################
# Base arguments #
##################

abs_dir   = Path(os.path.abspath(os.path.dirname(__file__)))
base_dir  = abs_dir / ".." / "results"
json_dir  = base_dir / "json"
build_dir = base_dir / "buildit"
xml_dir   = Path("tests") / "local" / "Results.xml"

#############
# Arguments #
#############

parser = argparse.ArgumentParser(description="Parse results")
parser.add_argument("--latest", dest="latest", action="store_true",
                    help="Parse the latest results")
parser.add_argument("-r", dest="results", default="", help="Results directory")
args = parser.parse_args()

######################################
# Specify results directory to parse #
######################################

result_dirs = os.listdir(build_dir)

if args.latest:
    result_dir = "latest"
elif args.results != "":
    result_dir = args.results
else:
    result_dir = helper.parse_input(result_dirs, input_message="Enter a directory to parse results: ",
                    output_message="Directory not valid", null_value="latest")

#############################
# Open Results.xml to parse #
#############################

# Open Results.xml and get it ready for parsing
with open(build_dir / result_dir / xml_dir) as xml:
    results = BeautifulSoup(xml, "xml")
    root = results.results.find("test-configuration")

############################
# Find build configuration #
############################

# Find the configuration which can be tested
build_config = root.find("build-configuration").contents[0]
logging.debug("Build configuration: {}".format(build_config))

########################
# Find each test group #
########################

# For each group, create a dictionary of the results and dump it in a json file
for group in root.find_all("group"):

    ################################
    # Create dictionary of results #
    ################################

    # Make the dictionary with all results
    result = {}
    for test in group.find_all("test"):
        test_result = test.find("test-execution")
        result[test.find("name").contents[0]] = {"result":test_result.result.contents[0]=="SUCCESS",
                                                  "exit-code": test_result.find("exit-code").contents[0],
                                                  "output": test_result.find("output").contents[0]}

    ###################################
    # Create and dump results in json #
    ###################################

    file_dir = json_dir / build_config
    file_dir.mkdir(parents=True, exist_ok=True) 
    file_name = file_dir / (group.find("name").contents[0] + ".json")
    logging.debug("Dumping results to file: {}".format(file_name))
    with open(file_name, 'w+') as fp:
        json.dump(result, fp)