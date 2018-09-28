#!/usr/bin/env python
from django.template import Template
from django.conf import settings
from django.template.loader import get_template
import django
from pathlib import Path
import os
import json
import subprocess
import argparse

def main():
    parser = argparse.ArgumentParser(description="Generate the HTML based on the difference of two files")
    parser.add_argument("--g1", dest="g1", default="", help="Specifies the first garbage collector to use", type=str)
    parser.add_argument("--g2", dest="g2", default="", help="Specifies the second garbage collector to use", type=str)
    parser.add_argument("-g", dest="g", default="", help="Specifies which set of garbage collectors to use", type=str)
    parser.add_argument("--hash", dest="hash", default="", help="Specifies which hash to generate the HTML from", type=str)
    parser.add_argument("-v", dest="verbosity", default="warning", help="Specifies the verbosity.", type=str)

    args = parser.parse_args()

    g1, g2 = get_gc_names(args.g1, args.g2, args.g)

    generate_html(g1, g2, args.hash)

def generate_html(g1, g2, hash=""):
    JIKESRVM_DIR = Path(os.path.abspath(os.path.dirname(__file__))) / ".."
    HTML_DIR     = JIKESRVM_DIR / "results" / "html"
    file = g1 + "_" + g2 + ".json"

    git_hash = get_hash() if hash == "" else hash
    hash_date = get_hash_date(git_hash)

    with open(str(HTML_DIR / hash_date / file), "r") as f:
        comparison = json.load(f)
    output_html(g1,g2,  comparison, git_hash)
    

    summary_html = generate_hash_html(HTML_DIR / hash_date, git_hash)
    save_html(HTML_DIR / hash_date, "summary", summary_html)
    all_summary = get_all_summary(HTML_DIR)
    save_html(HTML_DIR, "index", all_summary)

def get_gc_names(g1, g2, g):
    if g1 == "" or g2 == "":
        if g == "":
            print("Either the set of garbage collectors must be defined using `-g` or individual garbage collectors must be defined using `--g1` and `--g2`")
            exit(1)
        return g, "R"+g
    else:
       return g1, g2

def output_html(g1, g2, comparison, git_hash):
    JIKESRVM_DIR = Path(os.path.abspath(os.path.dirname(__file__))) / ".."
    HTML_DIR     = JIKESRVM_DIR / "results" / "html"
    hash_name = get_hash_date(git_hash)
    test_suites, failed = parse_gen(comparison, g1, g2)
    html = generate_gc_html(g1, g2, test_suites, git_hash, failed)
    save_html(HTML_DIR / hash_name, "_".join([g1,g2]), html)

def get_hash_date(git_hash):
    hash_date =  subprocess.check_output(["git", "show", "--no-patch", "--no-notes", "--pretty=%h_%cI", git_hash]).strip().decode()
    return hash_date.split("+", 1)[0].replace(":", "").replace("T", "-")

def get_hash():
    return subprocess.check_output(["git", "rev-parse", "--short", "HEAD"]).strip().decode()

def save_html(HTML_DIR, file_name, html):
    html_doc = HTML_DIR
    html_doc.mkdir(exist_ok=True)
    html_doc = html_doc / (file_name + ".html")
    with open(str(html_doc), "w+") as f:
        f.write(html)

def parse_gen(comparison, g1, g2):
    test_suites = {}
    for suite, suite_info in comparison['suites'].items():
        tests = {}
        for test, test_vals in suite_info.items():
            tests[test] = {
                "result": test_vals['result'],
                "exit_code": test_vals['exit_code'], 
                "output": test_vals['output'], 
                "reason": test_vals['reason'],
                "correct": test_vals['correct']
            }
        test_suites[suite] = tests
    return test_suites, comparison['fails']

def generate_gc_html(g1, g2, tests, hash, failed):
    JIKESRVM_DIR = Path(os.path.abspath(os.path.dirname(__file__))) / ".."
    TEMPLATE_DIR = JIKESRVM_DIR / "scripts" / "templates"

    TEMPLATES = [
        {
            'BACKEND': 'django.template.backends.django.DjangoTemplates',
            'DIRS': [str(TEMPLATE_DIR)],
        }
    ]

    settings.configure(TEMPLATES=TEMPLATES)
    django.setup()

    template = get_template('garbage_collector.html')

    all_tests = {}
    for test in tests.values():
        all_tests.update(test)

    c = {"gcname1": g1,
        "gcname2": g2,
        "tests": all_tests,
        "hash": hash,
        "wrong": failed
    }
    
    return template.render(c)

def generate_hash_html(json_dir, git_hash):
    template = get_template('test_summary.html')
    hash_summary = get_hash_summary(json_dir)

    c = {
        "hash": git_hash,
        "tests": hash_summary
    }

    return template.render(c)

def get_hash_summary(json_dir):
    hash_summary = {}
    files = list(json_dir.glob("*.json"))
    for file in files:
        with open(str(file), "r") as f:
            summary = json.load(f)
        test_name, test_fails = "_".join(summary['name']), summary['fails']
        hash_summary[test_name] = test_fails
    return hash_summary

def get_all_summary(dirs):
    JIKESRVM_DIR = Path(os.path.abspath(os.path.dirname(__file__))) / ".."
    
    template = get_template('index.html')
    dates = get_hashes_dates(dirs)
    summary = get_hash_results(dirs, dates)
    return template.render({'summary': summary})

def get_hashes_dates(dirs):
    dates = {}
    for direc in dirs.glob("*"):
        if direc.is_dir():
            git_hash, date = direc.name.split("_")
            dates[date] = git_hash
    return dates

def get_hash_results(dirs, dates):
    summary = {'hashes': [], 'results': {}}
    for i, date in enumerate(sorted(dates.keys())):
        summary['hashes'].append([dates[date], "_".join([dates[date], date])])
        for files in (dirs / ("_".join([dates[date], date]))).glob("*.json"):
            with open(str(files), "r") as f:
                results = json.load(f)
                file_name = get_file_name(files)
                if not file_name in summary['results']:
                    summary['results'][file_name.strip("'")] = ['' for _ in range(i)]
                summary['results'][file_name].append(results['fails'])
    return summary

def get_file_name(file):
    return file.name.split(".")[0]

if __name__ == '__main__':
    main()