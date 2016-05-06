package com.ge.predix.solsvc.dataingestion.handler;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.ge.predix.solsvc.bootstrap.ams.common.AssetRestConfig;
import com.ge.predix.solsvc.restclient.config.IOauthRestConfig;
import com.ge.predix.solsvc.restclient.impl.RestClient;
import com.ge.predix.solsvc.timeseries.bootstrap.config.TimeseriesRestConfig;


/**
 * 
 * @author 212421693
 *
 */
public abstract class BaseFactory {
	@SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(BaseFactory.class);
	// private static final Map<String, String> tenantUserMap = ImmutableMap.of(
	// "411", "demo:Demo,135", "777", "777_user:777_user", "511",
	// "511_user:511_user");

    /**
     * 
     */
    @Autowired
    protected IOauthRestConfig restConfig;

	
	/**
	 * 
	 */
	@Value("${predix.restHost:localhost}")
	protected String restHost;
	/**
	 * 
	 */
	@Value("${predix.restPort:9093}")
	protected String restPort;
	/**
	 * 
	 */
	@Value("${predix.restBaseResource:service}")
	protected String restBaseResource;
	
	
	/**
	 * 
	 */
	@Value("${predix.timeseries.retry.count}")
	protected int retryCount;
	
	/**
	 * 
	 */
	@Autowired
	protected RestClient restClient;

	/**
	 * 
	 */
	@Autowired
	protected AssetRestConfig assetRestConfig;

	/**
	 * 
	 */
	@Autowired
	protected TimeseriesRestConfig timeSeriesRestConfig;
	
//	public String getAssetUrl() {
//		return predixAssetUrl;
//	}
	
	/**
	 * @return -
	 */
	protected String getPredixAssetUrl() {
		return this.assetRestConfig.getAssetUri();
	}

	

	/**
	 * @return -
	 */
	public RestClient getRestClient() {
		return this.restClient;
	}


	/**
	 * @param restClient -
	 */
	public void setRestClient(RestClient restClient) {
		this.restClient = restClient;
	}

    /**
     * @param tenantId -
     * @param controllerId -
     * @param data -
     * @param authorization -
     */
    public void handlesData(String tenantId, String controllerId, String data, String authorization)
    {
        //
    }

    /**
     * @param tenantId -
     * @param controllerId -
     * @param data -
     * @param authorization -
     */
    public void handleData(String tenantId, String controllerId, String data, String authorization)
    {
        // TODO Auto-generated method stub
        
    }

}
