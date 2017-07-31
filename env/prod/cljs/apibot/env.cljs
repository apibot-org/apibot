(ns apibot.env)

;; ---- PROD ENV ----

(def env :prod)
(def title "apibot | write integration tests that rock! ")

(def auth0-client-id "Yi2Vms52QUMf1Y5CL60JL1293vyes8y2")
(def auth0-domain "picnictest.eu.auth0.com")
(def auth0-redirect-uri "https://picnictest.eu.auth0.com/mobile")
(def auth0-audience "https://api.apibot.co")

(def sentry-dsn "https://d2ccc1a99cd94154a219f663c75cf2c9:031f0f6f228945f280d08a73ef0768a5@sentry.io/192134")

(def apibot-root "https://api.apibot.co/api/1")
