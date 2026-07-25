package com.library.loanservice.client;

import com.library.loanservice.dto.MemberDto;
import com.library.loanservice.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class MemberClientFallbackFactory implements FallbackFactory<MemberClient> {

    private static final Logger log = LoggerFactory.getLogger(MemberClientFallbackFactory.class);

    @Override
    public MemberClient create(Throwable cause) {
        return memberId -> {
            log.warn("member-service unavailable while validating member {}: {}", memberId, cause.getMessage());
            throw new DownstreamServiceException("Member service is currently unavailable. Please try again shortly.");
        };
    }
}
