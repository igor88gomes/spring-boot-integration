org.springframework.cloud.contract.spec.Contract.make {
    /*
     * Kontrakt: tomt eller blankt 'message' ska ge 400 (Bad Request).
     * Säkerställer att valideringen av obligatorisk parameter fungerar korrekt
     * och att felet returneras i formatet application/problem+json (RFC 7807).
     */
    description 'POST /api/send med tomt eller blankt message ⇒ 400 (application/problem+json)'

    request {
        method 'POST'
        url('/api/send') {
            queryParameters {
                parameter 'message': value(consumer('   '), producer('   '))
            }
        }
    }

    response {
        status 400
        headers {
            // OBS: Ange exakt ProblemDetail-typ; undvik regex / consumer() i response
            header('Content-Type', 'application/problem+json')
        }
        body(
                // Använd konkreta värden i response (inga consumer()/regex här)
                status: 400,
                title: 'Valideringsfel',
                errors: [
                        [
                                field: 'message',
                                message: 'Meddelandet får inte vara tomt'
                        ]
                ]
        )
    }
}
