package com.rockville.wariddn.provgwv2.util;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;

@ConfigurationProperties(prefix = "provgw")
@Configuration
@Slf4j
@Getter
@Setter
public class Constants {

    private HashMap<String, String> DBSS_REQUEST;
    private HashMap<String, String> MESSAGE_TEMPLATE;  ///----------Come in the form of hash-map
//    private HashMap<String, String> QUERY_TEMPLATE;
    private HashMap<String, Object> GLOBAL_SETTINGS;
    //    private HashMap<String, Object> TABS_SETTINGS;
    
    @Value("#{'${provgw.DEMO_NUMBERS}'.split(',')}")
    private List<String> DEMO_NUMBERS;
    @Value("#{'${provgw.WHITELIST_NUMBERS}'.split(',')}")
    private List<String> WHITELIST_NUMBERS;
    @Value("#{'${provgw.DBSS_IGNORE_CHANNELS}'.split(',')}")
    private List<String> DBSS_IGNORE_CHANNELS;

    private HashMap<String, Boolean> ALLOWED_SUBSCRIPTION_BY_TYPE;
    private HashMap<String, Boolean> DO_UCIP_CHARGING_BY_USER_NETWORK_PACKAGE_TYPE;

    private HashMap<String, String> UCIP_CONFIGURATIONS;

    private HashMap<String, String> DBSS_PRODUCTID_CONFIGURATIONS;
//    private HashMap<String, String> DBSS_RABBITMQ_CONFIGURATIONS = new HashMap<>();

    private HashMap<String, String> DBSS_EVENT_CONFIGURATIONS;
    private HashMap<String, Boolean> SMS_NOTIFICATIONS;

    private boolean PRINT_SMS_BEFORE_SENDING;
    private boolean IS_DEMO_ACTIVE;
    private boolean IS_WHITELIST_ACTIVE;
    private boolean IS_DBSS_LIVE;

    private boolean SUBSCRIBE_PREPAID_FOR_FREE = false;

    private int SMS_ID;
//    private String APPLICATION_DB;
//    private String WDN_ACTOR_SYSTEM;
//    private String WDN_ACTOR;

    private String ORIGIN_HOST_NAME;
    private String ORIGIN_NODE_TYPE;
    private String ORIGIN_TRANSACTION_CODE_NORMAL;
    private String ORIGIN_TRANSACTION_CODE_GOLDEN;
    private String DEDUCTION_AMOUNT_NORMAL;
    private String DEDUCTION_AMOUNT_GOLDEN;

//    private String ORIGIN_TRANSACTION_CODE_PLUS;
//    private String ORIGIN_TRANSACTION_CODE_BASIC;
//    private String DEDUCTION_AMOUNT_PLUS;
//    private String DEDUCTION_AMOUNT_BASIC;
    private String SHORT_CODE;
    private String UCIP_ACTOR_PATH;
    private int ALLOW_POSTPAID_SUB;
    private int ALLOW_LOW_BALANCE_SUB;
    private int ALLOW_FREE_BUNDLE_SUB;
    private String SERVICE_OFF_DB_VALUE;
    private String SERVICE_ACTIVE_DB_VALUE;

    private String CHSMPP_ACTOR_PATH;
    private String TELCO_SETTING_NAME;
    private boolean IS_MEMCACHE_ENABLED;
    private String MEMCACHE_ADDRESS;

    private String QRY_ACTIVITY_LOG;  // use in class CommonOperations--------------come from application.properties
    private String GET_STATUS_LANGUAGE;

    private String QRY_UNSUBSCRIBE; ///use in class ParentUnsub in service pakage of root ---------- -come from application.properties
    private String UNSUB_FAILURE_QUERY;
    private String ON_SUCCESS_QUERY;
    private String OFF_SUCCESS_QUERY;

    ////-------------------------QRY--------------------------------  Use This
   private HashMap<String,String>QRY_TEMPLATE;

 //   private String QRY_SUB_SUCCESS_ACTIVATE_EXISTING_NORMAL_PREPAID;
 //   private String QRY_SUB_SUCCESS_ACTIVATE_EXISTING_NORMAL_POSTPAID;
 //   private String QRY_SUB_SUCCESS_ACTIVATE_EXISTING_GOLDEN;
 //   private String QRY_SUB_SUCCESS_QUERY_PREPAID_NORMAL;
  //  private String QRY_SUB_SUCCESS_QUERY_PREPAID_GOLDEN;
 //   private String QRY_SUB_SUCCESS_QUERY_POSTPAID_NORMAL;
 //    private String QRY_SUB_SUCCESS_QUERY_POSTPAID_GOLDEN;
  //  private String QRY_LOW_BALANCE_SUB_QUERY_NORMAL;
 //   private String QRY_LOW_BALANCE_SUB_QUERY_GOLDEN;
 //---------------------   private String QRY_GET_SUBSCRIBER_DETAILS_BY_VN; ----not used
   // private String QRY_GET_SUBSCRIBER_DETAILS_BY_TN;

  //  private String QRY_ADD_UPDATE_SUB_REQUEST;
 //- private String QRY_FETCH_SUB_REQUEST;
 //   private String QRY_DELETE_SUB_REQUEST;


   // private String QRY_HOLD_SUB_REQUEST;
   // private String QRY_RESERVE_DOUBLE_NUMBERS;
 //  private String QRY_DELETE_RESERVED_TMP_DN;
  //  private String QRY_GET_RESERVE_DOUBLE_NUMBERS;
  //  private String QRY_HOLD_RESERVED_NUMERMAPPING;
  //  private String QRY_SAVE_RESERVE_DOUBLE_NUMBERS;
   // private String QRY_RETRIEVE_RESERVED_TMP_DN;
   // private String QRY_HOLD_RESERVED_TMP_DN;
 //   private String QRY_RELEASE_RESERVED_DOUBLE_NUMBERS;

  //  private String QRY_VIEW_WHITELIST;
    //private String QRY_VIEW_BLACKLIST;
   // private String QRY_VIEW_GROUP;

//-------    private String QRY_EXISTING_SUB_SERVICE;  field is not used
//------    private String QRY_UNSUB_SUCCESS_QUERY_PREPAID;  field is not used
//----private String QRY_SUB_SUCCESS_QUERY_PREPAID_PLUS;    field is not used
//-------     private String QRY_SUB_SUCCESS_QUERY_PREPAID_BASIC;   field is not used

//----- private String QRY_SUB_SUCCESS_QUERY_POSTPAID_BASIC;   field is not used
 //---- private String QRY_SUB_SUCCESS_QUERY_POSTPAID_PLUS;     field is not used

 //   private String QRY_FETCH_RULE_COLLECTION_FOR_INFO_HANDLER;
 //----   private String QRY_FETCH_SYSTEM_PROPERTIES_NUMBER_MAPPING;     field is not used
//---   private String QRY_SET_BLOCK;   //  field is not used
//---    private String QRY_SET_UNBLOCK;   field is not used

 //  private String QRY_GET_CALL_FILTER;
 //   private String QRY_SET_CALL_FILTER;
  //  private String QRY_UPDATE_CALL_FILTER;

 //   private String QRY_GEN_CONFERENCE_CALL;

    //private String QRY_UPDATE_HR_FILTER;
 //   private String QRY_INSERT_HR_FILTER;
 //   private String QRY_FETCH_HR_FILTER_FOR_INFO_HANDLER;

  //-----------------  private String QRY_REMOVE_HR_FILTER;  field is not used

  //  private String QRY_INACTIVE_HR_FILTER;

 //-----------------   private String QRY_GET_COMPLETE_BLACKED_LIST;  field is not used
   // private String QRY_GET_BLACKED_LIST;
  //  private String QRY_ADD_BLACKED_LIST;
  //  private String QRY_REMOVE_BLACKED_LIST;
   // private String QRY_ACTIVE_BLACKED_LIST;

 //--------------   private String QRY_GET_COMPLETE_WHITE_LIST; field is not used
  //  private String QRY_GET_WHITE_LIST;
  //  private String QRY_ADD_WHITE_LIST;
  //  private String QRY_REMOVE_WHITE_LIST;
   // private String QRY_ACTIVE_WHITE_LIST;
  //-----------  private String QRY_ADD_DBSSEVENT; field is not used
   // private String QRY_INSERT_ON_VN_OFF_FOR_ALERT;
    //private String QRY_UPDATE_VN_ON_ALERT;
 //   private String QRY_UPDATE_USER_LANGUAGE;

     ///----------------------Below field are not used-------------------------------///

    private String GET_USER_TYPE_API;
    private String GET_USER_TYPE_API_TESTBED;

//    private String TABS_dbUrl;
//    private String TABS_user;
//    private String TABS_password;
//    private String TABS_KEY;
//    private String TABS_QUERY_NORMAL;
//    private String TABS_QUERY_GENERIC;
//    private String TABS_EQUIPID_GOLDEN;
//    private String TABS_EQUIPID_NORMAL;
//    private String TABS_EQUIPID_BUNDLE_BASIC;
//    private String TABS_EQUIPID_BUNDLE_PLUS;
//    private int TABS_MAX_CONN_POOL_SIZE;
//    private String TABS_SUB_SERVICE;
//    private String TABS_UNSUB_SERVICE;
//    private String CRM_CHECK_SUBSCRIBER;
//    private String CRM_CHECK_MYFI_WINGLE;

///----------------------Below field are not used-------------------------------///
    private String DB_OPERATOR;
    private String DB_SERVICE;
    private String IMPLEMENTATION_PACKAGE;



    private String GET_USER_DETAILS_API;
    private String UPDATE_USER_DETAILS_API;
//    private String GET_PAYMENT_TYPE;

    private Integer AssignedVNThreadTime;

    private String QRY_FETCH_GOLDEN_SUB_NOT_REPLIED_FOR_10MINS;

    private Boolean ALLOWED_TYPE_WARID_PREPAID;
    private Boolean ALLOWED_TYPE_WARID_POSTPAID;
    private Boolean ALLOWED_TYPE_JAZZ_PREPAID;
    private Boolean ALLOWED_TYPE_JAZZ_POSTPAID;
    private Boolean ALLOWED_TYPE_CHAMPION;

//    private Boolean IS_POSTPAID_BASE_CONSOLIDATED;
    private String DEFAULT_MESSAGE_FORMAT;
    private String GET_USER_TYPE_API_ISSUSPENDED;

    private String UCIP_CHARGING_API;

//    public String requestToString(Request req) {
//        return "Request{" + "methodName=" + req.getMethodName()
//                + ", msisdn=" + req.getMsisdn()
//                + ", destination=" + req.getDestination()
//                + ", service=" + req.getService()
//                + ", transactionId=" + req.getTransactionId()
//                + ", originalRequest=" + req.getOriginalRequest()
//                + ", language=" + req.getLanguage()
//                + ", description=" + req.getDescription()
//                + ", virtualNumber=" + req.getVirtualNumber() + '}';
//    }
}
