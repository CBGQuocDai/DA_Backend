import logging
import threading
import time

import pika

from .config import Config
from .service import TtsService

log = logging.getLogger(__name__)

EXCHANGE = "tts"
JOB_QUEUE = "tts.jobs"
RESULT_QUEUE = "tts.results"
DEAD_LETTER_EXCHANGE = "tts.dlx"
RESULT_DLQ = "tts.results.dlq"


class RabbitMqConsumer:
    def __init__(self, config: Config, service: TtsService):
        self._config = config
        self._service = service

    def run_forever(self) -> None:
        while True:
            try:
                self._consume()
            except KeyboardInterrupt:
                return
            except pika.exceptions.AMQPError:
                log.exception("Mất kết nối RabbitMQ, thử lại sau 5s")
                time.sleep(5)

    def _consume(self) -> None:
        c = self._config
        conn = pika.BlockingConnection(pika.ConnectionParameters(
            c.rabbitmq_host, c.rabbitmq_port,
            credentials=pika.PlainCredentials(c.rabbitmq_username, c.rabbitmq_password),
            heartbeat=60, blocked_connection_timeout=300))
        ch = conn.channel()
        self._declare_topology(ch)
        ch.basic_qos(prefetch_count=1)

        def finish(tag, result):  # chạy trên thread của connection
            if result is None:
                ch.basic_nack(tag, requeue=False)
                return
            ch.basic_publish(
                EXCHANGE, RESULT_QUEUE, result.to_json(),
                pika.BasicProperties(content_type="application/json",
                                     content_encoding="UTF-8", delivery_mode=2))
            ch.basic_ack(tag)  # ack sau khi đã publish kết quả

        def on_message(_ch, method, _props, body):
            # sinh audio lâu: làm ở thread riêng để connection vẫn gửi heartbeat
            def run():
                result = self._service.handle(body)
                conn.add_callback_threadsafe(lambda: finish(method.delivery_tag, result))
            threading.Thread(target=run, daemon=True).start()

        ch.basic_consume(JOB_QUEUE, on_message)
        log.info("Đang chờ job trên %s", JOB_QUEUE)
        try:
            ch.start_consuming()
        finally:
            if conn.is_open:
                conn.close()

    @staticmethod
    def _declare_topology(ch) -> None:
        """Khai báo đúng tham số như backend (RabbitMqConfig) để không bị PRECONDITION_FAILED."""
        ch.exchange_declare(EXCHANGE, exchange_type="direct", durable=True)
        ch.exchange_declare(DEAD_LETTER_EXCHANGE, exchange_type="direct", durable=True)

        ch.queue_declare(JOB_QUEUE, durable=True)
        ch.queue_bind(JOB_QUEUE, EXCHANGE, JOB_QUEUE)

        ch.queue_declare(RESULT_DLQ, durable=True)
        ch.queue_bind(RESULT_DLQ, DEAD_LETTER_EXCHANGE, RESULT_QUEUE)
        ch.queue_declare(RESULT_QUEUE, durable=True,
                         arguments={"x-dead-letter-exchange": DEAD_LETTER_EXCHANGE})
        ch.queue_bind(RESULT_QUEUE, EXCHANGE, RESULT_QUEUE)
