FROM python:3.8-slim-buster

WORKDIR /app

COPY end-to-end-tests/. .
RUN pip3 install -r requirements.txt

ENTRYPOINT [ "python3", "end_to_end_tests.py", "-d", "host.docker.internal:8080" ]