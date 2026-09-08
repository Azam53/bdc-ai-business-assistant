package com.bdc.service;

import com.bdc.search.WebSearchProvider;
import com.bdc.dto.Models.*;
import com.bdc.entity.*;
import com.bdc.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
public class WebChatService {
 private final WebSearchProvider search;
 private final ConversationRepository conversations;
 private final InteractionRepository audit;
 private final ObjectMapper json;
 public WebChatService(WebSearchProvider s,ConversationRepository c,InteractionRepository a,ObjectMapper j){search=s;conversations=c;audit=a;json=j;}
 public Map<String,Object> ask(ChatRequest request)throws Exception{
  if(request.question()==null||request.question().isBlank()||request.question().length()>500)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Web search requires 1–500 characters.");
  var conversation=request.conversationId()==null?new Conversation(UUID.randomUUID().toString(),request.question().substring(0,Math.min(70,request.question().length()))):conversations.findById(request.conversationId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Conversation not found"));
  // Send only the explicit search query: never conversation history, metrics or enterprise records.
  var log=new Interaction();log.question=request.question();log.conversationId=conversation.id;
  log.intent="WEB_SEARCH";log.functionName="webSearch";log.dataSource=search.name();
  conversations.save(conversation);
  try{
   var sources=search.search(request.question());
   var response=new LinkedHashMap<String,Object>();
   response.put("answer",sources.isEmpty()?"No sources matched this search. Try a shorter topic or different keywords.":"Found "+sources.size()+" sources from "+search.name()+". Read the excerpts below and open a source for details.");
   response.put("metrics",Map.of());response.put("table",List.of());response.put("chart",new Chart("bar",List.of(),List.of()));
   response.put("insights",List.of());response.put("dataSource",search.name());response.put("intent","WEB_SEARCH");
   response.put("function","webSearch");response.put("period","Retrieved "+Instant.now());response.put("conversationId",conversation.id);
   response.put("sources",sources);
   response.put("sourceNote",search.name().equals("Wikipedia search")?"Live Wikipedia search for background knowledge; not a general web or news search. Excerpts are external reference material and do not explain changes in your simulated business data.":"Live web search excerpts, not a verified AI synthesis. Sources may be incomplete or outdated and do not establish causes of changes in your business data.");
   log.status=sources.isEmpty()?"NO_RESULTS":"SUCCESS";log.response=json.writeValueAsString(response);audit.save(log);return response;
  }catch(Exception e){log.status="ERROR";audit.save(log);throw e;}
 }
 public String provider(){return search.name();}
}
