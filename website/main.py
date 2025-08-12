#   Copyright (c) 2024 Dynatrace LLC
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

"""Python Macros."""

import logging
import re
import yaml

from datetime import datetime
from tabulate import tabulate

logger = logging.getLogger(__name__)

camel_case_to_snake_case_pattern = re.compile(r"(?<!^)(?=[A-Z])")

def generate_markdown_table(schema_name, schema, required_fields, enum_values, display_comment, display_required):
    markdown = f'\n\n\n##<a name="{camel_case_to_snake_case_pattern.sub('-', schema_name).lower()}"></a> {schema_name}\n\n'
    markdown += f"{schema.get('description', '')}\n\n"
    markdown += "| Field | Type | Description |"
    if display_comment:
        markdown += " Comment |"
    if display_required:
        markdown +=  " Required |"
    markdown += "\n"
    markdown += "|-------|------|-------------|"
    if display_comment:
        markdown += "---------|"
    if display_required:
        markdown +=  "----------|"
    markdown += "\n"


    for field, details in schema.get("properties", {}).items():
        # do not include custom validation methods
        if field.startswith("valid"):
            continue

        field_type = details.get('format', details.get('type', 'object'))

        field_description = details.get('description', '')

        field_required = 'Yes' if (field_type == 'string' and details.get('minLength', 0) >= 1) or \
                                  (field in required_fields and "Default:" not in field_description) else 'No'

        comment = ''
        if field_type == 'string':
            if ref := details.get('allOf', []):
                assert len(ref) == 1, "Currently only ref of length 1 supported. Adapt if needed."

                if comment:
                    logger.warning(f"Existing comment ('{comment}') will be overwritten.")

                comment = f"Allowed values: {enum_values[ref[0]["$ref"].split("/")[-1]]}"

        if ref := details.get('$ref', ''):
            if comment:
                logger.warning(f"Existing comment ('{comment}') will be overwritten.")

            try:
                comment = f"Allowed values: {enum_values[ref.split("/")[-1]]}"
            except KeyError as e:
                logger.warning(f"Key error: {e}")

        if field_type == "array": # special case "var args"
            if comment:
                logger.warning(f"Existing comment ('{comment}') will be overwritten.")
            if min_items := details.get('minItems', ''):
                comment = f"Min. items: {min_items}"

        if pattern := details.get('pattern', ''):
            if comment:
                logger.warning(f"Existing comment ('{comment}') will be overwritten.")
            if pattern != "\S": # only non-trivial patterns
                # escape pipe (automatically issued warning can be ignored)
                comment = f"Pattern: `{pattern}`"

        if field == "args": # special case "var args"
            if comment:
                logger.warning(f"Existing comment ('{comment}') will be overwritten.")
            comment = "If this field is required or not depends on the `type`."
            field_required = "n/a"

        # links
        if field_type == 'object':
            if ref := details.get('allOf', []):
                assert len(ref) == 1, "Currently only ref of length 1 supported. Adapt if needed"
                field = f'[`{field}`](#{camel_case_to_snake_case_pattern.sub('-', ref[0]["$ref"].split("/")[-1]).lower()})'
            else:
                field = f"`{field}`"
        elif field_type == "array":
            if ref := details.get('items', {}):
                assert len(ref) == 1, "Currently only ref of length 1 supported. Adapt if needed"
                field = f'[`{field}`](#{camel_case_to_snake_case_pattern.sub('-', ref["$ref"].split("/")[-1]).lower()})'
            else:
                field = f"`{field}`"
        else:
            field = f"`{field}`"

        markdown += f"| {field} | {field_type} | {field_description} |"
        if display_comment:
            markdown += f" {comment} |"
        if display_required:
            markdown += f" {field_required} |"
        markdown += "\n"

    return markdown

def define_env(env):
    @env.macro
    def current_year():
        return datetime.now().year

    @env.macro
    def external_link(name, href, target="_blank"):
        return f"<a href=\"{href}\" target=\"{target}\">{name}</a>"

    @env.macro
    def generate_model_doc(filename):
        # Load YAML content from a file
        with open(filename, "r") as file:
            data = yaml.safe_load(file)

        # Navigate to the openAPIV3Schema properties
        schema = data['spec']['versions'][0]['schema']['openAPIV3Schema']['properties']['spec']

        # Extract default and pattern from description
        def clean_description(description):
            default = ''
            pattern = ''
            # Extract default
            default_match = re.search(r'Default:\s*(.*?)(?:\\n|$)', description)
            if default_match:
                default = f"`{default_match.group(1).strip()}`"
                description = re.sub(r'Default:\s*.*?(?:\\n|$)', '', description).strip()
            pattern_match = re.search(r'Pattern:\s*(.*?)(?:\\n|$)', description)
            if pattern_match:
                pattern = f"`{pattern_match.group(1).strip()}`"
                description = re.sub(r'Pattern:\s*.*?(?:\\n|$)', '', description).strip()
            return description, default, pattern

        def extract_tables(properties, required_fields=None, parent_key='spec'):
            tables = {parent_key: []}
            required_fields = required_fields or []

            for key, value in properties.items():
                field_type = value.get('type', 'object')
                description = value.get('description', '')
                default = value.get('default', '')
                description, extracted_default, constraint = clean_description(description)
                default = default or extracted_default

                # Handle enums
                enum_values = value.get('enum')
                if enum_values:
                    enum_str = ", ".join(map(lambda s: f"`{s}`", map(str, enum_values)))
                    constraint = f"{constraint}, {enum_str}" if constraint else f"{enum_str}"

                # Special handling for scaleDriver fields
                if parent_key == 'scaleDriver' and key != 'type':
                    is_required = 'n/a'
                else:
                    is_required = 'Yes' if key in required_fields else 'No'

                # Escape all fields
                description = description
                default = default
                constraint = constraint

                # Link object and array fields to their anchors
                if field_type == 'object' and 'properties' in value:
                    anchor = key.lower()
                    link = f"[{key}](#{anchor})"
                    tables[parent_key].append([link, field_type, description, default, constraint, is_required])
                    nested_key = key
                    tables.update(extract_tables(value['properties'], value.get('required', []), nested_key))

                elif field_type == 'array' and 'items' in value and 'properties' in value['items']:
                    anchor = key.lower()
                    link = f"[{key}](#{anchor})"
                    tables[parent_key].append([link, field_type, description, default, constraint, is_required])
                    nested_key = key
                    tables.update(extract_tables(value['items']['properties'], value['items'].get('required', []), nested_key))

                else:
                    tables[parent_key].append([key, field_type, description, default, constraint, is_required])

            return tables


        # Extract tables
        tables = extract_tables(schema['properties'], schema.get('required', []))

        # Assemble Markdown string
        markdown = ""
        for table_name, rows in tables.items():
            anchor = table_name.lower()
            markdown += f"\n### `{table_name}` <a name=\"{anchor}\"></a>\n\n"
            markdown += tabulate(rows, headers=["Field Name", "Type", "Description", "Default", "Constraint", "Required"], tablefmt="github")
            markdown += "\n"

        return markdown
