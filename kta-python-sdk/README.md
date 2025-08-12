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

# KTA Python SDK

This folder contains the source code of the KTA Python SDK.

---

## Contributor Guide

**Minimal Setup**

- Unix-like system (e.g., Linux, MacOS, Windows with WSL)
- Package/Project management: [uv](https://docs.astral.sh/uv/getting-started/installation/) (tested with 0.5.2)

**Optional**: [Docker](https://docs.docker.com/engine/install/) (if you want to build the container image from source)

### Setup

Install Python 3.12 using

```bash
uv python install 3.12
```

Then run

```bash
uv sync --all-extras
```

which will automatically create a virtual environment (`.venv`) and install all dependencies (incl. dev and optional dependencies).
For further information on `uv` commands, check the [uv guides](https://docs.astral.sh/uv/guides/).

### Code Style and Static Code Analysis

We use [ruff](https://github.com/astral-sh/ruff) for code formatting and code style enforcement, and [mypy](https://github.com/python/mypy) for static type checking.

```bash
uv run ruff check --fix src test
uv run ruff format src test
```

```bash
uv run mypy src
```

> [!IMPORTANT]  
> If you get an error message similar to `error: Cannot find implementation or library stub for module named "<module name"`, this might indicate there is something wrong with your setup.
> In this case, deactivate and remove the used virtual environment, follow the steps in [Setup](#setup) and rerun the commands.

### Testing

To run the test suite, use

```bash
uv run pytest test
```

### Build From Source

#### Local Build

To build the Python SDK locally, use

```bash
uv build --wheel
```

#### Container Image

To build the Python SDK container image, use

```bash
docker build -t <tag> -f docker/Dockerfile .
```
