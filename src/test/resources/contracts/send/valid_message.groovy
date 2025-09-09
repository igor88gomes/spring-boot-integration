org.springframework.cloud.contract.spec.Contract.make {
    description 'POST /api/send med giltigt message ⇒ 200'
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
    }
}
