package com.bdc.search;
import java.util.List;
public interface WebSearchProvider {
 record Source(String title,String url,String snippet) {}
 List<Source> search(String query);
 String name();
}
