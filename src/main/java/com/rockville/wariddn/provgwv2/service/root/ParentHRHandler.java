package com.rockville.wariddn.provgwv2.service.root;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockville.wariddn.provgwv2.dto.HrFilterDto;
import com.rockville.wariddn.provgwv2.dto.IncomingSMS;
import com.rockville.wariddn.provgwv2.dto.Request;
import com.rockville.wariddn.provgwv2.dto.VnDetails;
import com.rockville.wariddn.provgwv2.service.Operation;
import com.rockville.wariddn.provgwv2.service.SmsSendingService;
import com.rockville.wariddn.provgwv2.util.CommonOperations;
import com.rockville.wariddn.provgwv2.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParentHRHandler extends Operation {

    private final Constants constants;
    private final CommonOperations commonOperations;
    private final SmsSendingService smsSendingService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(Request request) {
        try {

            if (request != null && request.getMsisdn() == null) {
                log.error("[" + request.getTransactionId() + "] MSISDN can not be null:" + request.getMsisdn());
                return;
            }
            request.setMsisdn(Operation.normalizeMsisdn(request.getMsisdn()));

            request = validateLanguage(request);

            log.info("Request : {}", request);

            String sms_string_after_replacing_params = "";
            String qry_after_param_replacement = "";

            VnDetails vnd = commonOperations.fetchSubscriptionDetailsByTelcoNum(request.getMsisdn());
            if (vnd != null) {
                log.info(vnd.toString());
            }

            if (vnd != null && vnd.getVirtualMSISDN() > 0 && vnd.getStatus() > 0 && vnd.getStatus() < 4) {
                ObjectMapper mapper = new ObjectMapper();
                IncomingSMS sms = null;
                try {
                    sms = mapper.readValue(request.getOriginalRequest(), IncomingSMS.class);
                } catch (IOException ex) {
                    log.error("Exception in ParentHRHandler while parsing sms : {}", sms, ex);
                }
                String msg = new String(sms.getMessageBody()).substring(3).trim();
                if (!msg.isEmpty()) {
                    log.info("[" + request.getTransactionId() + "] Subscriber want to set hourly filter:" + msg);

                    List<HrFilterDto> dailySchedule = jdbcTemplate.query(constants.getQRY_TEMPLATE().get("QRY_FETCH_HR_FILTER_FOR_INFO_HANDLER")
                                    //getQRY_FETCH_HR_FILTER_FOR_INFO_HANDLER()
                            .replace("{telcoMsisdn}", request.getMsisdn()),
                            new BeanPropertyRowMapper<>(HrFilterDto.class)
                    );
                    log.info("Daily Schedule: " + dailySchedule);

                    if (msg.equalsIgnoreCase("OFF")) {
                        if (!dailySchedule.isEmpty()) {
                            qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_INACTIVE_HR_FILTER")
                                    //getQRY_INACTIVE_HR_FILTER()
                                    .replace("{TNUM}", request.getMsisdn());
                            int res = jdbcTemplate.update(qry_after_param_replacement);
                            commonOperations.writeActivityLog(request, "HR Filter OFF", "SUCCESS");
                            sms_string_after_replacing_params
                                    = constants.getMESSAGE_TEMPLATE().get("MSG_HR_OFF_" + request.getLanguage());
                        }
                    } else {
                        String[] splitedMsg = msg.split("\\s+");
                        String startTime = splitedMsg[1];
                        String endTime = splitedMsg[2];

                        Pattern patt = Pattern.compile("\\d{0,2}:?\\d{0,2}");
                        Matcher matcher = patt.matcher(startTime);
                        if (matcher.find()) {
                            Matcher endTimeMatcher = patt.matcher(endTime);
                            if (endTimeMatcher.find()) {
                                startTime = matcher.group();
                                endTime = endTimeMatcher.group();
                                Calendar a = null, b = null;
                                if (startTime.contains(":")) {
                                    a = getDate(startTime);

                                } else {
                                    startTime = startTime + ":00";
                                    a = getDate(startTime);
                                }
                                if (endTime.contains(":")) {
                                    b = getDate(endTime);

                                } else {
                                    endTime = endTime + ":00";
                                    b = getDate(endTime);
                                }
                                if (a.before(b)) {
                                    String startTimeFormatted = formatCalendarToTime(a);
                                    String endTimeFormatted = formatCalendarToTime(b);
                                    String startDateFormatted = formatCalendarToDate(a);
                                    String endDateFormatted = formatCalendarToDate(b);

                                    if (!dailySchedule.isEmpty()) {
                                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_UPDATE_HR_FILTER")
                                                //getQRY_UPDATE_HR_FILTER()
                                                .replace("{START_TIME}", startTimeFormatted)
                                                .replace("{END_TIME}", endTimeFormatted)
                                                .replace("{START_DATE}", startDateFormatted)
                                                .replace("{END_DATE}", endDateFormatted)
                                                .replace("{TNUM}", request.getMsisdn());
                                    } else {
                                        qry_after_param_replacement = constants.getQRY_TEMPLATE().get("QRY_INSERT_HR_FILTER")
                                                //getQRY_INSERT_HR_FILTER()
                                                .replace("{TNUM}", request.getMsisdn())
                                                .replace("{START_TIME}", startTimeFormatted)
                                                .replace("{END_TIME}", endTimeFormatted)
                                                .replace("{START_DATE}", startDateFormatted)
                                                .replace("{END_DATE}", endDateFormatted);
                                    }
                                    log.info(qry_after_param_replacement);

                                    int res = jdbcTemplate.update(qry_after_param_replacement);
                                    commonOperations.writeActivityLog(request, "HR Filter", "SUCCESS");
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_HR_ON_" + request.getLanguage());

                                    sms_string_after_replacing_params = sms_string_after_replacing_params
                                            .replace("{HR_FILTER}", "" + splitedMsg[1] + "to" + splitedMsg[2]);

                                } else {
                                    log.info("Endtime should be greater than start time");
                                    sms_string_after_replacing_params
                                            = constants.getMESSAGE_TEMPLATE().get("MSG_HR_FAILURE_WRONG_TIME_" + request.getLanguage());

                                }
                            } else {
                                log.info("Endtime is Not in HH:MM  Format");
                                sms_string_after_replacing_params
                                        = constants.getMESSAGE_TEMPLATE().get("MSG_HR_FAILURE_FORMAT_" + request.getLanguage());
                            }

                        } else {
                            log.info("Starttime is Not in HH:MM  Format");
                            sms_string_after_replacing_params
                                    = constants.getMESSAGE_TEMPLATE().get("MSG_HR_FAILURE_FORMAT_" + request.getLanguage());
                        }
                    }
                } else {
                    log.error("[" + request.getTransactionId() + "] Invalid Command");
                    commonOperations.writeActivityLog(request, "HR", "Invalid Command");
                    sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_INVALID_CMD_SIG_HELP_" + request.getLanguage());
                }
                smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                        sms_string_after_replacing_params,
                        request.getTransactionId(), request.getSendSms());
            } else {
                log.error("[" + request.getTransactionId() + "] Not A Subscriber");
                commonOperations.writeActivityLog(request, "INFO", "NonSub");
                sms_string_after_replacing_params = constants.getMESSAGE_TEMPLATE().get("MSG_INFO_NOT_SUB_" + request.getLanguage());

                smsSendingService.sendSms(constants.getSHORT_CODE(), request.getMsisdn(),
                        sms_string_after_replacing_params,
                        request.getTransactionId(), request.getSendSms());
            }
        } catch (Exception ex) {
            log.error("Exception: " + ex, ex);
        }
    }

    private Calendar getDate(String startTime) {
        String[] a = startTime.split(":");
        Calendar c1 = Calendar.getInstance();
        c1.set(Calendar.HOUR_OF_DAY, Integer.parseInt(a[0]));
        c1.set(Calendar.MINUTE, Integer.parseInt(a[1]));
        return c1;
    }

    private String formatCalendarToTime(Calendar calendar) {
        Date dateOne = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm"); //if 24 hour format
        String time = format.format(dateOne);
        return time;
    }

    private String formatCalendarToDate(Calendar calendar) {
        Date dateOne = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd"); //if 24 hour format
        String date = format.format(dateOne);
        return date;
    }
}
