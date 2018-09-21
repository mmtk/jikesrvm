#!/usr/bin/env python
from bs4 import BeautifulSoup
import json
import logging
from pathlib import Path
import os
import helper
import argparse  # parsing arguments to python
from tabulate import tabulate

def main():
    # Arguments
    parser = argparse.ArgumentParser(description="Parse results")
    parser.add_argument("--latest", dest="latest", action="store_true", default=False,
                        help="Parse the latest results")
    parser.add_argument("--dump", dest="dump", action="store_true", default=False,
                        help="Parse the latest results")
    parser.add_argument("-r", dest="results", default="", help="Results directory")
    parser.add_argument("-v", dest="verbosity", default="warning", help="Set the verbosity level")
    args = parser.parse_args()

    helper.set_verbosity_level(args.verbosity)
    results = parse_results(args.results, args.latest)

    output_results(results, only_fails=True, html=True)

def output_results(results, only_fails=False, html=False):
    fmt = "html" if html else "fancy_grid"
    wrap1 = "<h1>" if html else ""
    wrap2 = "</h1>" if html else ""
    summary, tables = summary_tables(results, only_fails,fmt)

    print(wrap1 + "Summary" + wrap2)
    print(tabulate(summary, headers="firstrow", tablefmt=fmt))
    for test_suite, table in tables.items():
        print(wrap1 + test_suite + wrap2)
        print(table)

def summary_tables(results, only_fails, fmt):
    summary = [["Test Suite", "Passed", "Failed", "Total"]]
    tables = {}
    for test_suite, tests in results.items():
        passed = 0
        table = [["Test", "Result", "Reason"]]
        for test, test_result in tests.items():
            test_col = [test, test_result['result'], test_result['reason']]
            if test_result['result'] == "SUCCESS":
                passed+=1
                if only_fails:
                    continue
            table.append(test_col)
        summary.append([test_suite, passed, len(tests) - passed, len(tests)])
        tables[test_suite] = tabulate(table, headers="firstrow", tablefmt=fmt)
    return summary, tables

def parse_results(results, latest=False, dump=False):
    ABS_DIR   = Path(os.path.abspath(os.path.dirname(__file__)))
    JIKESRVM_DIR = ABS_DIR / ".." 
    RESULTS_DIR  = JIKESRVM_DIR / "results"
    PARSED_DIR  = RESULTS_DIR / "json"
    BUILDS_DIR = RESULTS_DIR / "buildit"
    XML_DIR   = Path("tests") / "local" / "Results.xml"

    result_dir = "latest" if latest else results
    result_dir = BUILDS_DIR / result_dir
    logging.debug("Result directory: {}".format(result_dir))
    assert result_dir.exists(), "Must have already run the tests"
        
    # Open Results.xml and get it ready for parsing
    with open(str(result_dir / XML_DIR)) as xml:
        results = BeautifulSoup(xml, "xml")
        root = results.results.find("test-configuration")

    build_config = root.find("build-configuration").contents[0]
    logging.debug("Build configuration: {}".format(build_config))

    all_results = {}

    for group in root.find_all("group"):
        # Make the dictionary with all results
        result = {}
        for test in group.find_all("test"):
            test_result = test.find("test-execution")
            result[test.find("name").contents[0]] = {"result":test_result.result.contents[0],
                                                    "exit-code": test_result.find("exit-code").contents[0],
                                                    "output": test_result.find("output").contents[0],
                                                    "reason": (test_result.find("result-explanation").contents + [""])[0]}
        if dump:
            file_dir = PARSED_DIR / build_config
            file_dir.mkdir(parents=True, exist_ok=True) 
            file_name = file_dir / (group.find("name").contents[0] + ".json")
            logging.debug("Dumping results to file: {}".format(file_name))
            with open(str(file_name), 'w+') as fp:
                json.dump(result, fp)
        all_results[group.find("name").contents[0]] = result
    return all_results

if __name__ == '__main__':
    main()