#!/usr/bin/env python
import json
import logging, sys
import glob
from pathlib import Path
import os
import helper
import argparse
import shutil

def main():
    # Arguments
    parser = argparse.ArgumentParser(description="Generate differences of two files")
    parser.add_argument("--g1", dest="g1", default="", help="Specifies the first garbage collector to use", type=str)
    parser.add_argument("--g2", dest="g2", default="", help="Specifies the second garbage collector to use", type=str)
    parser.add_argument("-g", dest="g", default="", help="Specifies which set of garbage collectors to use", type=str)
    parser.add_argument("-v", dest="verbosity", default="warning", help="Specifies the verbosity.", type=str)

    args = parser.parse_args()

    g1, g2 = get_gc_names(args.g1, args.g2, args.g)

    set_verbosity_level(args.verbosity)

    results = gen_differences(g1,g2)

    pretty(results, indent=0)

def get_gc_names(g1, g2, g):
    if g1 == "" or g2 == "":
        if g == "":
            print("Either the set of garbage collectors must be defined using `-g` or individual garbage collectors must be defined using `--g1` and `--g2`")
            exit(1)
        return g, "R"+g
    else:
       return g1, g2

def get_file_name(file):
    return file.name.split(".")[0]

def gen_differences(g1, g2, dump=True):
    """
    Generates the differences between the tests of both garbage collectors
    Parameters:
        g1: The first garbage collector. This should be the intended result.
        g2: The second garbage collector. The collector to test on.
    Returns:
        A dictionary, of only the differences.
    """
    # Define Constants
    JIKESRVM_DIR = Path(os.path.abspath(os.path.dirname(__file__))) / ".."
    CONFIG_FILE  = JIKESRVM_DIR / "scripts" / "config.txt"
    RESULTS_DIR  = JIKESRVM_DIR / "results"
    JSON_DIR     = RESULTS_DIR / "json"
    DIFF_DIR     = RESULTS_DIR / "diff"

    g1_results_dir = JSON_DIR / g1
    g2_results_dir = JSON_DIR / g2

    assert g1_results_dir.exists() and g2_results_dir.exists(), "The results of the tests of both garbage collectors must be parsed first."
    
    g1_files, g2_files = get_result_files(g1_results_dir, g2_results_dir)

    special_cases = get_contents(CONFIG_FILE)

    all_results = {}

    for g1_file in g1_files:
        logging.info(("{}: Processing from {}").format(g1, g1_file))

        g2_file = g2_results_dir / g1_file.name
        if not g2_file.exists():
            logging.info(("File {} not found").format(g2_file))
            continue

        logging.info(("File {} found").format(g2_file))

        tests1 = get_contents(g1_file)
        tests2 = get_contents(g2_file)

        group_results = {}

        for test_name in tests1.keys():
            test_results = find_test_differences(test_name, tests1, tests2, g1_file, special_cases, g1, g2)
            if len(test_results) != 0:
                group_results[test_name] = test_results
        if len(group_results) != 0:
            all_results[get_file_name(g1_file)] = group_results

    if dump:
        dump_differences(DIFF_DIR / (g1+"_"+g2+".json"), all_results)

    return all_results 

def find_test_differences(test_name, tests1, tests2, g1_file, special_cases, g1, g2):
    results = {}
    logging.info("Comparing test {} from suite {}".format(test_name, g1_file.name))
    test_info1 = tests1.get(test_name, -1)
    test_info2 = tests2.get(test_name, -1)
    if test_info2 == -1:
        print("The tests of {} are not the same".format(g1_file.name))
        exit(1)
    exit_code1 = test_info1["exit-code"]
    exit_code2 = test_info2["exit-code"]
    result1 = test_info1["result"]
    result2 = test_info2["result"]
    test_output1 = test_info1["output"]
    test_output2 = test_info2["output"]

    # Check that the second build is not a special case
    special_results = special_cases.get(g2, {}).get(test_name, {})
    if special_results.get("result") == result2 and special_results.get("exit-code") == exit_code2:
        return results
    if (result1 != result2 or exit_code1 != exit_code2):
        results = {
            g1: test_info1,
            g2: test_info2
        }
    return results

def get_result_files(dir1, dir2):
    files1 = list(dir1.glob("*.json"))
    files2 = list(dir2.glob("*.json"))

    logging.debug("First configuration files found: {}".format(files1))
    logging.debug("Second configuration files found: {}".format(files2))
    logging.debug("Intersection: {}".format(list(set(files1) & set(files2))))
    return files1, files2

def get_contents(file):
    with open(file) as f:
        info = json.load(f)
    return info

def make_results_dir(dir, g1, g2):
    diff_results_dir = dir / (g1 + "_" + g2)
    if diff_results_dir.exists():
        shutil.rmtree(diff_results_dir)
    diff_results_dir.mkdir(parents=True, exist_ok=False) 

def get_all_files(dir):
    return os.listdir(dir)

def set_verbosity_level(level):
    levels = {
        "debug": logging.DEBUG,
        "info": logging.INFO,
        "warning": logging.WARNING,
        "error": logging.ERROR,
        "critical": logging.CRITICAL
    }
    log_level = levels.get(level.lower(), -1)

    if log_level != -1:
        logging.basicConfig(stream=sys.stderr, level=log_level)
    else:
        print("Logging level must be a value from", levels.keys())
        exit(1)

def pretty(d, indent=0):
    for key, value in d.items():
        print('\t' * indent + str(key))
        if isinstance(value, dict):
            pretty(value, indent+1)
        else:
            print('\t' * (indent+1) + str(value))

def dump_differences(file, contents):
    with open(file, "w+") as f:
        json.dump(contents, f)

if __name__ == '__main__':
    main()