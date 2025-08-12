FROM python:3.12-slim

WORKDIR /app

COPY dependencies/ /app/dependencies
COPY app.py /app/app.py

RUN pip install $(ls dependencies/kta_python_sdk-*-py3-none-any.whl)[http-backend]

CMD ["python", "app.py"]
