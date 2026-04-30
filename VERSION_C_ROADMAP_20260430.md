# 🔒 VERSION C - SECURITY HARDENING & TEST EXPANSION ROADMAP

**Status**: Planning Phase  
**Target Completion**: May 2-3, 2026 (2-3 days)  
**Dependencies**: Version B ✅ Complete  
**Goal**: 80% code coverage + Security hardening for production

---

## 📋 COMPONENTS TO BUILD IN VERSION C

### PART 1: Rate Limiting (Security Layer) ⏰

**Feature**: Prevent brute force attacks + API abuse

**Implementation**:

```java
@Service
public class RateLimitingService {
    // Per endpoint rate limiting
    // Config: max 100 requests per 5 minutes per IP
    
    @Bean
    public RateLimiter rateLimiter() {
        return RateLimiter.create(20.0); // 20 requests/sec
    }
    
    // Integrate with @Aspect for AOP
    @Around("@annotation(RateLimited)")
    public Object rateLimit(ProceedingJoinPoint pjp) {
        // Check if client exceeded limit
        // Return 429 Too Many Requests if exceeded
    }
}

// Custom annotation
@RateLimited(maxRequests = 100, windowMinutes = 5)
@PostMapping("/api/v1/sdi/trasmetti/{fatturaId}")
public ResponseEntity<SdiTransmissionResponse> trasmetti(...) { ... }
```

**Effort**: 3-4 hours
**Files**: 
- RateLimitingService.java
- RateLimitedAspect.java
- @RateLimited annotation

---

### PART 2: CSRF Protection (Security Layer) 🛡️

**Feature**: Prevent Cross-Site Request Forgery attacks

**Implementation**:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .and()
            .authorizeRequests()
                .antMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll();
    }
}

// Client must:
// 1. GET /api/csrf → receive token
// 2. POST /api/fatture with X-CSRF-Token header
```

**Effort**: 2-3 hours
**Files**:
- SecurityConfig.java (updated)
- CsrfTokenController.java
- Documentation

---

### PART 3: Input Sanitization (Security Layer) 💪

**Feature**: Prevent XSS + injection attacks via input cleaning

**Implementation**:

```java
@Component
public class InputSanitizer {
    public static String sanitize(String input) {
        if (input == null) return null;
        
        // Remove HTML tags
        String clean = input.replaceAll("<[^>]*>", "");
        
        // Remove SQL injection attempts
        clean = clean.replaceAll("('|(\\-\\-)|(;)|(\\||&&)|(DROP|DELETE|INSERT|UPDATE))", "");
        
        // Limit length
        if (clean.length() > 255) {
            clean = clean.substring(0, 255);
        }
        
        return clean.trim();
    }
}

// Apply on all string fields in DTOs
@NotBlank
@JsonProperty
private String numero = InputSanitizer.sanitize(raw_input);
```

**Effort**: 2-3 hours
**Files**:
- InputSanitizer.java
- Updated all DTOs

---

### PART 4: Audit Trail Logging (Security Layer) 📝

**Feature**: Track all API operations for compliance + debugging

**Implementation**:

```java
@Aspect
@Component
public class AuditLoggingAspect {
    
    @Around("@annotation(Auditable)")
    public Object auditLog(ProceedingJoinPoint pjp) throws Throwable {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        String action = pjp.getSignature().getName();
        
        auditDAO.log(new AuditLog(
            user = user,
            action = action,
            timestamp = now(),
            ipAddress = getClientIp(),
            statusCode = 200/400/500
        ));
        
        return pjp.proceed();
    }
}

// Apply to sensitive endpoints
@Auditable
@PostMapping("/api/v1/sdi/trasmetti/{fatturaId}")
public ResponseEntity<> trasmetti(...) { ... }
```

**Effort**: 3-4 hours
**Files**:
- AuditLoggingAspect.java
- AuditLog.java (model)
- AuditLogDAO.java

---

### PART 5: Test Coverage Expansion (80%+) 🧪

**Current State**: 28+ tests (~40% coverage)  
**Target**: 80%+ coverage (60+ tests)

**Expand Tests**:

#### 5.1 Service Layer Tests (20+ new)
```
SdiServiceTest expansion:
├── testGenerateFatturaXmlWithNullCliente()
├── testSendXmlToSdiFailure() - HTTP 500
├── testPollSdiNotificationTimeout()
├── testRetryLogicExponentialBackoff()
└── [16+ more edge cases]

NotificationPollingServiceTest:
├── testPollPendingTransmissions()
├── testTimeoutHandling()
├── testMaxRetriesExceeded()
└── [8+ more cases]
```

**Effort**: 6-8 hours

#### 5.2 DAO Layer Tests (15+ new)
```
SdiTransmissionDAOTest:
├── testFindByIdTrasmissione()
├── testFindByFatturaId()
├── testFindPendingTransmissions()
├── testGetStatistics()
└── [10+ more persistence cases]

@DataJpaTest for integration with test DB
```

**Effort**: 4-5 hours

#### 5.3 Controller Integration Tests (20+ new)
```
Expand FattureControllerMvcTest:
├── testValidationErrorFormats()
├── testErrorHandlingPath()
├── testConcurrentRequests()
├── testRateLimitingExceeded() ← requires Part 1
└── [15+ more HTTP scenarios]

PreventiviControllerMvcTest (10+ new):
├── Similar to FattureController tests
```

**Effort**: 8-10 hours

#### 5.4 Validator Tests Expansion (5+ new)
```
ValidatorTest expansion:
├── testPartitaIvaEdgeCases()
├── testEmailWithUnicodeCharacters()
├── testPhoneNumberFormats()
└── [2+ more edge cases]
```

**Effort**: 2-3 hours

**Total Testing Effort**: 20-26 hours

---

## 📊 VERSION C TIMELINE

### Day 1 (May 2, Morning)
- **3h**: Rate limiting implementation
- **3h**: CSRF protection setup
- **2h**: Initial test suite runs

### Day 1 (May 2, Afternoon) 
- **2h**: Input sanitization
- **3h**: Audit logging
- **2h**: Test expansion begins (service layer)

### Day 2 (May 3, Morning)
- **5h**: Continue test expansion (DAO + controller)
- **3h**: Security testing

### Day 2 (May 3, Afternoon)
- **5h**: Test completion and coverage reporting
- **2h**: Documentation + final validation
- **1h**: Code review + merge

**Total Duration**: ~26-30 hours (3-4 days for 1 developer)

---

## 🔐 SECURITY CHECKLIST - VERSION C

- [ ] Rate limiting on all endpoints
- [ ] CSRF token generation + validation
- [ ] Input sanitization on all string fields
- [ ] SQL injection prevention
- [ ] XSS prevention
- [ ] Audit logging on sensitive operations
- [ ] Error message sanitization (no stack traces!)
- [ ] JWT validation on protected endpoints
- [ ] HTTPS/TLS enforcement
- [ ] Security headers (STS, CSP, X-Frame-Options)

---

## 📈 TEST COVERAGE TARGETS - VERSION C

| Layer | Current | Target | New Tests |
|-------|---------|--------|-----------|
| Unit Tests | 15 | 40 | +25 |
| DAO Tests | 0 | 15 | +15 |
| Integration | 13 | 25 | +12 |
| Validator | 15 | 20 | +5 |
| **Total** | **28** | **60+** | **+57** |

**Coverage Target**: 80%+ (from 40%)

---

## 🚀 DELIVERABLES - VERSION C

### Code (400+ LOC new)
1. RateLimitingService.java + Aspect (200 LOC)
2. SecurityConfig updates (50 LOC)
3. InputSanitizer.java (80 LOC)
4. AuditLoggingAspect.java (100 LOC)

### Tests (1,500+ LOC new)
1. Service layer tests (400 LOC)
2. DAO layer tests (350 LOC)
3. Controller integration tests (500 LOC)
4. Validator expansion (150 LOC)

### Documentation
1. Security implementation guide
2. Test coverage report
3. Deployment security checklist

---

## ✅ ACCEPTANCE CRITERIA - VERSION C

### Security
- ✅ No unprotected endpoints
- ✅ Rate limiting prevents abuse
- ✅ CSRF tokens correctly validated
- ✅ All inputs sanitized
- ✅ Audit trail logs all operations
- ✅ Error messages don't leak info

### Testing
- ✅ 80%+ code coverage achieved
- ✅ All edge cases tested
- ✅ DAO persistence tests passing
- ✅ MVC integration tests passing
- ✅ Zero security vulnerabilities

### Documentation
- ✅ Security configuration documented
- ✅ Test coverage report generated
- ✅ Deployment guide updated

---

## 📦 OPTIONAL ENHANCEMENTS (If time permits)

1. **API Documentation**
   - Swagger/OpenAPI integration
   - Endpoint documentation
   - Example requests/responses

2. **Performance Optimizations**
   - N+1 query fixes
   - Caching strategy (Redis)
   - Database indexing

3. **Monitoring**
   - Metrics collection (Micrometer)
   - Health check endpoints
   - Prometheus integration

---

## 🏁 POST-VERSION C STATUS

### Production Readiness
- Core functionality: ✅ 100%
- Security: ✅ 95% (missing only monitoring)
- Testing: ✅ 80%+
- Documentation: ✅ 90%

### Timeline to Production
- Security: Complete ✅
- Testing: Complete ✅
- Staging: 1 week (UAT)
- Production: 1 week (monitoring setup)
- **Total**: 2 weeks to production

---

## 🎯 RECOMMENDED STARTING POINT

**Step 1**: Rate limiting (3-4 hours, most impactful for security)  
**Step 2**: CSRF protection (2-3 hours)  
**Step 3**: Input sanitization (2-3 hours)  
**Step 4**: Audit logging (3-4 hours)  
**Step 5**: Test expansion (20-26 hours, can be done in parallel)  

**First 11 hours**: Security layer complete  
**Next 20-26 hours**: Test coverage to 80%+

---

**Ready to begin Version C?** 

Recommend starting with **Rate Limiting** (highest security impact) followed by **Test Expansion** (highest coverage impact).

Let's go! 🚀
