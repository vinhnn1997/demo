package vn.gov.tax.common.security;

import java.time.Instant;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TokenRevocationService {
    private static final DefaultRedisScript<Long> SET_MAX_CUTOFF_SCRIPT = new DefaultRedisScript<>();

    static {
        SET_MAX_CUTOFF_SCRIPT.setResultType(Long.class);
        SET_MAX_CUTOFF_SCRIPT.setScriptText("""
                local current = redis.call('GET', KEYS[1])
                local newVal = tonumber(ARGV[1])
                local ttl = tonumber(ARGV[2])
                if (not current) or (tonumber(current) < newVal) then
                  redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
                  return newVal
                end
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
                return tonumber(current)
                """);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final TokenRevocationProperties properties;

    public void revokeTokensIssuedNotAfter(String userId, Instant cutoff) {
        if (userId == null || userId.isBlank() || cutoff == null) {
            throw new IllegalArgumentException("userId and cutoff are required");
        }
        String key = properties.getKeyPrefix() + userId;
        long epochSeconds = cutoff.getEpochSecond();
        long ttlMillis = Math.max(1L, properties.getTtl().toMillis());
        stringRedisTemplate.execute(
                SET_MAX_CUTOFF_SCRIPT,
                List.of(key),
                Long.toString(epochSeconds),
                Long.toString(ttlMillis));
    }

    public Long findCutoffEpochSeconds(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        String value = stringRedisTemplate.opsForValue().get(properties.getKeyPrefix() + userId);
        return value == null ? null : Long.valueOf(value);
    }
}
