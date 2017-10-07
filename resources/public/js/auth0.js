window.apibot_signup = function() {
  var auth = new auth0.WebAuth({
    domain: "picnictest.eu.auth0.com",
    clientID: "Yi2Vms52QUMf1Y5CL60JL1293vyes8y2",
    redirectUri: "https://www.apibot.co/app",
    audience: "https://api.apibot.co",
    responseType: 'token id_token',
    scope: 'openid profile email'
  }).authorize();
}