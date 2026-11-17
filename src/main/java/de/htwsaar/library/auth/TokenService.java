package de.htwsaar.library.auth;

import de.htwsaar.library.member.Member;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class TokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties props, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.clock = clock;
    }

    String issue(Member member) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("library-api")
                .subject(String.valueOf(member.getId()))
                .audience(List.of("library"))
                .issuedAt(now)
                .expiresAt(now.plus(props.ttl()))
                .claim("scope", String.join(" ", member.getScopes()))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
