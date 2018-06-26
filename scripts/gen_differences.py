import json
import logging, sys
import glob
from pathlib import Path


# Set logging level. Levels are debug, info, warning, error, critical
logging.basicConfig(stream=sys.stderr, level=logging.ERROR)

# Get all json files that are appended with R
rust_files = glob.glob("results/R*.json")
logging.debug(("All rust files found: {}").format(rust_files))

# Iterate through all rust files
for rust_file in rust_files:
    logging.info(("Processing {}").format(rust_file))
    # Check if there are any equivalent Java MMTk json files
    java_file = Path(rust_file.replace("results/R","results/"))
    # Check that there is an equivalent java file, otherwise skip
    if not java_file.is_file():
        logging.warn("File not found")
        continue
    # Otherwise compare the two files with each other
    else:
        logging.info(("File at {} is found").format(java_file))
        # Open rust and java files to compare them
        with open(java_file) as jf, open(rust_file) as rf:
            logging.debug("Successfully opened java and rust file")
            # Load both data
            java_data = json.load(jf)
            rust_data = json.load(rf)
            for (jtest,jres), (rtest,rres) in zip(java_data.items(), rust_data.items()):
                logging.debug(("jtest:{}, jres:{}, rtest:{}, rres:{}").format(jtest, jres, rtest, rres))
                # These tests should be the same
                assert jtest == rtest, "Tests are not in correct order / missing. Most likely something is wrong with parser or testing framework"
                if (rres["result"] != jres["result"]):
                    if not rres["result"]:
                        print("=================",jtest,"=================")
                        print("------ Java succeeded:", jres["result"],"------")
                        print("------ Java returned exit code", jres["exit-code"],"------")
                        print(jres["output"])
                        print("------ Rust succeeded:", rres["result"],"------")
                        print("------ Rust returned exit code", rres["exit-code"],"------")
                        print(rres["output"])
                else:
                    if (rres["exit-code"] != jres["exit-code"]):
                        print("=================",jtest,"=================")
                        print("------ Java returned exit code", jres["exit-code"],"------")
                        print("------ Java succeeded:", jres["result"],"------")
                        print(jres["output"])
                        print("------ Rust returned exit code", rres["exit-code"],"------")
                        print("------ Rust succeeded:", rres["result"],"------")
                        print(rres["output"])
