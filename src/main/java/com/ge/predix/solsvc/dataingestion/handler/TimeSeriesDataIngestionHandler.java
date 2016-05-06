package com.ge.predix.solsvc.dataingestion.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ge.predix.solsvc.bootstrap.ams.dto.Asset;
import com.ge.predix.solsvc.bootstrap.ams.dto.AssetMeter;
import com.ge.predix.solsvc.dataingestion.api.Constants;
import com.ge.predix.solsvc.dataingestion.service.type.JSONData;
import com.ge.predix.solsvc.dataingestion.websocket.WebSocketClient;
import com.ge.predix.solsvc.dataingestion.websocket.WebSocketConfig;
import com.ge.predix.solsvc.ext.util.JsonMapper;
import com.ge.predix.solsvc.timeseries.bootstrap.config.TimeseriesWSConfig;
import com.ge.predix.solsvc.timeseries.bootstrap.factories.TimeseriesFactory;
import com.ge.predix.timeseries.entity.datapoints.ingestionrequest.Body;
import com.ge.predix.timeseries.entity.datapoints.ingestionrequest.DatapointsIngestion;

/**
 * 
 * @author predix -
 */
@Component
public class TimeSeriesDataIngestionHandler extends BaseFactory
{
    private static Logger              log = Logger.getLogger(TimeSeriesDataIngestionHandler.class);
    @Autowired
    private TimeseriesFactory timeSeriesFactory;

    @Autowired
    private AssetDataHandler  assetDataHandler;
    
    
	@Autowired
	private TimeseriesWSConfig tsInjectionWSConfig;

	@Autowired
	private WebSocketConfig wsConfig;
	
	@Autowired
	private WebSocketClient wsClient;
	
	@Autowired
	private JsonMapper jsonMapper;

    /**
     *  -
     */
    @SuppressWarnings("nls")
    @PostConstruct
    public void intilizeDataIngestionHandler()
    {
        log.info("*******************TimeSeriesDataIngestionHandler Initialization complete*********************");
    }

    @Override
    @SuppressWarnings("nls")
    public void handleData(String tenentId, String controllerId, String data, String authorization)
    {
        log.debug(data);
        if (StringUtils.isEmpty(authorization)) {
        	//log.info("reading credentials from "+restConfig.getOauthClientId());
        	String[] oauthClient  = this.restConfig.getOauthClientId().split(":");
        	authorization = "Bearer "+getRestTemplate(oauthClient[0],oauthClient[1]).getAccessToken().getValue();
        }
        try
        {
            // data =
            // "[{\"name\":\"CompressionRatio\",\"unit\":\"1\",\"register\":\"\",\"datatype\":\"DOUBLE\",\"value\":13566.69088883387,\"timestamp\":1435693320269},{\"name\":\"DischgPressure\",\"unit\":\"2\",\"register\":\"\",\"datatype\":\"DOUBLE\",\"value\":14673.170162468017,\"timestamp\":1435693320269},{\"name\":\"SuctPressure\",\"unit\":\"2\",\"register\":\"\",\"datatype\":\"DOUBLE\",\"value\":20.762477268848848,\"timestamp\":1435693320269},{\"name\":\"MaxPressure\",\"unit\":\"2\",\"register\":\"\",\"datatype\":\"DOUBLE\",\"value\":14462.28474207516,\"timestamp\":1435693320269},{\"name\":\"MinPressure\",\"unit\":\"2\",\"register\":\"\",\"datatype\":\"DOUBLE\",\"value\":0.0,\"timestamp\":1435693320269},{\"name\":\"Temperature\",\"unit\":\"2\",\"register\":\"\",\"datatype\":\"DOUBLE\",\"value\":25876.681922754717,\"timestamp\":1435693320269},{\"name\":\"Velocity\",\"unit\":\"2\",\"register\":\"\",\"datatype\":\"DOUBLE\",\"value\":6355.340893305594,\"timestamp\":1435693320269}]";
            ObjectMapper mapper = new ObjectMapper();
            List<JSONData> list = mapper.readValue(data, new TypeReference<List<JSONData>>()
            {
                //
            });
            log.debug("TimeSeries URL : " + this.tsInjectionWSConfig.getInjectionUri() );
            log.debug("WebSocket URL : " + this.wsConfig.getPredixWebSocketURI());
            for (JSONData json : list)
            {
            	Long i=new Long(System.currentTimeMillis());
            	String filter = "attributes.machineControllerId.value";
            	String value = "/asset/Bently.Nevada.3500.Rack" + json.getUnit();
            	String nodeName=json.getName();
            	Asset asset = this.assetDataHandler.retrieveAsset(null, filter, value, authorization);
            	if ( asset != null )
                {
            		LinkedHashMap<String, AssetMeter> meters = asset.getAssetMeter();
                    if ( meters != null )
                    {
                    	AssetMeter assetMeter = getAssetMeter(meters, nodeName);
                    	if (assetMeter != null) {
                		                		
                    		/***POSTING DATA TO TIMESERIES WEBSOCKET***/
                    		DatapointsIngestion dpIngestion = createTimeseriesDataBody(
									json, i, asset, assetMeter);
		                    List<Header> headers = generateHeaders();
                    		this.timeSeriesFactory.createConnectionToTimeseriesWebsocket(headers);
		                    this.timeSeriesFactory.postDataToTimeseriesWebsocket(dpIngestion, headers);
		                    this.timeSeriesFactory.closeConnectionToTimeseriesWebsocket();
		                    log.info("Posted Data to Timeseries");
		                    /*****************************************/
		                    
		                    this.wsClient.postToWebSocketServer(this.jsonMapper.toJson(dpIngestion));
		                    log.info("Posted Data to Predix Websocket Server");
                    	}else {
                    		log.warn("assetMeterMap is empty, unable to find filter=attributes.machineControllerId.value=/asset/Bently.Nevada.3500.Rack" + json.getUnit() + " nodeName=" + json.getName() + " authorization=" + authorization);
                        }
                    }else{
                    	log.warn("3. asset has no meters, unable to find filter=" + filter + " = " + value + " nodeName="
                                + nodeName + " authorization=" + authorization);
                    }             
	            }else {
                	log.warn("4. asset not found, unable to find filter=" + filter + " = " + value + " nodeName="
                            + nodeName + " authorization=" + authorization);
                }
            }
            
        }
        catch (JsonParseException e)
        {
            throw new RuntimeException(e);
        }
        catch (JsonMappingException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

	/**
	 * @param json
	 * @param i
	 * @param asset
	 * @param assetMeter
	 * @return -
	 */
	private DatapointsIngestion createTimeseriesDataBody(JSONData json,
			Long i, Asset asset, AssetMeter assetMeter) {
		DatapointsIngestion dpIngestion = new DatapointsIngestion();
		dpIngestion.setMessageId(String.valueOf(System.currentTimeMillis()));
		
		Body body = new Body();
		body.setName(assetMeter.getSourceTagId());
		
		//attributes
		com.ge.dsp.pm.ext.entity.util.map.Map map = new com.ge.dsp.pm.ext.entity.util.map.Map();
		map.put("assetId", asset.getAssetId());
		if(!StringUtils.isEmpty(assetMeter.getSourceTagId())){
			 String sourceTagAttribute = assetMeter.getSourceTagId().split(":")[1];
			 map.put("sourceTagId", sourceTagAttribute); 		
		}
		body.setAttributes(map);
		
		//datapoints
		List<Object> datapoint1 = new ArrayList<Object>();
		datapoint1.add(converLocalTimeToUtcTime(json.getTimestamp().getTime()));	
		Double convertedValue =  getConvertedValue(assetMeter.getMeterDatasource().getNodeName(), Double.parseDouble(json.getValue().toString()));
		datapoint1.add(convertedValue);
		
		List<Object> datapoints = new ArrayList<Object>();
		datapoints.add(datapoint1);
		body.setDatapoints(datapoints);
		
		List<Body> bodies = new ArrayList<Body>();
		bodies.add(body);
		
		dpIngestion.setBody(bodies);		
		
		return dpIngestion;
	}


    /**
     * @param nodeName -
     * @param value -
     * @return -
     */
    @SuppressWarnings("nls")
    public Double getConvertedValue(String nodeName, Double value)
    {
        Double convValue = null;
        switch (nodeName.toLowerCase())
        {
            case Constants.COMPRESSION_RATIO:
                convValue = value * 9.0 / 65535.0 + 1;
                break;
            case Constants.DISCHG_PRESSURE:
                convValue = value * 100.0 / 65535.0;
                break;
            case Constants.SUCT_PRESSURE:
                convValue = value * 100.0 / 65535.0;
                break;
            case Constants.MAX_PRESSURE:
                convValue = value * 100.0 / 65535.0;
                break;
            case Constants.MIN_PRESSURE:
                convValue = value * 100.0 / 65535.0;
                break;
            case Constants.VELOCITY:
                convValue = value * 0.5 / 65535.0;
                break;
            case Constants.TEMPERATURE:
                convValue = value * 200.0 / 65535.0;
                break;
            default:
                throw new UnsupportedOperationException("nameName=" + nodeName + " not found");
        }
        return convValue;
    }

    private long converLocalTimeToUtcTime(long timeSinceLocalEpoch)
    {
        return timeSinceLocalEpoch + getLocalToUtcDelta();
    }

    private long getLocalToUtcDelta()
    {
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        return local.getTimeInMillis();
    }

    private AssetMeter getAssetMeter(LinkedHashMap<String, AssetMeter> meters,String nodeName)
    {
    	AssetMeter ret = null;
		if ( meters != null ) {
            for (Entry<String, AssetMeter> entry : meters.entrySet())
            {
                AssetMeter assetMeter = entry.getValue();
                //MeterDatasource dataSource = assetMeter.getMeterDatasource();
                if ( assetMeter != null && !assetMeter.getSourceTagId().isEmpty() && nodeName !=null
                        && nodeName.toLowerCase().contains(assetMeter.getSourceTagId().toLowerCase()))
                {
                    ret = assetMeter;
                    return ret;
                }
            }
		}else {
			log.warn("2. asset has no assetmeters with matching nodeName"+ nodeName);
        }
        return ret;
    }
    @SuppressWarnings("nls")
    private OAuth2RestTemplate getRestTemplate(String clientId, String clientSecret)
    {
        // get token here based on username password;
       // ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
        ClientCredentialsResourceDetails clientDetails = new ClientCredentialsResourceDetails();
        clientDetails.setClientId(clientId);
        clientDetails.setClientSecret(clientSecret);
        String url = this.restConfig.getOauthResourceProtocol() + "://" + this.restConfig.getOauthRestHost()
                + this.restConfig.getOauthResource();
        clientDetails.setAccessTokenUri(url);
        clientDetails.setGrantType("client_credentials");
       
        OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(clientDetails);

        return restTemplate;
    }
    
	@SuppressWarnings({ "unqualified-field-access", "nls" })
	private List<Header> generateHeaders()
    {
        List<Header> headers = this.restClient.getSecureTokenForClientId();
		this.restClient.addZoneToHeaders(headers,
				tsInjectionWSConfig.getZoneId());
		headers.add(new BasicHeader("Origin", "http://localhost"));
        return headers;
    }

}
