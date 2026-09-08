package com.bdc.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.util.HtmlUtils;

@RestController
@Profile("render")
public class LoginController {
 @GetMapping(value="/login",produces="text/html")
 public String login(CsrfToken csrf,@RequestParam(required=false) String error){
  String message=error==null?"":"<p role='alert' class='error'>The username or password was incorrect. Please try again.</p>";
  return """
   <!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Sign in | BDC AI Business Assistant</title>
   <style>*{box-sizing:border-box}body{margin:0;min-height:100vh;display:grid;place-items:center;padding:24px;background:#122b42;font:15px system-ui,sans-serif;color:#263d50}.card{width:100%%;max-width:430px;background:#fff;border-radius:16px;padding:36px;box-shadow:0 20px 70px #0003}.brand{color:#138b80;font-weight:800;font-size:36px;letter-spacing:-2px}.badge{font-size:10px;color:#987322;letter-spacing:1px;margin:16px 0}h1{font-size:24px;line-height:1.3;margin:14px 0}p{color:#6b8190;font-size:13px;line-height:1.6}label{display:block;font-size:13px;font-weight:600;margin:20px 0 7px}input{width:100%%;padding:12px;border:1px solid #ccd9df;border-radius:7px;font:inherit}input:focus{outline:2px solid #64b9ad;outline-offset:2px}button{width:100%%;background:#138b80;color:white;border:0;border-radius:7px;padding:13px;font:inherit;font-weight:600;margin-top:25px;cursor:pointer}.error{color:#ae3a3a;background:#fff1ef;padding:10px;border-radius:5px}.note{font-size:11px;margin-top:24px}</style></head>
   <body><main class="card"><div class="brand">bdc</div><div class="badge">DEMO ENVIRONMENT</div><h1>BDC AI Business Assistant</h1><p>Sign in to explore your business data.</p>%s
   <form method="post" action="/login"><input type="hidden" name="%s" value="%s"><label for="username">Username</label><input id="username" name="username" autocomplete="username" required autofocus><label for="password">Password</label><input id="password" name="password" type="password" autocomplete="current-password" required><button type="submit">Sign in</button></form>
   <p class="note">Portfolio demo using simulated enterprise data. No live SAP system is connected.</p></main></body></html>
   """.formatted(message,HtmlUtils.htmlEscape(csrf.getParameterName()),HtmlUtils.htmlEscape(csrf.getToken()));
 }
}
