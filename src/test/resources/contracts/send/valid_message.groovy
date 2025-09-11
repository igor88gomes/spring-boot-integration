org.springframework.cloud.contract.spec.Contract.make {
    description 'POST /api/send med giltigt message ⇒ 200 (text/plain)'
    request {
        method 'POST'
        url('/api/send') {
            queryParameters {
                parameter 'message': value(consumer('hello'), producer('hello'))
            }
        }
    }
    response {
        status 200
        headers {
            contentType(textPlain()) // <-- säkrar "Content-Type: text/plain"
        }
    }
}
