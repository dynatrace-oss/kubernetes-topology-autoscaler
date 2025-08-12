ARG PROJ_VERSION
ARG ALPINE_VERSION="3.20"
ARG PYTHON_VERSION="3.12"
ARG PYTHON_VERSION_MICRO="${PYTHON_VERSION}.7"

FROM alpine:${ALPINE_VERSION} AS build

RUN apk upgrade --no-cache
RUN apk add gcc python3-dev musl-dev linux-headers

COPY --from=ghcr.io/astral-sh/uv:0.5.2 /uv /usr/local/bin/uv

ENV UV_LINK_MODE=copy \
    UV_COMPILE_BYTECODE=1 \
    UV_PYTHON=python3.12 \
    UV_PROJECT_ENVIRONMENT=/app

COPY . /app

WORKDIR /app

RUN --mount=type=cache,target=/root/.cache \
    --mount=type=bind,source=uv.lock,target=uv.lock \
    --mount=type=bind,source=pyproject.toml,target=pyproject.toml \
    uv build

###########################################################################

FROM python:${PYTHON_VERSION_MICRO}-alpine${ALPINE_VERSION}

RUN apk upgrade --no-cache

RUN <<EOT
addgroup -S app
adduser -S -D -h /app -G app app
EOT

COPY --from=build --chown=app:app /app/dist /app/dist

RUN pip install --no-cache /app/dist/kta_python_sdk-${PROJ_VERSION}-py3-none-any.whl

USER app
WORKDIR /app

# docker run [options] <tag> <your-entrypoint>.py
ENTRYPOINT ["python3"]
