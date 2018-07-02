#!/usr/bin/env python3
#!/usr/bin/env python3
import subprocess
import requests

class Machine(object):
    def __init__(self, properties):
        for k, v in properties:
            self.__setattr__(k, v)

    def __str__(self):
        return "{}.moma 10.0.0.{}[{}] {}".format(
            self.host, self.ip, self.hwid, self.role
        )

    def is_reserved(self):
        r = requests.get(
            "http://squirrel.anu.edu.au/reserve-check",
            params={"host": self.host}
        )
        return len(r.text)!=0

    @staticmethod
    def get_machines():
        ps = subprocess.run(
            "ssh squirrel.moma cat /moma-admin/config/machines.lst".split(),
            stdout = subprocess.PIPE,
            stderr = subprocess.PIPE
        )
        lines = [line.decode("utf-8") for line in ps.stdout.splitlines()]
        headers = Machine.__split_line(lines[0][1:])
        lines = lines[1:]
        machines = []
        for line in lines:
            if not line:
                continue
            if line[0] == "#":
                continue # commented out machines
            fields = Machine.__split_line(line)
            machines.append(Machine(zip(headers, fields)))
        return machines   
    
    @staticmethod
    def __split_line(line):
        return [token.strip() for token in line.split(",")]

def get_unreserved_list():
    unreserved = []
    for m in Machine.get_machines():
        if not m.is_reserved():
            unreserved.append(m)
    return unreserved

def main():
    for m in Machine.get_machines():
        print(m)
        print(m.is_reserved())

if __name__ == "__main__":
    main()
