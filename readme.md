# Spring Kotlin ActiveMq
Apache ActiveMQ, tam bir Java Mesaj Hizmeti istemcisi ile birlikte Java'da yazılmış açık kaynaklı bir mesaj aracısıdır.

## Setup
```ssh
docker-compose up -d
```

## Test
Make a connection with server using sse.

```http request
GET http://localhost:8080/sse
```

Send following request many times.

```http request
GET http://localhost:8080/jms
```

You should see some messages coming from the socket

```
data:413dea94-a582-40f2-9f8e-9de5659c2241

data:c442f95f-ba6b-4994-9feb-aade6a1350f5

data:f1377bed-780c-43e8-90bd-a5470c744960
```


check the logs out and should be something like;
```
: Sending message to Topic: testtopic, randomUUID: 9cd007dd-9b88-4c38-90d3-a7e7baee9430
: Received ActiveMQTextMessage from Topic: 9cd007dd-9b88-4c38-90d3-a7e7baee9430
```

## Ref
https://docs.spring.io/spring-boot/docs/2.0.x/reference/html/boot-features-messaging.html