(ns apibot.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer [request header]]
            [apibot.handler :refer [app]]))

(def token
  "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik1USkNRelU0UmtNeVFqZ3lRVFZHTURVeE5qUXdSVGd6TVVaQ09FWXpNelZDUWpnMk0wRXdNUSJ9.eyJpc3MiOiJodHRwczovL3BpY25pY3Rlc3QuZXUuYXV0aDAuY29tLyIsInN1YiI6IkhjaUF0UW5RR245QXZBMUozQzc3aVNBUmZWdVdONHF2QGNsaWVudHMiLCJhdWQiOiJodHRwczovL2FwaS1kZXYuYXBpYm90LmNvIiwiZXhwIjoxNDk4Njg1NzM4LCJpYXQiOjE0OTg1OTkzMzgsInNjb3BlIjoiIn0.zLQ7nuvtyZPfjozxuhuaD7dNSpTr1jgXgknYwLA-fEzteFKsyVSxoOHaar95kiuQ96gJTj_NUgis_mwIZgszzhfGcC8RxfG45Zfx9_rWXiZ7OPodfWvyYjT8UuTQ0IGIca46z6cueA8-AqFAzCdvl230_jCK3tfpoMTM0tbUHVgcCbJU1ORHPklLi6C3UaiALbyZWEC-_UaheoifhE1xC_xzkctgu9TcG6idH5vUuMoSF_qJaYTzJqJGnDQERS1FkCTVEEdbjXfBg4ok7rZ6bpzR6XZ_4zwwYZVlYNwp6U_-LU9dSTm67Ws5O0LolrktDlnQ2KXiEsCbwY1LJkcGsg")

(defn auth-request [& args]
  (-> (apply request args)
      (header "x-apibot-auth" token)))

(deftest token-verification
  (testing "verifying a correct token"
    (let [verifier (apibot.auth/create-verifier)]
      (is (some? (.verify verifier token))))))

(deftest test-app
  (testing "not-found route"
    (let [response ((app) (auth-request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "token validation - when token not present"
    (let [response ((app) (request :get "/api/1/users/me/graphs"))]
      (is (= 403 (:status response))))))
