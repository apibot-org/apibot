(ns apibot.env)

;; ---- PROD ENV ----

(def env :prod)
(def title "apibot | write integration tests that rock! ")

(def auth0-client-id "Yi2Vms52QUMf1Y5CL60JL1293vyes8y2")
(def auth0-domain "picnictest.eu.auth0.com")
(def auth0-redirect-uri "https://www.apibot.co/app")
(def auth0-audience "https://api.apibot.co")

(def sentry-dsn "https://d2ccc1a99cd94154a219f663c75cf2c9@sentry.io/192134")

(def mixpanel-token "ea8d97c8fc33ca1ff94c73175d8d5010")

(def apibot-root "https://www.apibot.co/api/1")
