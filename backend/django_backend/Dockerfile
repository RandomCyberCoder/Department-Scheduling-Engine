FROM python:3.13
RUN pip install --upgrade pip 
RUN apt update && apt install -y pipx && pipx install poetry
ENV PATH="/root/.local/bin:$PATH"
RUN poetry --version
RUN mkdir -p /django_backend/scheduling_backend
WORKDIR /django_backend
COPY pyproject.toml poetry.lock  /django_backend/
RUN poetry install --no-root
CMD ["sleep", "infinity"]
