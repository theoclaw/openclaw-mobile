FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

COPY server.py /app/server.py

ENV LISTEN_HOST=0.0.0.0
ENV LISTEN_PORT=8080

EXPOSE 8080

CMD ["python", "/app/server.py"]

