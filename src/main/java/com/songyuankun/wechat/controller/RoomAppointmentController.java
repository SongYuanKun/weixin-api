package com.songyuankun.wechat.controller;

import com.songyuankun.wechat.common.DaoCommon;
import com.songyuankun.wechat.common.Response;
import com.songyuankun.wechat.common.ResponseUtils;
import com.songyuankun.wechat.dao.AppointmentTimePoint;
import com.songyuankun.wechat.dao.TimePoint;
import com.songyuankun.wechat.enums.AppointmentTimePointStatusEnum;
import com.songyuankun.wechat.repository.AppointmentTimePointRepository;
import com.songyuankun.wechat.request.RoomAppointmentForm;
import com.songyuankun.wechat.response.MyAppointmentTimeResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author songyuankun
 */
@Api
@RestController
@RequestMapping("room_appointment")
@Slf4j
public class RoomAppointmentController {

    private final AppointmentTimePointRepository appointmentTimePointRepository;

    public RoomAppointmentController(AppointmentTimePointRepository appointmentTimePointRepository) {
        this.appointmentTimePointRepository = appointmentTimePointRepository;
    }

    @PostMapping("save")
    public Response save(Authentication authentication, @RequestBody @Validated RoomAppointmentForm roomAppointmentForm) {
        Integer userId = Integer.valueOf(authentication.getName());
        for (Integer integer : roomAppointmentForm.getCurrentTime()) {
            AppointmentTimePoint appointmentTimePoint = new AppointmentTimePoint();
            DaoCommon.createDao(authentication, appointmentTimePoint);
            appointmentTimePoint.setDay(roomAppointmentForm.getDay());
            appointmentTimePoint.setUserId(userId);
            appointmentTimePoint.setStatus(0);
            appointmentTimePoint.setCreateTime(new Date());
            appointmentTimePoint.setTimePointId(integer);
            appointmentTimePointRepository.save(appointmentTimePoint);
        }
        return ResponseUtils.success("");
    }

    @GetMapping("queryAppointmentTime")
    public List<TimePoint> queryEmptyTime(String date) {
        List<AppointmentTimePoint> appointmentTimePoints = appointmentTimePointRepository.findAllByDay(date);
        List<Integer> timePoints = appointmentTimePoints.stream().map(AppointmentTimePoint::getTimePointId).collect(Collectors.toList());
        List<TimePoint> all = new ArrayList<>(TimePoint.LIST);
        all.forEach(t -> t.setStatus(timePoints.contains(t.getId()) ? 1 : 0));
        return all;
    }

    @GetMapping("queryMyAppointment")
    public List<MyAppointmentTimeResponse> queryMyAppointment(Authentication authentication, @RequestParam(required = false, defaultValue = "0") Integer pageNumber, @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<MyAppointmentTimeResponse> responseList = new ArrayList<>();
        Integer userId = Integer.valueOf(authentication.getName());
        Sort sort = new Sort(Sort.Direction.DESC, "day", "timePointId");
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        List<Integer> statusList = Arrays.asList(AppointmentTimePointStatusEnum.SUCCESS.getValue(), AppointmentTimePointStatusEnum.SIGN_IN.getValue());
        List<AppointmentTimePoint> appointmentTimePoints = appointmentTimePointRepository.findAllByUserIdAndStatusIn(userId, statusList, pageable);
        for (AppointmentTimePoint appointmentTimePoint : appointmentTimePoints) {
            MyAppointmentTimeResponse myAppointmentTimeResponse = new MyAppointmentTimeResponse();
            myAppointmentTimeResponse.setDay(appointmentTimePoint.getDay());
            myAppointmentTimeResponse.setStatus(appointmentTimePoint.getStatus());
            myAppointmentTimeResponse.setTimePointId(appointmentTimePoint.getTimePointId());
            myAppointmentTimeResponse.setTimePoint(TimePoint.MAP.get(appointmentTimePoint.getTimePointId()));
            responseList.add(myAppointmentTimeResponse);
        }
        return responseList;
    }

}
