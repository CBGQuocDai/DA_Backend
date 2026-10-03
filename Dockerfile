FROM ubuntu:latest
LABEL authors="quocdai"

ENTRYPOINT ["top", "-b"]