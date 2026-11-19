package de.htwsaar.library.auth;

import de.htwsaar.library.member.Member;
import de.htwsaar.library.member.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class AuthService {


    private final Logger logger =
            LoggerFactory.getLogger(AuthService.class);

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final TokenService tokenService;

    public AuthService(MemberService memberService, PasswordEncoder passwordEncoder, JwtProperties jwtProperties, TokenService tokenService) {
        this.memberService = memberService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.tokenService = tokenService;
    }
    public TokenDtos.TokenResponse login(TokenDtos.LoginRequest req) {
        Member member = memberService.findByEmail(req.email())
                .orElseThrow(() -> loginFailed("unknown-email"));

        if (member.getPasswordHash() == null)
            throw loginFailed("no-password");

        if (!passwordEncoder.matches(req.password(), member.getPasswordHash()))
            throw loginFailed("wrong-password");

        if (member.isLocked())
            throw loginFailed("locked");

        return new TokenDtos.TokenResponse(tokenService.issue(member), "Bearer", jwtProperties.ttl().toSeconds());
    }

    private AuthExceptions.InvalidCredentialsException loginFailed(String reason) {
        logger.atWarn()
                .setMessage("Login failed")
                .addKeyValue("reason", reason)
                .log();
        return new AuthExceptions.InvalidCredentialsException();
    }
}
