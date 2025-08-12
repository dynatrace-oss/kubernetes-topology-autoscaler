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

from datetime import datetime

def on_config(config, **kwargs):
    config.copyright = f"Copyright &copy; 2024-{datetime.now().year} <a href='https://dynatrace.com' target='_blank'>Dynatrace LLC</a> | Created by <a href='https://research.dynatrace.com' target='_blank'>Dynatrace Research</a> | Version: {config.extra.get('version')}"