package com.bdc.ai;
import com.bdc.dto.Models.Result;import org.springframework.web.client.RestClient;import java.util.*;
/** Receives only calculated insight statements. Returned text must exactly select an approved statement.
 * This restrictive extraction contract prevents invented financial numbers. Failure uses the deterministic answer. */
public class OpenAIProvider implements AIProvider {
 public String explain(Result r){
  String key=System.getenv("OPENAI_API_KEY");if(key==null||key.isBlank())throw new IllegalStateException("OPENAI_API_KEY is required outside demo mode");
  try{
   var allowed=new ArrayList<>(r.insights());allowed.add(r.answer());
   var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(5000);factory.setReadTimeout(15000);
   var response=RestClient.builder().baseUrl("https://api.openai.com").requestFactory(factory).build().post().uri("/v1/chat/completions").header("Authorization","Bearer "+key).body(Map.of("model",System.getenv().getOrDefault("OPENAI_MODEL","gpt-4.1-mini"),"messages",List.of(Map.of("role","system","content","Select and return exactly one of the provided statements, verbatim. No other text."),Map.of("role","user","content",String.join("\n",allowed))))).retrieve().body(Map.class);
   var choices=(List<Map<String,Object>>)response.get("choices");String text=(String)((Map<?,?>)choices.get(0).get("message")).get("content");return allowed.contains(text)?text:r.answer();
  }catch(Exception e){return r.answer();}
 }
}
