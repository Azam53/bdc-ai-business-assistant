package com.bdc.search;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.HtmlUtils;
import java.net.URI;
import java.util.*;

@Component
public class HttpWebSearchProvider implements WebSearchProvider {
 private final String provider;
 private final String key;
 private final RestClient client;
 public HttpWebSearchProvider(@Value("${WEB_SEARCH_PROVIDER:wikipedia}") String provider,
                              @Value("${TAVILY_API_KEY:}") String key) {
  this.provider=provider; this.key=key;
  var factory=new SimpleClientHttpRequestFactory();
  factory.setConnectTimeout(5000); factory.setReadTimeout(15000);
  client=RestClient.builder().requestFactory(factory)
      .defaultHeader("User-Agent","BDCBusinessAssistant/1.0 (local educational demo)").build();
 }
 public String name(){return provider.equals("tavily")?"Tavily web search":"Wikipedia search";}
 public List<Source> search(String query){
  if(!Set.of("wikipedia","tavily").contains(provider))
   throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Unknown WEB_SEARCH_PROVIDER; choose wikipedia or tavily.");
  if(provider.equals("tavily")&&key.isBlank())
   throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Set TAVILY_API_KEY on the server, or use WEB_SEARCH_PROVIDER=wikipedia for key-free search.");
  try {
   JsonNode response;
   if(provider.equals("tavily")) {
    response=client.post().uri("https://api.tavily.com/search").header("Authorization","Bearer "+key)
     .body(Map.of("query",query,"search_depth","basic","max_results",5,"include_answer",false,"include_raw_content",false))
     .retrieve().body(JsonNode.class);
   } else {
    String terms=query.replaceFirst("(?i)^(what is|what are|explain|tell me about|search for|search the web for)\\s+","").replaceAll("[?]+$","").trim();
    response=client.get().uri(b->b.scheme("https").host("en.wikipedia.org").path("/w/api.php")
     .queryParam("action","query").queryParam("list","search").queryParam("format","json")
     .queryParam("srsearch","{query}").queryParam("srlimit",5).queryParam("srprop","snippet").build(terms))
     .retrieve().body(JsonNode.class);
   }
   if(response==null||response.has("error"))throw new IllegalStateException("Invalid search response");
   var results=new ArrayList<Source>();
   JsonNode rows=provider.equals("tavily")?response.path("results"):response.path("query").path("search");
   if(!rows.isArray())throw new IllegalStateException("Missing search results");
   for(JsonNode row:rows){
    String url=provider.equals("tavily")?row.path("url").asText():"https://en.wikipedia.org/?curid="+row.path("pageid").asLong();
    if(safeUrl(url))results.add(new Source(clean(row.path("title").asText(),180),url,
      clean(row.path(provider.equals("tavily")?"content":"snippet").asText(),600)));
    if(results.size()==5)break;
   }
   return List.copyOf(results);
  }catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Web search is unavailable or timed out. Try again; your business analytics remains available.");}
 }
 static boolean safeUrl(String url){
  try{URI u=URI.create(url);return Set.of("https","http").contains(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null;}catch(Exception e){return false;}
 }
 static String clean(String text,int limit){String plain=HtmlUtils.htmlUnescape(text.replaceAll("<[^>]*>","")).trim();return plain.length()>limit?plain.substring(0,limit)+"…":plain;}
}
