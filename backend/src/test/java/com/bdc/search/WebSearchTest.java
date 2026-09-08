package com.bdc.search;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.bdc.service.WebChatService;
import com.bdc.repository.*;
import com.bdc.entity.*;
import com.bdc.dto.Models.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
class WebSearchTest {
 @Test void safeSources(){assertTrue(HttpWebSearchProvider.safeUrl("https://example.com/article"));assertFalse(HttpWebSearchProvider.safeUrl("javascript:alert(1)"));assertFalse(HttpWebSearchProvider.safeUrl("https://user:pass@example.com"));assertEquals("Sales & cost",HttpWebSearchProvider.clean("<b>Sales</b> &amp; cost",100));}
 @Test void searchPersistsWithoutChangingBusinessContext()throws Exception{
  var search=mock(WebSearchProvider.class);var conv=mock(ConversationRepository.class);var audit=mock(InteractionRepository.class);
  var c=new Conversation("existing","Business analysis");c.context="private business context";when(conv.findById("existing")).thenReturn(Optional.of(c));
  when(search.name()).thenReturn("Test search");when(search.search("public topic")).thenReturn(List.of(new WebSearchProvider.Source("Reference","https://example.com","Excerpt")));
  var service=new WebChatService(search,conv,audit,new ObjectMapper());var result=service.ask(new ChatRequest("public topic","existing"));
  assertEquals("WEB_SEARCH",result.get("intent"));assertEquals(Map.of(),result.get("metrics"));assertEquals("private business context",c.context);
  verify(search).search("public topic");var captured=ArgumentCaptor.forClass(Interaction.class);verify(audit).save(captured.capture());assertEquals("SUCCESS",captured.getValue().status);assertTrue(captured.getValue().response.contains("https://example.com"));
 }
 @Test void emptyAndFailuresAreAudited()throws Exception{
  var search=mock(WebSearchProvider.class);var conv=mock(ConversationRepository.class);var audit=mock(InteractionRepository.class);when(search.name()).thenReturn("Test search");when(search.search("none")).thenReturn(List.of());
  var service=new WebChatService(search,conv,audit,new ObjectMapper());assertTrue(service.ask(new ChatRequest("none",null)).get("answer").toString().startsWith("No sources"));
  when(search.search("failure")).thenThrow(new IllegalStateException());assertThrows(IllegalStateException.class,()->service.ask(new ChatRequest("failure",null)));
  var captured=ArgumentCaptor.forClass(Interaction.class);verify(audit,times(2)).save(captured.capture());assertEquals("NO_RESULTS",captured.getAllValues().get(0).status);assertEquals("ERROR",captured.getAllValues().get(1).status);
  assertThrows(ResponseStatusException.class,()->service.ask(new ChatRequest("",null)));verify(search,never()).search("");
 }
}
