package com.bdc.config;
import com.bdc.provider.*;import com.bdc.ai.*;import org.springframework.context.annotation.*;import org.springframework.beans.factory.annotation.Value;
@Configuration public class Providers {
 @Bean BusinessDataProvider data(@Value("${bdc.demo}") boolean demo,@Value("${bdc.provider}") String provider){if(demo||provider.equals("mock"))return new MockBusinessDataProvider();if(provider.equals("sap"))return new SapBusinessDataCloudProvider();throw new IllegalArgumentException("Unknown business provider");}
 @Bean AIProvider ai(@Value("${bdc.demo}") boolean demo,@Value("${bdc.ai}") String provider){if(demo||provider.equals("mock"))return new MockAIProvider();if(provider.equals("openai"))return new OpenAIProvider();throw new IllegalArgumentException("AI provider is not implemented: "+provider);}
}
