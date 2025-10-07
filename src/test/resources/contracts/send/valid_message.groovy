org.springframework.cloud.contract.spec.Contract.make {
    /*
     * Kontrakt: giltigt 'message' ska accepteras och ge 200 (OK).
     * Säkerställer att valideringen passerar och att svaret returneras
     * i klartextformat (text/plain), vilket är avsett för lyckade POST-anrop.
     */
    description 'POST /api/send med giltigt message ⇒ 200 (text/plain)'

    request {
        method 'POST'
        url('/api/send') {
            queryParameters {
                // Exempelvärde för lyckad sändning.
                // (Om du vill tillåta "vilket som helst icke-tomt" i request, kan du byta consumer('hello') till consumer(regex('.+')))
                parameter 'message': value(consumer('hello'), producer('hello'))
            }
        }
    }

    response {
        status 200
        headers {
            contentType(textPlain()) // Säkrar att svaret är text/plain (lyckad sändning)
        }
        body('Meddelande skickat till kön: hello')
    }
}
