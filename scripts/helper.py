import logging

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
    file.write("================={}=================\n".format(test_name))
    file.write("---- {} succeeded: {}, returned exit code {} ----\n\n\n".format(build, result["result"], result["exit-code"]))
    file.write(result["output"]+"\n")