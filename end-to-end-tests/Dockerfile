FROM python:3.10-alpine

WORKDIR /app

COPY end-to-end-tests/requirements.txt requirements.txt
RUN pip3 install -r requirements.txt

COPY end-to-end-tests/. .

Entrypoint [ "python3", "end_to_end_tests.py"]