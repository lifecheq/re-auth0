# re-auth0

[![Clojars Project](https://img.shields.io/clojars/v/lifecheq/re-auth0.svg)](https://clojars.org/lifecheq/re-auth0)

[re-frame](https://github.com/Day8/re-frame) effects for [Auth0](https://auth0.com/docs/libraries).

## Usage

This sticks relatively close to the structure of [Auth0.js v9](https://github.com/auth0/auth0.js),
which this wraps. See the [official docs](https://auth0.com/docs/libraries/auth0js/v9) for reference.

Example

```clj
(ns my.app
  (:require [lifecheq/re-auth0.core :as re-auth0]
            ...))

...

(re-frame/reg-event-fx
 :init-web-auth
 (fn [_ _]
   {::re-auth0/init {:client-id config/auth0-client-id
                     :domain    config/auth0-domain}})

)

(re-frame/reg-event-fx
 :login
 (fn [_ _]
   {::re-auth0/authorize {:response-type    "token id_token"
                          :scope            "email app_metadata"
                          :redirect-uri     "https://some.url"
                          :on-authenticated [:new-auth-result]
                          :on-error         [:auth-error]})})
```

etc.

Significant differences:

- Use `kebab-case` for parameters in the `options` map, instead of `CamelCase`
used in Auth0.js.
- The Auth0 calls accept a single callback function, which has the signature
`function(err, authResult)`. This library specifies two callback vectors, which
are given as part of the `options` map.

## License

Copyright © 2018 LifeCheq

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
