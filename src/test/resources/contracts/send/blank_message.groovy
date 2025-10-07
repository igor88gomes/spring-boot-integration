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
                title: 'Valideringsfel',
                errors: [
                        [ field: 'message', message: 'Meddelandet får inte vara tomt' ]
                ]
        )
        bodyMatchers {
            jsonPath('$.errors[0].field', byEqualTo('message'))
            jsonPath('$.errors[0].message', byEqualTo('Meddelandet får inte vara tomt'))
        }
    }
}
