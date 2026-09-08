package com.bdc.provider;
import com.bdc.dto.Models.DataSet;
/** Adapter boundary. Implement authorized tenant-specific BDC/BTP/OData integration here.
 * No SAP URL or authentication contract is assumed. Normalize currency to USD and data to DataSet.
 * Add pagination, timeouts, OAuth token refresh and tenant isolation before enabling. */
public class SapBusinessDataCloudProvider implements BusinessDataProvider {
 public DataSet load(){throw new UnsupportedOperationException("SAP integration is not configured. Implement the tenant-specific adapter first.");}
 public String source(){return "SAP Business Data Cloud";}
}
