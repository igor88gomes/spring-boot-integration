org.springframework.cloud.contract.spec.Contract.make {
    /*
     * Kontrakt: ogiltiga tecken (t.ex. "<script>") ska ge 400 (Bad Request).
     * Regexen i controller tillåter endast bokstäver (inkl. Å/Ä/Ö), siffror,
     * blanksteg och symbolerna - _ . : , ! ? — men inte "<" eller ">".
     * Säkerställer att valideringen av förbjudna tecken fungerar och att svaret
     * returneras i formatet application/problem+json (RFC 7807).
     */
    description 'POST /api/send med ogiltiga tecken ⇒ 400 (application/problem+json)'

    request {
        method 'POST'
        url('/api/send') {
            queryParameters {
                parameter 'message': value(consumer('<script>'), producer('<script>'))
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
                        [ field: 'message',
                          // Aceite as duas variantes comuns de texto em sueco via matcher abaixo
                          message: 'Otillåtna tecken i \'message\'. Tillåtna: bokstäver, siffror, blanksteg samt - _ . : , ! ?' ]
                ]
        )
        bodyMatchers {
            jsonPath('$.errors[0].field', byEqualTo('message'))
            // casa tanto "Otillåtna tecken ..." quanto "Endast bokstäver (A–Ö) ..."
            jsonPath('$.errors[0].message',
                    byRegex('(Otillåtna tecken.*)|(Endast bokstäver .* mellanslag.*)'))
        }
    }
}
