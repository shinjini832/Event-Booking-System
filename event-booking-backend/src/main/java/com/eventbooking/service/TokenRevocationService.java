package com.eventbooking.service;

import com.eventbooking.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeFamily(String familyId) {
        var family = refreshTokenRepository.findByFamilyId(familyId);
        family.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAllAndFlush(family);
    }
}
