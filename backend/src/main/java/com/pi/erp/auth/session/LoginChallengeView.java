package com.pi.erp.auth.session;

import java.util.List;

public record LoginChallengeView(String code, String challengeToken, List<UserSession> sessions) {}
