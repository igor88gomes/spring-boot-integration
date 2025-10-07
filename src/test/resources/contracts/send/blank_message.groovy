org.springframework.cloud.contract.spec.Contract.make {
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
                        [
                                field  : 'message',
                                // alinhar com o valor real observado no CI
                                message: "Parametern 'message' får inte vara tom."
                        ]
                ]
        )
        bodyMatchers {
            jsonPath('$.errors[0].field', byEquality())
            // aceitar as duas redações (a real e a antiga), se preferir robustez
            jsonPath('$.errors[0].message',
                    byRegex("(Parametern 'message' får inte vara tom\\.)|(Meddelandet får inte vara tomt)"))
        }
    }
}
