package com.bdc.service;
import com.bdc.analytics.*;import com.bdc.ai.*;import com.bdc.provider.*;import com.bdc.dto.Models.*;import com.bdc.entity.*;import com.bdc.repository.*;import com.fasterxml.jackson.databind.ObjectMapper;import org.springframework.stereotype.Service;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;
import java.time.*;import java.util.*;import java.math.*;
@Service public class ChatService {
 private final AnalyticsEngine engine;private final AIProvider ai;private final BusinessDataProvider data;private final ConversationRepository conversations;private final InteractionRepository audit;private final ObjectMapper json;
 public ChatService(AnalyticsEngine e,AIProvider a,BusinessDataProvider d,ConversationRepository c,InteractionRepository i,ObjectMapper j){engine=e;ai=a;data=d;conversations=c;audit=i;json=j;}
 public record Context(Filter filter,List<String> countries,String intent){}
 public Result ask(ChatRequest request)throws Exception{
  if(request.question()==null||request.question().isBlank()||request.question().length()>2000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Question must contain 1–2000 characters");
  Conversation conversation=request.conversationId()==null?new Conversation(UUID.randomUUID().toString(),request.question().substring(0,Math.min(70,request.question().length()))):conversations.findById(request.conversationId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Conversation not found"));
  String q=request.question().toLowerCase(Locale.ROOT);Context previous=conversation.context==null?null:json.readValue(conversation.context,Context.class);
  boolean follow=q.matches(".*(why|growing faster|what about|explain|that|those).*" )&&previous!=null;
  Filter f=follow?previous.filter():engine.defaults();LocalDate end=engine.end();LocalDate quarter=end.withMonth(((end.getMonthValue()-1)/3)*3+1).withDayOfMonth(1);
  var matcher=java.util.regex.Pattern.compile("last (\\d+) months?").matcher(q);
  if(matcher.find()){int months=Integer.parseInt(matcher.group(1));if(months<1||months>24)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose between 1 and 24 months");f=new Filter(end.minusMonths(months-1).withDayOfMonth(1),end,null,null,null,null,null);}
  else if(q.contains("last month"))f=new Filter(end.withDayOfMonth(1),end,null,null,null,null,null);
  else if(q.contains("last quarter"))f=new Filter(quarter.minusMonths(3),quarter.minusDays(1),null,null,null,null,null);
  else if(q.contains("q2"))f=new Filter(LocalDate.of(end.getYear(),4,1),LocalDate.of(end.getYear(),6,30),null,null,null,null,null);
  else if(q.contains("quarter")||q.contains("declining")||q.contains("dropped"))f=new Filter(quarter,end,null,null,null,null,null);
  List<String> countries=new ArrayList<>();for(String c:List.of("India","Germany","USA","UAE","UK"))if(java.util.regex.Pattern.compile("\\b"+c.toLowerCase()+"\\b").matcher(q).find())countries.add(c);
  if(follow&&countries.size()<2&&previous.countries().size()>1)countries=previous.countries();
  String intent;
  if(q.matches("(?s).*\\b(drop|delete|insert|update|select|alter|truncate)\\s+(table|from|into|sales|customers|products|\\*)\\b.*"))intent="UNSUPPORTED";
  else if(countries.size()>1)intent="COMPARE_COUNTRY_REVENUE";
  else if(q.contains("customer")&&(q.contains("drop")||q.contains("reduc")||q.contains("trend")))intent="CUSTOMER_ORDER_TREND";
  else if(q.contains("customer"))intent="TOP_CUSTOMERS";
  else if(q.contains("declin")&&q.contains("product"))intent="DECLINING_PRODUCTS";
  else if(q.contains("product"))intent="TOP_PRODUCTS";
  else if(q.contains("why")||q.contains("explain")||q.contains("summary")||q.contains("change"))intent="COMPARE_PERIODS";
  else if(q.contains("margin")||q.contains("profit"))intent="PROFIT_MARGIN";
  else if(q.contains("region"))intent="REVENUE_BY_REGION";
  else if(q.contains("country")||q.contains("countries"))intent="REVENUE_BY_COUNTRY";
  else if(q.contains("revenue")||q.contains("sales")||q.contains("chart"))intent="REVENUE_BY_PERIOD";
  else intent="UNSUPPORTED";
  if(countries.size()==1)f=new Filter(f.from(),f.to(),countries.get(0),null,null,null,null);
  var log=new Interaction();log.question=request.question();log.conversationId=conversation.id;log.intent=intent;log.dataSource=data.source();
  try{
   List<Row> rows;String function;
   switch(intent){
    case "COMPARE_COUNTRY_REVENUE" -> {rows=engine.compareCountries(f,countries);function="compareCountries";}
    case "CUSTOMER_ORDER_TREND" -> {rows=engine.getCustomerOrderTrend(f);function="getCustomerOrderTrend";}
    case "TOP_CUSTOMERS" -> {rows=engine.getTopCustomers(f);function="getTopCustomers";}
    case "DECLINING_PRODUCTS" -> {rows=engine.getDecliningProducts(f);function="getDecliningProducts";}
    case "TOP_PRODUCTS" -> {rows=engine.getTopProducts(f);function="getTopProducts";}
    case "COMPARE_PERIODS" -> {rows=engine.comparePeriods(f);function="comparePeriods";}
    case "REVENUE_BY_COUNTRY" -> {rows=engine.getRevenueByCountry(f);function="getRevenueByCountry";}
    case "REVENUE_BY_REGION" -> {rows=engine.getRevenueByRegion(f);function="getRevenueByRegion";}
    case "PROFIT_MARGIN" -> {rows=engine.getRevenueByPeriod(f);function="getProfitMargin";}
    case "UNSUPPORTED" -> {rows=List.of();function="none";}
    default -> {rows=engine.getRevenueByPeriod(f);function="getRevenueByPeriod";}
   }
   log.functionName=function;var metrics=new LinkedHashMap<String,BigDecimal>();var insights=new ArrayList<String>();String answer;
   if(intent.equals("UNSUPPORTED"))answer="I can analyze revenue, country comparisons, top customers, declining products, order trends and margins. Try one of the suggested questions.";
   else if(intent.equals("COMPARE_COUNTRY_REVENUE")){for(Row r:rows){metrics.put(r.label()+" revenue",r.revenue());insights.add(r.label()+" revenue is $"+r.revenue()+"; change versus prior period: "+(r.change()==null?"unavailable":r.change()+"%")+".");}answer="Here is the country comparison for "+f.from()+" to "+f.to()+". "+String.join(" ",insights);if(follow){
    for(String country:countries){Filter cf=new Filter(f.from(),f.to(),country,null,null,null,null);var now=engine.group(cf,Sale::businessUnit);var before=engine.group(engine.previous(cf),Sale::businessUnit);for(Row unit:now){BigDecimal prior=before.stream().filter(b->b.label().equals(unit.label())).map(Row::revenue).findFirst().orElse(BigDecimal.ZERO);insights.add(country+"  -  "+unit.label()+" contributed $"+unit.revenue().subtract(prior)+" to the revenue change.");}}
    answer+=" The business-unit contributions below explain where the change occurred. These observed changes do not establish the underlying business causes.";
   }}
   else{metrics.putAll(engine.getProfitMargin(f));insights.addAll(engine.insights(f));answer=switch(intent){case "DECLINING_PRODUCTS"->rows.size()+" products had lower revenue than the preceding equal-length period.";case "CUSTOMER_ORDER_TREND"->rows.size()+" customers reduced order count by more than 20% versus the preceding period. Change percentages in the table refer to orders.";case "TOP_CUSTOMERS"->"These are the top "+rows.size()+" customers ranked by revenue.";case "COMPARE_PERIODS"->insights.get(0)+" The country breakdown identifies contributions; it does not establish business causes.";default->"Revenue totals $"+metrics.get("revenue")+" across "+metrics.get("orders")+" orders, with a "+metrics.get("profitMargin")+"% gross margin.";};if(intent.equals("COMPARE_PERIODS")){for(Row r:engine.compareCountries(f,List.of()))insights.add(r.label()+" contributed $"+r.revenue().subtract(r.previous())+" to the revenue change.");}}
   Result result=new Result(answer,metrics,rows,engine.chart(rows,intent.equals("REVENUE_BY_PERIOD")?"line":"bar"),insights,data.source(),intent,function,f.from()+" → "+f.to(),conversation.id);
   result=new Result(ai.explain(result),metrics,rows,result.chart(),insights,data.source(),intent,function,result.period(),conversation.id);
   conversation.context=json.writeValueAsString(new Context(f,countries,intent));conversations.save(conversation);log.status="SUCCESS";log.response=json.writeValueAsString(result);audit.save(log);return result;
  }catch(Exception e){log.status="ERROR";audit.save(log);throw e;}
 }
}
