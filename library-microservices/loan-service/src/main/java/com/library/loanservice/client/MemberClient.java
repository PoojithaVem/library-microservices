package com.library.loanservice.client;

import com.library.loanservice.dto.MemberDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-service", fallbackFactory = MemberClientFallbackFactory.class)
public interface MemberClient {

    @GetMapping("/api/members/{id}")
    MemberDto getMemberById(@PathVariable("id") Long memberId);
}
