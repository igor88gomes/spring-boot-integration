org.springframework.cloud.contract.spec.Contract.make {
    /*
     * Kontrakt: message > 256 tecken ska ge 400 (Bad Request).
     * Säkerställer att storleksbegränsningen i controller fortsätter gälla
     * och att felet returneras i strukturerat JSON-format enligt RFC 7807.
     */
    description 'POST /api/send med message > 256 ⇒ 400 (application/problem+json)'

    request {
        method 'POST'
        url('/api/send') {
            queryParameters {
                parameter 'message': value(consumer('x' * 300), producer('x' * 300)) // 300 > 256
            }
        }
    }

    response {
        status 400
        headers {
            // Byt från generisk JSON till exakt ProblemDetail-typ
            header('Content-Type', 'application/problem+json')
        }
        body(
                status: 400,
                title : value(producer('Valideringsfel'), consumer('Valideringsfel')),
                errors: [
                        [
                                field  : value(producer('message'), consumer('message')),
                                message: value(
                                        producer("Parametern 'message' får vara högst 256 tecken."),
                                        consumer("Parametern 'message' får vara högst 256 tecken.")
                                )
                        ]
                ]
        )
    }
}
