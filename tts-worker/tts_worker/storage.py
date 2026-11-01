from google.cloud import storage


class GcsStorage:
    def __init__(self, bucket_name: str):
        self._bucket = storage.Client().bucket(bucket_name)

    def download(self, path: str, dest: str) -> None:
        self._bucket.blob(path).download_to_filename(dest)

    def upload(self, src: str, path: str, content_type: str) -> None:
        self._bucket.blob(path).upload_from_filename(src, content_type=content_type)
