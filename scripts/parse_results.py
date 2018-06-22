from bs4 import BeautifulSoup
import json

with open("results/buildit/latest/tests/local/Results.xml") as xml:
    results = BeautifulSoup(xml, "xml")
    root = results.results.find("test-configuration")



# Print out which configuration is being tested
build_config = root.find("build-configuration").contents[0]

for group in root.find_all("group"):
    file_name = "results/"+build_config+"-"+group.find("name").contents[0]+".json"
    result = {}
    for test in group.find_all("test"):
        result[test.find("name").contents[0]] = {"result": test.find("test-execution").result.contents[0]=="SUCCESS",
                                                  "exit-code": test.find("test-execution").find("exit-code").contents[0],
                                                  "output": test.find("test-execution").find("output").contents[0]}
    with open(file_name, 'w') as fp:
        json.dump(result, fp)