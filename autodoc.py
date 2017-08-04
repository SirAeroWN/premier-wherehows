## templates for building each document section




# class for holding all the strings associated with a doc section
class DocSection:
    """docstring for DocSection"""
    def __init__(self, http_request_type, url, controller):
        self.http_request_type = http_request_type

        self.url = url

        self.controller = controller

        # {0} = number, usually 400 or 404; {1} = string message
        self.error_template = "{\n\t\"return_code\": {0},\n\t\"error_message\": \"{1}\"\n}"

        # {0} = lowercase, dashed version of {1}; {1} = title for section, usually of a from with url descriptor followed by http request type
        self.link_source_template = "<a href=\"{0}\">{1}</a>"

        # {0} = lowercase, dashed version of {1}; {1} = title for section, usually of a from with url descriptor followed by http request type
        self.link_destination_template = "####<a name=\"{0}\">{1}</a>"

        # {0} = Parameter Name; {1} Description; {2} = Default; {3} = Required (Y/N)
        self.table_row_template = "| {0} | {1} | {3} | {4} |"

        # {0} = link_destination; {1} = url; {2} = http request type; {3} = table row; {4} = sucess_response_json; {5} error_response_json; {6} = sample call
        self.doc_section_template = "{0}\n* **URL**\n\t{1}\n* **Method:**\n\t`{2}`\n* **Data Params**\n| Parameter Name | Description | Default | Required |\n| ----------- | ----------- | ------- |:--------:|\n{3}\n* **Success Response:**\n```json\n{4}\n```\n* **Error Response:**\n```json\n{5}\n```\n* **Sample Call**\n```\n{6}\n"

        self.error = ''

        self.link_source = ''

        self.link_destination = ''

        self.table_row = ''

        self.doc_section = ''


    # convert a header title to destination name
    def destination_from_header(self, header):
        destination = header.lower().replace(" ", "-")
        return destination

    # convert destination to source
    def source_from_destination(self, destination):
        source = "#" + destination
        return source

    # parse url for generating names
    def get_section_name(self):
        words = self.url.split("/")[1:]
        for i, word in enumerate(words):
            if len(word) > 0 and word[0] in [':', '*']:
                words[i] = word[1:]
        section_name = ' '.join(words).title()
        return section_name

    def get_header(self):
        section_name = self.get_section_name()
        header = section_name + ' ' + self.http_request_type.upper()
        return header

    # parse url for getting arguments, return list of lists with arg + type pair
    def get_url_arguments(self):
        args = re.findall('(?<=:)[a-z]+', self.url)
        args.append(re.findall('(?<=\*)[a-z]+', self.url))

        arg_and_type = []
        for arg in args:
            parameter_type = re.search('(?<={0}: )[A-Z][a-z]+'.format(arg), self.controller)
            if not parameter_type:
                parameter_type = ''
            else:
                parameter_type = parameter_type.group()
            arg_and_type.append([ arg, parameter_type ])

        return arg_and_type

    # fill out links
    def fill_links(self):
        header = self.edit_entry('header', self.get_header())

        destination = self.destination_from_header(header)

        self.link_destination = self.link_destination_template.format(destination, header)

        source = self.source_from_destination(destination)

        self.link_source = self.link_source_template.format(source, header)

    # fill out the example error (support for multiple errors?)
    def fill_error(self):
        # first check if we want to use the default of 400 and 'Exception message'
        # if 'y' or empty use default, if 'p' edit just port, if 'm' edit just message, if 'n' edit both
        prompt = 'Use default error? (code: 400; message: Exception message)[y/p/n] '
        resp = input(prompt).strip()

    # generic edit function
    def edit_entry(self, entry_name, entry):
        prompt = '>> Edit ' + entry_name + ': "' + entry + '": '

        edit_string = input(prompt).strip()

        if not len(edit_string) > 0:
            return entry
        elif edit_string[0:2] == 's/':
            __, original, replacement = edit_string.split('/')
            entry = entry.replace(original, replacement)
            return entry
        else:
            return edit_string




## routes file parsing functions

# split a line into its parts, should be three parts, concat anything beyond that into 3rd piece
#     [ http request type, url, controller function ]
def split_routes_line(line):
    components = line.strip().split()

    # components should have a length of three, if it doesn't then combine enything beyond that into the third item
    if (len(components) > 3):
        # concat the end
        components[2] = " ".join(components[2:])

        # now get rid of the extra strings
        del components[3:]

    return components



## orchestration functions

# function to test if line has a route
def check_line(line):
    if len(line) < 3 or line[0] == '#':
        return False
    else:
        return True



## testing

def test_link_generation(line):
    comp = split_routes_line(line)
    test_doc = DocSection(comp[0], comp[1], comp[2])
    test_doc.fill_links()
    print(test_doc.link_source, test_doc.link_destination)

if __name__ == '__main__':
    test_link_generation('GET         /dataset/after/:type/:time    controllers.DatasetController.getLatestAfter(type: String, time: Long)')