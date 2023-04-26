package com.rockville.wariddn.provgwv2.dto;

public class DbssResponse {

    private boolean success;
    private String msg;
    private String respCode;
    private Object respData;

    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getRespCode() {
        return respCode;
    }

    public void setRespCode(String respCode) {
        this.respCode = respCode;
    }

    public Object getRespData() {
        return respData;
    }

    public void setRespData(Object respData) {
        this.respData = respData;
    }

    @Override
    public String toString() {
        return "DBSSResponse{" + "success=" + success + ", msg=" + msg + ", respCode=" + respCode + ", respData=" + respData + '}';
    }

    /**
     * <pre>Possible responses from DBSS
     * --- SUCCESS: DBSSResponse {
     *      success=true,
     *      msg=Success,
     *      respCode=000,
     *      respData= {
     *           "data": [{"id":"d98e343a97e84999bee2dfe5236afcbb", "type":"products", "attributes":{"request-id":"d98e343a97e84999bee2dfe5236afcbb","href":"/api/v1/requests/d98e343a97e84999bee2dfe5236afcbb","status":"scheduled","scheduled-at":"2021-11-15T23:54:41+05:00"}}]
     *      }
     * }
     * --- FAILURE: DBSSResponse {
     *      success=false,
     *      msg=400 Bad Request: [{"errors":[{"detail":"Package 'Double Up Number - Daily Rs 1.72 (DOUBLENUMBERDAILYPRE)' already active and cannot be reactivated","source":{"pointer":"/data"},"status":"400","code":"2070"}]}],
     *      respCode=111,
     *      respData=null
     * }
     * </pre>
     *
     * @return
     */
    public boolean proceedWithProvisioning() {
        if (getSuccess()) {
            // we have a successful response from DBSS
            // so, we can proceed with Provisioning
            return true;
        } else if (getMsg().contains("already active and cannot be reactivated")) {
            // request at DBSS failed, indicating that the user is already ACTIVE at DBSS end
            // we can proceed with local subscription
            return true;
        }
        return false;
    }

    public boolean proceedWithDeProvisioning(String packageId) {
        if (getSuccess()) {
            // we have a successful response from DBSS
            // so, we can proceed with Provisioning
            return true;
        } else if (getMsg().contains("Package " + packageId + " is not active")) {
            // request at DBSS failed, indicating that the user is already ACTIVE at DBSS end
            // we can proceed with local subscription
            return true;
        }
        return false;
    }
}
