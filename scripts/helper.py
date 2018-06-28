import logging
import math

def parse_input(valid_inputs, input_message="Enter a string: ", output_message="Input not valid",
                sanitise=True, null_value="", invalid_inputs = []):
    """Parse and validate input"""
    # Continue accepting input until valid
    while True:
        user_input = input(input_message)
        if (sanitise):
            user_input = user_input.replace("/", "")
            user_input = user_input.replace("\\", "")
        if user_input == "" and null_value in valid_inputs and null_value not in invalid_inputs:
            logging.debug("Input set to: {}".format(user_input))
            return null_value
        elif user_input not in valid_inputs or user_input in invalid_inputs:
            print(output_message)
        else:
            logging.debug("Input set to: {}".format(user_input))
            return user_input

def write_result(file, test_name, result, build):
    """Write data to file"""
    name_output, info_output = get_header(test_name, result, build)
    file.write(name_output + "\n")
    file.write(info_output + "\n\n")
    file.write(result["output"] + "\n")

def get_header(test_name, result, build):
    if result["result"]:
        success = "SUCCEEDED"
    else:
        success = "FAILED"
    info = "{} {}, returned {}".format(build, success, result["exit-code"])
    return (append_equals(test_name, max(len(test_name), len(info) + 10)),
            append_equals(info, max(len(test_name), len(info) + 10)))
    
def append_equals(text, length):
    equals_length = length - len(text)
    return "=" * math.floor(equals_length / 2) + text + "=" * math.ceil(equals_length / 2)