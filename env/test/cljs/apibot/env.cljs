(ns apibot.env)

;; ---- TEST ENV  ----

(def env :test)
(def title "apibot | test environment ")

(def auth0-client-id "s1cFxP0SBD4XUuW60PsDNJhfncFKanUU")
(def auth0-domain "picnictest.eu.auth0.com")
(def auth0-redirect-uri "https://picnictest.eu.auth0.com/mobile")
(def auth0-audience "https://api-dev.apibot.co")

(def sentry-dsn "https://d91c1afa1f614562b24d2a1fed3c0769@sentry.io/186706")

(def mixpanel-token "db33c4c883e125a0f05c0e13932cac7c")

(def apibot-root "https://api-dev.apibot.co/api/1")
