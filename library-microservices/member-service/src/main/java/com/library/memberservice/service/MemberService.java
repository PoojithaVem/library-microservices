package com.library.memberservice.service;

import com.library.memberservice.dto.*;
import com.library.memberservice.entity.Member;
import com.library.memberservice.entity.Role;
import com.library.memberservice.exception.DuplicateEmailException;
import com.library.memberservice.exception.InvalidCredentialsException;
import com.library.memberservice.exception.ResourceNotFoundException;
import com.library.memberservice.repository.MemberRepository;
import com.library.memberservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }

        Member member = Member.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // never store raw password
                .role(Role.MEMBER)
                .build();

        member = memberRepository.save(member);
        String token = jwtUtil.generateToken(member.getEmail(), member.getId(), member.getRole().name());
        return new AuthResponse(token, member.getId(), member.getName(), member.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Compare using the encoder (constant-time, salt-aware) - never with .equals() on hashes.
        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(member.getEmail(), member.getId(), member.getRole().name());
        return new AuthResponse(token, member.getId(), member.getName(), member.getRole().name());
    }

    public MemberDto getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id " + id));
        return toDto(member);
    }

    private MemberDto toDto(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .role(member.getRole().name())
                .build();
    }
}
