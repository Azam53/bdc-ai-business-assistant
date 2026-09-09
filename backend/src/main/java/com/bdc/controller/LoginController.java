package com.bdc.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Keep old login bookmarks working after making the demo public. */
@Controller
@Profile("render")
public class LoginController {
 @GetMapping("/login")
 public String login(){return "redirect:/";}
}
