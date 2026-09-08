package com.bdc.controller;
import com.bdc.analytics.*;import com.bdc.service.*;import com.bdc.repository.*;import com.bdc.dto.Models.*;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.web.server.ResponseStatusException;import java.time.*;import java.util.*;
@RestController @RequestMapping("/api") public class ApiController {
 @org.springframework.beans.factory.annotation.Value("${bdc.demo}") private boolean demo;
 @org.springframework.beans.factory.annotation.Value("${bdc.ai}") private String aiProvider;
 @org.springframework.beans.factory.annotation.Value("${bdc.provider}") private String dataProvider;
 private final WebChatService web;
 private final AnalyticsEngine engine;private final ChatService chat;private final ConversationRepository conversations;private final InteractionRepository audit;
 public ApiController(AnalyticsEngine e,ChatService c,ConversationRepository r,InteractionRepository a,WebChatService w){web=w;engine=e;chat=c;conversations=r;audit=a;}
 @GetMapping("/settings") public Object settings(){return Map.of("demo",demo,"aiProvider",demo?"mock":aiProvider,"dataProvider",demo?"mock":dataProvider,"webSearchProvider",web.provider());}
 @GetMapping("/csrf") public Object csrf(org.springframework.security.web.csrf.CsrfToken token){return token==null?Map.of():Map.of("headerName",token.getHeaderName(),"token",token.getToken());}
 @GetMapping("/health") public Object health(){return Map.of("status","UP");}
 @GetMapping("/catalog") public Object catalog(){return engine.catalog();}
 @PostMapping("/web-chat") public Object webChat(@RequestBody ChatRequest request)throws Exception{return web.ask(request);}
 @PostMapping("/chat") public Result chat(@RequestBody ChatRequest request)throws Exception{return chat.ask(request);}
 @GetMapping("/dashboard") public Object dashboard(@RequestParam Map<String,String> p){Filter d=engine.defaults();LocalDate from=LocalDate.parse(p.getOrDefault("from",d.from().toString())),to=LocalDate.parse(p.getOrDefault("to",d.to().toString()));if(from.isAfter(to))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Start date must precede end date");return engine.dashboard(new Filter(from,to,p.get("country"),p.get("region"),p.get("customer"),p.get("product"),p.get("businessUnit")));}
 @GetMapping("/conversations") public Object conversations(){return conversations.findAllByOrderByCreatedDesc();}
 @GetMapping("/conversations/{id}") public Object history(@PathVariable String id){return audit.findByConversationIdOrderByIdAsc(id);}
 @GetMapping("/audit") public Object audit(){return audit.findTop200ByOrderByIdDesc();}
 @GetMapping(value="/export",produces="text/csv") public ResponseEntity<String> export(){var rows=engine.getRevenueByCountry(engine.defaults());var csv=new StringBuilder("Country,Revenue USD,Cost USD,Orders\n");rows.forEach(r->csv.append(r.label()).append(',').append(r.revenue()).append(',').append(r.cost()).append(',').append(r.orders()).append('\n'));return ResponseEntity.ok().header("Content-Disposition","attachment; filename=bdc-country-revenue.csv").body(csv.toString());}
}
