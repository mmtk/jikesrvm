#!/usr/bin/env python
from bs4 import BeautifulSoup
import json
from pathlib import Path
import pathlib
import os

##################
# Base arguments #
##################

abs_dir   = Path(os.path.abspath(os.path.dirname(__file__)))
base_dir  = abs_dir / ".." / "results"
json_dir  = base_dir / "json"
build_dir = base_dir / "buildit"
xml_dir   = Path("tests") / "local" / "Results.xml"

######################################
# Specify results directory to parse #
######################################

# Specify a directory in the results folder to look at
result_dir = input("Enter a directory to look at: ")
# Sanitise '/' characters
result_dir.replace("/", "")
# If none is specified, look at the latest
if result_dir == "":
    result_dir = "latest"

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
        result[test.find("name").contents[0]] = {"result": test.find("test-execution").result.contents[0]=="SUCCESS",
                                                  "exit-code": test.find("test-execution").find("exit-code").contents[0],
                                                  "output": test.find("test-execution").find("output").contents[0]}

    ###################################
    # Create and dump results in json #
    ###################################

    file_dir = json_dir / build_config
    file_dir.mkdir(parents=True, exist_ok=True) 
    file_name = file_dir / (group.find("name").contents[0] + ".json")
    with open(file_name, 'w') as fp:
        json.dump(result, fp)