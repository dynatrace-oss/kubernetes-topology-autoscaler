<!--
   Copyright (c) 2024 Dynatrace LLC
  
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
   
       http://www.apache.org/licenses/LICENSE-2.0
  
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
  -->

# KTA Website

This folder contains the source code of the KTA website.

---

## Contributor Guide

The website uses [`mkdocs`](https://www.mkdocs.org/) with the [Material theme](https://squidfunk.github.io/mkdocs-material/).

**Minimal Setup**

- Unix-like system (e.g., Linux, MacOS, Windows with WSL)
- Package/Project management: [uv](https://docs.astral.sh/uv/getting-started/installation/) (tested with 0.5.2)

### Setup

Install Python 3.12 using

```shell
uv python install 3.12
```

Then run

```shell
uv venv --python=3.12
```

to create a virtual environment.

Activate it using

```shell
source .venv/bin/activate
```

Finally, run

```shell
uv pip install -r requirements.txt
```

to install the needed dependencies.

For further information on `uv` commands, check the [uv guides](https://docs.astral.sh/uv/guides/).

### Linting

Go to the project root and run

```bash
make markdownlint-docs
```

to check for potential violations.

### Verify Changes to the Website During Development

1. If you made changes to `kta-kubernetes-operator`, build it and copy the generated Custom Resource Definition to `docs/assets/model-spec/schema.yml`. This file will be used to autogenerate the API reference for the Kubernetes Operator.
2. Check if the website looks as expected.

    - `mkdocs serve` - Start the live-reloading server.
    - `mkdocs build` - Build the website.

## Deployment

The website is deployed manually.
For release-specific versioning, we will use [`mike`](https://github.com/jimporter/mike) in the future.
