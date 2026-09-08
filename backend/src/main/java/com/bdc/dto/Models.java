package com.bdc.dto;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
public class Models {
 public record Customer(String id,String name,String country,String region){}
 public record Product(String id,String name,String businessUnit){}
 public record SalesOrder(String id,String customerId,LocalDate date){}
 public record Invoice(String id,String orderId,LocalDate date,BigDecimal amount,String status){}
 public record Sale(String orderId,String customerId,String customer,String productId,String product,String country,String region,String businessUnit,LocalDate date,int quantity,BigDecimal revenue,BigDecimal cost){}
 public record DataSet(List<Customer> customers,List<Product> products,List<SalesOrder> orders,List<Sale> items,List<Invoice> invoices){}
 public record Filter(LocalDate from,LocalDate to,String country,String region,String customer,String product,String businessUnit){}
 public record Row(String label,BigDecimal revenue,BigDecimal cost,long orders,BigDecimal previous,BigDecimal change){}
 public record Chart(String type,List<String> labels,List<Series> datasets){}
 public record Series(String label,List<BigDecimal> data){}
 public record Result(String answer,Map<String,BigDecimal> metrics,List<Row> table,Chart chart,List<String> insights,String dataSource,String intent,String function,String period,String conversationId){}
 public record ChatRequest(String question,String conversationId){}
}
