package com.okta.loadgen;

import static com.okta.loadgen.LoadGen.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.*;
/**
 *
 * @author schandra
 */

public class LoadGen {
    final static Properties configuration = new Properties();
    protected static int numConsumers = 1;

    public static void main(String args[]) throws Exception{
        System.out.println("Start LoadGen: "+new Date());
        System.out.println();
        long startTime = System.currentTimeMillis();
        
        if (args.length < 1)
        {
            System.out.println(new Date() + " : **ERROR** : Missing configuration file argument");
            System.out.println("Run using following command : ");
            System.out.println("java -jar load_gen.jar <config_file>");
            System.exit(-1);
        }
        try{
            configuration.load(new FileInputStream(args[0]));
        }
        catch(Exception e){
            System.out.println("Error reading configuration. Exiting...");
            System.exit(-1);
        }
        numConsumers = Integer.parseInt(configuration.getProperty("numConsumers", "1"));
        
        Thread[] consumers = new Thread[numConsumers];
		for (int i = 0; i < numConsumers; i++){
    	    Consumer worker = new Consumer();
        	consumers[i] = new Thread(worker);
            consumers[i].start();
        }
        
	    for (int i = 0; i < numConsumers; i++)
    	    consumers[i].join();

        System.out.println();
        System.out.println("Done : "+new Date());
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime)/1000;
        System.out.println("Total time for load = "+duration+" seconds");
    }
}
   
 class Consumer implements Runnable {
    private final String org;
    private final String apiToken;
    private final CloseableHttpClient httpclient;
    Consumer() { 
        org = configuration.getProperty("org");
        apiToken = configuration.getProperty("apiToken");
        httpclient = HttpClientBuilder.create().setRetryHandler(new DefaultHttpRequestRetryHandler(3, false)).build();
    }
    public void run() {
        try {
            while (true) { 
                consume("Something");
            }
        } catch (InterruptedException ex) { 
            //System.out.println("Finished processing for this thread");
        } catch (Exception excp) {
            System.out.println(excp.getLocalizedMessage());//This consumer thread will abort execution
        }     
    }
   
    void consume(Object dummy) throws Exception{
        JSONObject user = new JSONObject();
        JSONObject profile = new JSONObject();

        profile.put("firstName","ChangeMe");


        user.put("profile", profile);

        // Build JSON payload
        StringEntity data = new StringEntity(user.toString(),ContentType.APPLICATION_JSON);

        // build http request and assign payload data
        HttpUriRequest request = RequestBuilder
                .post("https://"+org+"/api/v1/users/"+RandomStringUtils.randomAlphabetic(8))
                .setHeader("Authorization", "SSWS " + apiToken)
                .setEntity(data)
                .build();
        CloseableHttpResponse httpResponse = null;
        try{
            httpResponse = httpclient.execute(request);
        } catch(Exception e){//Issue with the connection. Let's not lose the consumer thread
			//No-op
        }finally{
            if (null != httpResponse)
                httpResponse.close();
        }
    }
}
