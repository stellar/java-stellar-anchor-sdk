FROM python:3.10-alpine

WORKDIR /app

COPY ./end-to-end-tests/. .
RUN pip3 install -r requirements.txt

ENTRYPOINT [ "python3", "end_to_end_tests.py"]