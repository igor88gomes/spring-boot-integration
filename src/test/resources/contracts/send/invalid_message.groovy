org.springframework.cloud.contract.spec.Contract.make {
    description 'POST /api/send med tom/blank message ⇒ 400'
    request {
        method 'POST'
        // blankt meddelande (form/query)
        url('/api/send') {
            queryParameters {
                parameter 'message': value(consumer(' '), producer(' '))
            }
        }
    }
    response {
        status 400
    }
}
