package com.moneybook.backend.common.controller;

import com.moneybook.backend.common.response.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 백엔드 애플리케이션의 기동 상태를 확인하는 API를 제공한다.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * 인증 없이 접근할 수 있는 애플리케이션 상태를 반환한다.
     *
     * @return 정상 기동 상태
     */
    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP");
    }
}
