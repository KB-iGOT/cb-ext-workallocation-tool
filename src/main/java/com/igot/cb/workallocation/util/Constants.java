package com.igot.cb.workallocation.util;

/**
 * @author Mahesh RV
 */
public class Constants {

    public static final String KEYSPACE_SUNBIRD = "sunbird";
    public static final String CORE_CONNECTIONS_PER_HOST_FOR_LOCAL = "coreConnectionsPerHostForLocal";
    public static final String CORE_CONNECTIONS_PER_HOST_FOR_REMOTE = "coreConnectionsPerHostForRemote";
    public static final String HEARTBEAT_INTERVAL = "heartbeatIntervalSeconds";
    public static final String CASSANDRA_CONFIG_HOST = "cassandra.config.host";
    public static final String SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL = "sunbird_cassandra_consistency_level";
    public static final String DEFAULT_SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL = "ONE";
    public static final String EXCEPTION_MSG_FETCH = "Exception occurred while fetching record from ";
    public static final String INSERT_INTO = "INSERT INTO ";
    public static final String DOT = ".";
    public static final String OPEN_BRACE = "(";
    public static final String VALUES_WITH_BRACE = ") VALUES (";
    public static final String QUE_MARK = "?";
    public static final String COMMA = ",";
    public static final String CLOSING_BRACE = ");";
    public static final String RESPONSE = "response";
    public static final String SUCCESS = "success";
    public static final String FAILED = "Failed";
    public static final String ERROR_MESSAGE = "errmsg";
    public static final String ERROR = "ERROR";
    public static final String DOT_SEPARATOR = ".";
    public static final String SHA_256_WITH_RSA = "SHA256withRSA";
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String SUB = "sub";
    public static final String SSO_URL = "sso.url";
    public static final String SSO_REALM = "sso.realm";
    public static final String ACCESS_TOKEN_PUBLICKEY_BASEPATH = "accesstoken.publickey.basepath";
    public static final String ID = "id";
    public static final String FETCH_RESULT_CONSTANT = ".fetchResult:";
    public static final String URI_CONSTANT = "URI: ";
    public static final String API_VERSION_1 = "1.0";
    public static final String UPDATE = "UPDATE ";
    public static final String SET = " SET ";
    public static final String WHERE_ID = "where id";
    public static final String EQUAL_WITH_QUE_MARK = " = ? ";
    public static final String SEMICOLON = ";";
    public static final String USER = "user";
    public static final String UNKNOWN_IDENTIFIER = "Unknown identifier ";
    public static final String EXCEPTION_MSG_UPDATE = "Exception occurred while updating record to ";
    public static final String SUNBIRD_ENCRYPTION = "sunbird_encryption";
    public static final String ON = "ON";
    public static final String ENCRYPTION_KEY = "sunbird_encryption_key";
    public static final String USER_ORG_SERVICE_PREFIX = "UOS_";
    public static final String X_AUTH_TOKEN = "x-authenticated-user-token";
    public static final String USER_ID_DOESNT_EXIST = "User Id doesn't exist! Please supply a valid auth token";
    public static final String API_CREATE_WORK_ALLOCATION = "api.work.allocation.create";
    public static final String CREATE = "create";
    public static final String DRAFT = "Draft";
    public static final String DATA = "data";
    public static final String TABLE_WORK_ALLOCATION = "workallocation";
    public static final String WORK_ORDER_ID = "workorderid";
    public static final String TABLE_WORK_ORDER = "workorder";
    public static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String API_UPDATE_WORK_ALLOCATION = "api.work.allocation.update";
    public static final String API_UPDATE_WORK_ORDER = "api.work.order.update" ;
    public static final String API_FILE_UPLOAD = "api.file.upload";
    public static final String USER_ID = "userId";

    private Constants() {
    }
}
