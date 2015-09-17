package com.healthcloud.qa.utils;

import static com.jayway.restassured.RestAssured.given;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

/**
 * Wrapper for RestAssured. Uses an HTTP request template and a single record housed in a RecordHandler object to
 * generate and perform an HTTP requests.
 * 
 */
public class HTTPReqGen {
	
	public enum HttpType{
		PUT,GET,DELETE,POST
	}
  
  protected static final Logger logger = LoggerFactory.getLogger(HTTPReqGen.class);

  private RequestSpecification reqSpec;

  private String call_host = "";
  private String call_suffix = "";
  private String call_string = "";
  private HttpType call_type = null;
  private String body = "";
  private Map<String, String> headers = new HashMap<String, String>();
  private HashMap<String, String> cookie_list = new HashMap<String, String>();

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getCallString() {
    return call_string;
  }

  /**
   * Constructor. Initializes the RequestSpecification (relaxedHTTPSValidation avoids certificate errors).
   * 
   */
  public HTTPReqGen() {
    reqSpec = given().relaxedHTTPSValidation();
  }

  public HTTPReqGen(String proxy) {
    reqSpec = given().relaxedHTTPSValidation().proxy(proxy);
  }

  /**
   * Pulls HashMap from given RecordHandler and calls primary generate_request method with it.
   * 
   * @param template String, should contain the full template.
   * @param record RecordHandler, the input data used to fill in replacement tags that exist in the template.
   * @return this Reference to this class, primarily to allow request generation and performance in one line.
   * @throws Exception 
   */
  public HTTPReqGen generate_request(String template, RecordHandler record) throws Exception {

    return generate_request(template, (HashMap<String, String>) record.get_map());
  }

  /**
   * Generates request data, using input record to fill in the template and then parse out relevant data. To fill in the
   * template, identifies tags surrounded by << and >> and uses the text from the corresponding fields in the
   * RecordHandler to replace them. The replacement is recursive, so tags may also exist in the fields of the
   * RecordHandler so long as they are also represented by the RecordHandler and do not form an endless loop.
   * After filling in the template, parses the resulting string in preparation for performing the HTTP request. Expects the
   * the string to be in the following format:
   *
   * <<call_type>> <<call_suffix>>
   * Host: <<root_host_name>>
   * <<header1_name>>:<<header1_value>>
   * ...
   * <<headerN_name>>: <<headerN_value>>
   *
   * <<body_text>>
   *
   * <<call_type>> must be GET, PUT, POST, or DELETE. <<call_suffix>> must be a string with no spaces. It is appended to
   * <<root_host_name>> to form the complete call string. After a single blank line is encountered, the rest of the file
   * is used as the body of text for PUT and POST calls. This function also expects the Record Handler to include a field
   * named "VPID" containing a unique record identifier for debugging purposes.
   * 
   * @param template String, should contain the full template.
   * @param record RecordHandler, the input data used to fill in replacement tags that exist in the template.
   * @return this Reference to this class, primarily to allow request generation and performance in one line.
   * @throws Exception 
   */
  public HTTPReqGen generate_request(String template, HashMap<String, String> record) throws Exception {

    String filled_template = "";
    Boolean found_replacement = true;
    headers.clear();
    
    try {
      
      // Splits template into tokens, separating out the replacement strings
      // like <<id>>
      String[] tokens = tokenize_template(template);

      // Repeatedly perform replacements with data from record until no
      // replacements are found
      // If a replacement's result is an empty string, it will not throw an
      // error (but will throw one if there is no column for that result)
      while(found_replacement) {
        found_replacement = false;
        filled_template = "";
  
        for(String item: tokens) {
  
          if(item.startsWith("<<") && item.endsWith(">>")) {
            found_replacement = true;
            item = item.substring(2, item.length() - 2);
            
            if( !record.containsKey(item)) {
              logger.info("Template contained replacement string whose value did not exist in input record:[" + item + "]");
            }            
            
            item = record.get(item);
          }
  
          filled_template += item;
        }
  
        tokens = tokenize_template(filled_template);
      }
      
    } catch (Exception e) {
      logger.error("Problem performing replacements from template: ", e);
    }

    try {
      
      // Feed filled template into BufferedReader so that we can read it line
      // by line.
      InputStream stream = IOUtils.toInputStream(filled_template, "UTF-8");
      BufferedReader in = new BufferedReader(new InputStreamReader(stream));
      String line = "";
      String[] line_tokens;
      
      // First line should always be call type followed by call suffix
      line = in.readLine();
      line_tokens = line.split(" ");
      call_type = Enum.valueOf(HttpType.class, line_tokens[0]);
      call_suffix = line_tokens[1];

      // Second line should contain the host as it's second token
      line = in.readLine();
      line_tokens = line.split(" ");
      call_host = line_tokens[1];

      // Full call string for RestAssured will be concatenation of call
      // host and call suffix
      call_string = call_host + call_suffix;

      // Remaining lines will contain headers, until the read line is
      // empty
      line = in.readLine();
      while(line != null && !line.equals("")) {

        String lineP1 = line.substring(0, line.indexOf(":")).trim();
        String lineP2 = line.substring(line.indexOf(" "), line.length()).trim();

        headers.put(lineP1, lineP2);

        line = in.readLine();
      }

      // If read line is empty, but next line(s) have data, create body
      // from them
      if(line != null && line.equals("")) {
        body = "";
        while( (line = in.readLine()) != null && !line.equals("")) {
          body += line;
        }
      }

    } catch(Exception e) {
      logger.error("Problem setting request values from template: ", e);
    }

    return this;
  }
  
  /**
   * Performs the request using the stored request data and then returns the response.
   * 
   * @return response Response, will contain entire response (response string and status code).
   */
  public Response perform_request() throws Exception {
    
    Response response = null;
    
    try {

      for(Map.Entry<String, String> entry: headers.entrySet()) {
        reqSpec.header(entry.getKey(), entry.getValue());
      }
      
      for(Map.Entry<String, String> entry: cookie_list.entrySet()) {
        reqSpec.cookie(entry.getKey(), entry.getValue());
      }
  
      switch(call_type) {
  
        case GET: {
          response = reqSpec.get(call_string);
          break;
        }
        case POST: {
          response = reqSpec.body(body).post(call_string);
          break;
        }
        case PUT: {
          response = reqSpec.body(body).put(call_string);
          break;
        }
        case DELETE: {
          response = reqSpec.delete(call_string);
          break;
        }
  
        default: {
          logger.error("Unknown call type: [" + call_type + "]");
        }
      }
      
    } catch (Exception e) {
      logger.error("Problem performing request: ", e);
    }

    return response;
  }

  /**
   * Splits a template string into tokens, separating out tokens that look like "<<key>>"
   * 
   * @param template String, the template to be tokenized.
   * @return list String[], contains the tokens from the template.
   */
  private String[] tokenize_template(String template) {
    return template.split("(?=[<]{2})|(?<=[>]{2})");
  }

}