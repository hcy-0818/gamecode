package com.example.account.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.Registration;
import java.util.List;

public interface RegistrationService extends IService<Registration> {
    Registration submit(Long userId, Registration registration);
    void cancel(Long userId, Long registrationId);
    List<Registration> listMyRegistrations(Long userId);
    List<Registration> listPending();
    List<Registration> listAll();
    void approve(Long registrationId);
    void reject(Long registrationId);
    void delete(Long userId, Long registrationId);
}
