package com.bdc.provider;
import com.bdc.dto.Models.*;
import java.time.*;import java.util.*;import java.math.*;
public class MockBusinessDataProvider implements BusinessDataProvider {
 private final DataSet data;
 public MockBusinessDataProvider(){
  Random rng=new Random(42);var customers=new ArrayList<Customer>();var products=new ArrayList<Product>();var orders=new ArrayList<SalesOrder>();var items=new ArrayList<Sale>();var invoices=new ArrayList<Invoice>();
  String[] countries={"India","Germany","USA","UAE","UK"},regions={"Asia Pacific","Europe","North America","Middle East","Europe"},units={"Cloud & Software","Industrial Systems","Business Services"};
  String[] names={"Vertex","Atlas","Northstar","Meridian","Apex","Nexus","Horizon","Cobalt","Orion","Summit"};
  for(int i=0;i<100;i++)customers.add(new Customer("CUST-%04d".formatted(i+1),names[i%10]+" "+countries[i%5]+" "+(i/10+1),countries[i%5],regions[i%5]));
  for(int i=0;i<50;i++)products.add(new Product("PRD-"+(101+i),new String[]{"Cloud Analytics","Precision Drive","Managed Operations","Data Platform","Smart Sensor"}[i%5]+" "+(i+1),units[i%3]));
  LocalDate start=LocalDate.of(2024,9,1);int seq=0;
  for(int m=0;m<24;m++)for(Customer c:customers){
   int count=(m>=21 && Integer.parseInt(c.id().substring(5))%7==0)?1:3;
   for(int n=0;n<count;n++){
    Product p=products.get(rng.nextInt(50));int idx=Integer.parseInt(p.id().substring(4))-101;
    double growth=(c.country().equals("India")||c.country().equals("UAE"))?1+m*.035:1+m*.008;
    double trend=(idx<7)?Math.max(.25,1-m*.028):1;
    double seasonal=1+.13*Math.sin(m*Math.PI/6);if(m==23)seasonal*=.78;
    int qty=Math.max(1,(int)((5+rng.nextInt(20))*growth*trend*seasonal));
    BigDecimal revenue=BigDecimal.valueOf(qty*(180+idx*37)*(0.92+rng.nextDouble()*.16)).setScale(2,RoundingMode.HALF_UP);
    BigDecimal cost=revenue.multiply(BigDecimal.valueOf(.52+(idx%5)*.055+(c.country().equals("Germany")?.05:0))).setScale(2,RoundingMode.HALF_UP);
    String id="SO-%06d".formatted(++seq);LocalDate date=start.plusMonths(m).plusDays(rng.nextInt(28));
    orders.add(new SalesOrder(id,c.id(),date));items.add(new Sale(id,c.id(),c.name(),p.id(),p.name(),c.country(),c.region(),p.businessUnit(),date,qty,revenue,cost));invoices.add(new Invoice("INV-%06d".formatted(seq),id,date.plusDays(5),revenue,"PAID"));
   }
  }
  data=new DataSet(List.copyOf(customers),List.copyOf(products),List.copyOf(orders),List.copyOf(items),List.copyOf(invoices));
 }
 public DataSet load(){return data;}public String source(){return "Mock SAP Business Data Cloud";}
}
