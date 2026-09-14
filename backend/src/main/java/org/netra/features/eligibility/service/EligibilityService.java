package org.netra.features.eligibility.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.SessionExpiredException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.eligibility.dto.*;
import org.netra.features.eligibility.entity.*;
import org.netra.features.eligibility.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EligibilityService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityService.class);

    private final EligibilitySessionRepository sessionRepository;
    private final EligibilityAnswerRepository answerRepository;
    private final EligibilityQuestionRepository questionRepository;
    private final EligibilityRuleEngine ruleEngine;
    private final AuditService auditService;
    private final String activeRuleVersion;
    private final long sessionTtlMinutes;

    public EligibilityService(
            EligibilitySessionRepository sessionRepository,
            EligibilityAnswerRepository answerRepository,
            EligibilityQuestionRepository questionRepository,
            EligibilityRuleEngine ruleEngine,
            AuditService auditService,
            @Value("${netra.eligibility.active-rule-version:INDIA-NBTC-2026-01}") String activeRuleVersion,
            @Value("${netra.eligibility.session-ttl-minutes:60}") long sessionTtlMinutes) {
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.ruleEngine = ruleEngine;
        this.auditService = auditService;
        this.activeRuleVersion = activeRuleVersion;
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request, String clientIp, String userAgent) {
        Optional<UUID> authenticatedUserId = SecurityUtils.getCurrentUserId();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(sessionTtlMinutes, ChronoUnit.MINUTES);

        EligibilitySession session = new EligibilitySession(
                authenticatedUserId.orElse(null),
                activeRuleVersion,
                expiresAt
        );

        String capabilityToken = null;
        if (authenticatedUserId.isEmpty()) {
            capabilityToken = SecurityUtils.generateSecureToken();
            session.setCapabilityTokenHash(SecurityUtils.sha256Hex(capabilityToken));
        }

        session = sessionRepository.save(session);

        auditService.logEvent(
                "SESSION_STARTED",
                session.getUserId(),
                session.getId(),
                session.getRuleVersion(),
                null,
                clientIp,
                userAgent
        );

        return new SessionResponse(session.getId(), session.getRuleVersion(), session.getStatus(), session.getExpiresAt(), capabilityToken);
    }

    @Transactional(readOnly = true)
    public QuestionnaireResponse getActiveQuestions() {
        List<EligibilityQuestion> questions = questionRepository
                .findByVersionAndActiveOrderByStepNumberAscSortOrderAsc(activeRuleVersion, true);

        Map<Integer, List<EligibilityQuestion>> grouped = questions.stream()
                .collect(Collectors.groupingBy(EligibilityQuestion::getStepNumber, TreeMap::new, Collectors.toList()));

        List<QuestionSectionDto> sections = new ArrayList<>();
        Map<Integer, String[]> sectionMeta = Map.of(
                1, new String[]{"Basic Information", "Age, weight, and biological criteria"},
                2, new String[]{"Recent Donation", "Prior donation history and recovery interval"},
                3, new String[]{"Current Health", "Symptom check and current medications"},
                4, new String[]{"Donation Safety", "Procedures, surgeries, and clinical safety"},
                5, new String[]{"Pre-Donation Check", "Rest, nutrition, and day-of hydration"}
        );

        for (Map.Entry<Integer, List<EligibilityQuestion>> entry : grouped.entrySet()) {
            int stepNum = entry.getKey();
            String[] meta = sectionMeta.getOrDefault(stepNum, new String[]{"Section " + stepNum, "Questions"});
            List<QuestionItemDto> items = entry.getValue().stream().map(q -> new QuestionItemDto(
                    q.getQuestionKey(),
                    q.getStepNumber(),
                    q.getCategory().name(),
                    q.getQuestionText(),
                    q.getHelpText(),
                    q.getQuestionType(),
                    parseOptions(q.getOptionsJson()),
                    q.getValidationJson()
            )).collect(Collectors.toList());

            sections.add(new QuestionSectionDto(stepNum, meta[0], meta[1], items));
        }

        return new QuestionnaireResponse(activeRuleVersion, sections.size() + 1, sections);
    }

    @Transactional
    public int submitAnswers(UUID sessionId, SubmitAnswersRequest request) {
        return submitAnswers(sessionId, request, null);
    }

    @Transactional
    public int submitAnswers(UUID sessionId, SubmitAnswersRequest request, String capabilityToken) {
        EligibilitySession session = validateSessionAccess(sessionId, capabilityToken);

        int savedCount = 0;
        for (AnswerItemDto dto : request.getAnswers()) {
            Optional<EligibilityAnswer> existing = answerRepository.findBySessionIdAndQuestionKey(sessionId, dto.getQuestionKey());
            if (existing.isPresent()) {
                existing.get().setAnswerValue(dto.getValue());
                answerRepository.save(existing.get());
            } else {
                EligibilityAnswer answer = new EligibilityAnswer(session, dto.getQuestionKey(), dto.getValue());
                answerRepository.save(answer);
            }
            savedCount++;
        }

        return savedCount;
    }

    @Transactional
    public EligibilityResultResponse evaluateSession(UUID sessionId, CheckEligibilityRequest request, String clientIp, String userAgent) {
        return evaluateSession(sessionId, request, clientIp, userAgent, null);
    }

    @Transactional
    public EligibilityResultResponse evaluateSession(UUID sessionId, CheckEligibilityRequest request, String clientIp, String userAgent, String capabilityToken) {
        EligibilitySession session = validateSessionAccess(sessionId, capabilityToken);

        List<EligibilityAnswer> answersList = answerRepository.findBySessionId(sessionId);
        Map<String, String> answersMap = answersList.stream()
                .collect(Collectors.toMap(EligibilityAnswer::getQuestionKey, EligibilityAnswer::getAnswerValue, (a, b) -> b));

        EligibilityResultResponse result = ruleEngine.evaluate(sessionId, session.getRuleVersion(), answersMap, LocalDate.now());

        session.setResult(result.getResult());
        session.setEstimatedEligibleDate(result.getEstimatedNextEligibleDate());
        session.setCompletedAt(Instant.now());
        session.setStatus(SessionStatus.COMPLETED);
        sessionRepository.save(session);

        auditService.logEvent(
                "SESSION_COMPLETED",
                session.getUserId(),
                session.getId(),
                session.getRuleVersion(),
                result.getResult().name(),
                clientIp,
                userAgent
        );

        return result;
    }

    @Transactional(readOnly = true)
    public EligibilityResultResponse getSessionResult(UUID sessionId) {
        return getSessionResult(sessionId, null);
    }

    @Transactional(readOnly = true)
    public EligibilityResultResponse getSessionResult(UUID sessionId, String capabilityToken) {
        EligibilitySession session = validateSessionAccess(sessionId, capabilityToken);

        if (session.getResult() == null) {
            throw new ResourceNotFoundException("No evaluation has been performed for this session yet.");
        }

        EligibilityResultResponse response = new EligibilityResultResponse();
        response.setSessionId(session.getId());
        response.setRuleVersion(session.getRuleVersion());
        response.setResult(session.getResult());
        response.setDisclaimer(EligibilityRuleEngine.DISCLAIMER_TEXT);
        response.setEstimatedNextEligibleDate(session.getEstimatedEligibleDate());

        switch (session.getResult()) {
            case LIKELY_ELIGIBLE -> {
                response.setTitle("You appear eligible to donate blood");
                response.setMessage("Based on your answers, you meet the preliminary criteria for whole-blood donation.");
                response.setNextActions(List.of(
                        new NextActionDto("FIND_BLOOD_BANKS", "Find Nearby Blood Banks", "/blood-banks"),
                        new NextActionDto("FIND_EVENTS", "Find Donation Events", "/events"),
                        new NextActionDto("REGISTER_DONATION", "Register for Donation", "/register")
                ));
            }
            case TEMPORARY_DEFERRAL -> {
                response.setTitle("You may need to wait before donating");
                response.setMessage("Based on your answers, one or more temporary deferral criteria apply.");
                List<NextActionDto> actions = new ArrayList<>();
                if (session.getEstimatedEligibleDate() != null) {
                    actions.add(new NextActionDto("SET_REMINDER", "Set Reminder for Next Eligible Date", "/reminder"));
                }
                actions.add(new NextActionDto("FIND_BLOOD_BANKS", "Find Blood Banks", "/blood-banks"));
                response.setNextActions(actions);
            }
            case MEDICAL_REVIEW_REQUIRED -> {
                response.setTitle("We can't determine your eligibility from the app alone");
                response.setMessage("Please speak with the blood bank or qualified medical staff before donating.");
                response.setNextActions(List.of(
                        new NextActionDto("FIND_BLOOD_BANKS", "Find Nearby Blood Banks", "/blood-banks")
                ));
            }
            case INSUFFICIENT_INFORMATION -> {
                response.setTitle("More information needed");
                response.setMessage("Some required pre-screening information was incomplete.");
                response.setNextActions(List.of(
                        new NextActionDto("COMPLETE_SURVEY", "Complete Questionnaire", "/eligibility/flow")
                ));
            }
        }

        return response;
    }

    public RuleVersionResponse getActiveRuleVersion() {
        return new RuleVersionResponse(
                activeRuleVersion,
                Instant.parse("2026-01-01T00:00:00Z"),
                "National Blood Transfusion Council (NBTC) Guidelines & Schedule F Part XII-B",
                "Government of India / National Blood Transfusion Council"
        );
    }

    private EligibilitySession validateSessionAccess(UUID sessionId, String capabilityToken) {
        EligibilitySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Eligibility session not found: " + sessionId));

        if (session.isExpired()) {
            throw new SessionExpiredException("Eligibility session has expired. Please start a new check.");
        }

        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
        if (session.getUserId() != null) {
            if (currentUserId.isEmpty() || !session.getUserId().equals(currentUserId.get())) {
                throw new UnauthorizedSessionAccessException("You do not have permission to access or modify this eligibility session.");
            }
        } else {
            // Anonymous session: MUST fail closed if capability token or token hash is missing or invalid
            if (session.getCapabilityTokenHash() == null || session.getCapabilityTokenHash().isBlank() ||
                    capabilityToken == null || capabilityToken.isBlank() ||
                    !session.getCapabilityTokenHash().equalsIgnoreCase(SecurityUtils.sha256Hex(capabilityToken.trim()))) {
                throw new UnauthorizedSessionAccessException("Valid capability token is required to access this anonymous eligibility session.");
            }
        }

        return session;
    }

    private List<QuestionOptionDto> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return Collections.emptyList();
        }
        List<QuestionOptionDto> options = new ArrayList<>();
        if (optionsJson.contains("MALE")) {
            options.add(new QuestionOptionDto("MALE", "Male"));
            options.add(new QuestionOptionDto("FEMALE", "Female"));
            options.add(new QuestionOptionDto("OTHER", "Other / Prefer not to say"));
        }
        return options;
    }
}
