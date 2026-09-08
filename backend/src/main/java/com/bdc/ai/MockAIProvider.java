package com.bdc.ai;
import com.bdc.dto.Models.Result;
public class MockAIProvider implements AIProvider {public String explain(Result r){return r.answer();}}
