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
            headers {
                header 'Accept-Language': value(consumer('sv-SE'), producer('sv-SE'))
            }
        }
    }

    response {
        status 400
        headers {
            header('Content-Type', 'application/problem+json')
        }
        body(
                status: 400,
                title : 'Valideringsfel',
                errors: [
                        [
                                field  : 'message',
                                // Exempeltext – behåll tydlig svensk formulering
                                message: "Parametern 'message' får vara högst 256 tecken."
                        ]
                ]
        )
        // Säkerställ att felet gäller fältet och att texten refererar till 256
        matchers {
            jsonPath('$.errors[0].field', byEqualTo('message'))
            jsonPath('$.errors[0].message', byRegex('.*256.*'))
        }
    }
}
