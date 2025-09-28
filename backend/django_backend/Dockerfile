FROM python:3.13
RUN pip install --upgrade pip 
RUN apt update && apt install -y pipx && pipx install poetry
ENV PATH="/root/.local/bin:$PATH"
RUN poetry --version
RUN mkdir /app
WORKDIR /app
COPY poetry.lock pyproject.toml .env ./
