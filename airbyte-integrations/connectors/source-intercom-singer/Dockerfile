FROM airbyte/integration-base-singer:0.1.1

# Bash is installed for more convenient debugging.
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update \
    && apt-get install -y bash \
    && apt-get install -y gcc \
    && rm -rf /var/lib/apt/lists/*

ENV CODE_PATH="source_intercom_singer"
ENV AIRBYTE_IMPL_MODULE="source_intercom_singer"
ENV AIRBYTE_IMPL_PATH="SourceIntercomSinger"

LABEL io.airbyte.version=0.2.1
LABEL io.airbyte.name=airbyte/source-intercom-singer

WORKDIR /airbyte/integration_code
COPY $CODE_PATH ./$CODE_PATH
COPY setup.py ./

RUN pip install .
