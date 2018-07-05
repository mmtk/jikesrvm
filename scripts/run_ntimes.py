#!/usr/bin/env python3
# Code snippets from https://github.com/caizixian/menthol
import sys
import subprocess
import datetime
import pathlib
import socket


def mkdirp(path):
    pathlib.Path(path).mkdir(parents=True, exist_ok=True)


def main():
    n = int(sys.argv[1])
    app = sys.argv[2:]

    date_str = datetime.datetime.now().strftime("%Y-%m-%d-%H%M%S")
    hostname = socket.gethostname()
    log_dir = "{}-{}".format(hostname, date_str)

    mkdirp(log_dir)

    for i in range(n):
        print(i, end="", flush=True)
        p = subprocess.run(app, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        stdout_file = pathlib.Path(log_dir) / "{}[{}].stdout".format(i, p.returncode)
        stderr_file = pathlib.Path(log_dir) / "{}[{}].stderr".format(i, p.returncode)
        with stdout_file.open("w") as stdoutfd, stderr_file.open("w") as stderrfd:
            stdoutfd.write(p.stdout.decode("utf-8"))
            stderrfd.write(p.stderr.decode("utf-8"))
        print(".", end="", flush=True)

    print()


if __name__ == '__main__':
    main()
