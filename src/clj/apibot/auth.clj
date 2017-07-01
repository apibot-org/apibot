(ns apibot.auth
  (:require
    [apibot.config :refer [env]])
  (:import
    [com.auth0.jwt.algorithms Algorithm]
    [com.auth0.jwt JWT JWTVerifier]
    [com.auth0.jwt.interfaces RSAKeyProvider]
    [com.auth0.jwk UrlJwkProvider Jwk]
    [java.security.interfaces RSAPublicKey]))


(defn ^RSAKeyProvider jwt-key-provider
  []
  (let [provider (new UrlJwkProvider "https://picnictest.eu.auth0.com/")]
    (reify RSAKeyProvider
      (getPrivateKey [this] nil)
      (getPrivateKeyId [this] nil)
      (getPublicKeyById [this key-id]
        (let [^Jwk jwk (.get provider key-id)
              ^RSAPublicKey pubKey (.getPublicKey jwk)]
          pubKey)))))

(defn ^JWTVerifier create-verifier
  "Create a new JWTVerifier which can be used to verify tokens
  by calling (.verify verifier).

  Throws JWTVerificationException if verification fails."
  ([^RSAKeyProvider this]
   (-> this
       (Algorithm/RSA256)
       (JWT/require)
       (.withIssuer "https://picnictest.eu.auth0.com/")
       (.withAudience (into-array String [(:auth0-audience env)]))
       (.build)))
  ([]
   (-> (jwt-key-provider) create-verifier)))
