FROM python:3.12-slim
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends libpq-dev gcc \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
RUN mkdir -p /app/storage/dicom
ENV PYTHONPATH=/app
EXPOSE 8010
CMD ["uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "8010"]
