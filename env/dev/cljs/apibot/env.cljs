(ns apibot.env)

;; ---- DEV ENV  ----

(def env :dev)
(def title "apibot | development")

(def auth0-client-id "s1cFxP0SBD4XUuW60PsDNJhfncFKanUU")
(def auth0-domain "picnictest.eu.auth0.com")
(def auth0-redirect-uri "http://localhost:3000/index.html")
(def auth0-audience "https://api-dev.apibot.co")

(def sentry-dsn "https://d2ccc1a99cd94154a219f663c75cf2c9:031f0f6f228945f280d08a73ef0768a5@sentry.io/192134")

(def mixpanel-token "db33c4c883e125a0f05c0e13932cac7c")

(def apibot-root "http://localhost:3000/api/1") ;(def apibot-root "https://api-dev.apibot.co/api/1")
