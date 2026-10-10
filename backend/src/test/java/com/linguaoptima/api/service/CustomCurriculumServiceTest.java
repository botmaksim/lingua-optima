/**
 * @file CustomCurriculumServiceTest.java
 * @brief Unit tests for CustomCurriculumService covering rule and vocabulary quotas and CRUD operations.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.config.PricingProperties;
import com.linguaoptima.api.domain.CustomCurriculumEntry;
import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.request.CustomCurriculumRequest;
import com.linguaoptima.api.dto.response.CustomCurriculumResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.QuotaExceededException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.CustomCurriculumRepository;
import com.linguaoptima.api.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomCurriculumServiceTest {

    @Mock
    private CustomCurriculumRepository customCurriculumRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Spy
    private PricingProperties pricingProperties = new PricingProperties();

    @InjectMocks
    private CustomCurriculumService customCurriculumService;

    private User teacher;
    private User otherTeacher;

    @BeforeEach
    void setUp() {
        teacher = User.builder()
            .id(UUID.randomUUID())
            .email("teacher@test.com")
            .fullName("Teacher Alice")
            .role(Role.TEACHER)
            .build();

        otherTeacher = User.builder()
            .id(UUID.randomUUID())
            .email("other@test.com")
            .fullName("Teacher Bob")
            .role(Role.TEACHER)
            .build();
    }

    @Test
    void testGetCustomCurriculum_AllAndFiltered() {
        CustomCurriculumEntry e1 = CustomCurriculumEntry.builder()
            .id(UUID.randomUUID())
            .user(teacher)
            .title("Passive Voice Nuances")
            .cefrLevel(CefrLevel.B2)
            .curriculumType("RULE")
            .content("Always use by-agent when relevant")
            .createdAt(LocalDateTime.now())
            .build();

        when(customCurriculumRepository.findByUserIdOrderByCreatedAtDesc(teacher.getId()))
            .thenReturn(List.of(e1));
        when(customCurriculumRepository.findByUserIdAndCurriculumTypeOrderByCreatedAtDesc(teacher.getId(), "RULE"))
            .thenReturn(List.of(e1));

        List<CustomCurriculumResponse> all = customCurriculumService.getCustomCurriculum(teacher, null);
        assertEquals(1, all.size());
        assertEquals("Passive Voice Nuances", all.get(0).getTitle());

        List<CustomCurriculumResponse> filtered = customCurriculumService.getCustomCurriculum(teacher, "RULE");
        assertEquals(1, filtered.size());
        assertEquals("RULE", filtered.get(0).getCurriculumType());
    }

    @Test
    void testCreateCustomRule_Success() {
        when(subscriptionRepository.findByUser(teacher))
            .thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(customCurriculumRepository.countByUserIdAndCurriculumType(teacher.getId(), "RULE"))
            .thenReturn(0L);

        CustomCurriculumEntry saved = CustomCurriculumEntry.builder()
            .id(UUID.randomUUID())
            .user(teacher)
            .title("Conditionals Rule")
            .cefrLevel(CefrLevel.B1)
            .curriculumType("RULE")
            .content("Rule content")
            .createdAt(LocalDateTime.now())
            .build();
        when(customCurriculumRepository.save(any(CustomCurriculumEntry.class))).thenReturn(saved);

        CustomCurriculumRequest request = CustomCurriculumRequest.builder()
            .title("Conditionals Rule")
            .cefrLevel(CefrLevel.B1)
            .curriculumType("RULE")
            .content("Rule content")
            .build();

        CustomCurriculumResponse res = customCurriculumService.createCustomCurriculum(request, teacher);
        assertNotNull(res);
        assertEquals("Conditionals Rule", res.getTitle());
        assertEquals("RULE", res.getCurriculumType());
    }

    @Test
    void testCreateCustomVocab_Success() {
        when(subscriptionRepository.findByUser(teacher))
            .thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.PREMIUM).build()));
        when(customCurriculumRepository.countByUserIdAndCurriculumType(teacher.getId(), "VOCABULARY"))
            .thenReturn(1L);

        CustomCurriculumEntry saved = CustomCurriculumEntry.builder()
            .id(UUID.randomUUID())
            .user(teacher)
            .title("Legal English Vocab")
            .cefrLevel(CefrLevel.C1)
            .curriculumType("VOCABULARY")
            .content("plaintiff, defendant, subpoena")
            .createdAt(LocalDateTime.now())
            .build();
        when(customCurriculumRepository.save(any(CustomCurriculumEntry.class))).thenReturn(saved);

        CustomCurriculumRequest request = CustomCurriculumRequest.builder()
            .title("Legal English Vocab")
            .cefrLevel(CefrLevel.C1)
            .curriculumType("VOCABULARY")
            .content("plaintiff, defendant, subpoena")
            .build();

        CustomCurriculumResponse res = customCurriculumService.createCustomCurriculum(request, teacher);
        assertNotNull(res);
        assertEquals("Legal English Vocab", res.getTitle());
        assertEquals("VOCABULARY", res.getCurriculumType());
    }

    @Test
    void testCreateCustomRule_QuotaExceeded() {
        when(subscriptionRepository.findByUser(teacher))
            .thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        // Free tier allows maxCustomRules = 1
        when(customCurriculumRepository.countByUserIdAndCurriculumType(teacher.getId(), "RULE"))
            .thenReturn(1L);

        CustomCurriculumRequest request = CustomCurriculumRequest.builder()
            .title("Another Rule")
            .cefrLevel(CefrLevel.A2)
            .curriculumType("RULE")
            .content("Some rule content")
            .build();

        assertThrows(QuotaExceededException.class, () ->
            customCurriculumService.createCustomCurriculum(request, teacher)
        );
    }

    @Test
    void testCreateCustomVocab_QuotaExceeded() {
        when(subscriptionRepository.findByUser(teacher))
            .thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        // Free tier allows maxCustomVocabularySets = 1
        when(customCurriculumRepository.countByUserIdAndCurriculumType(teacher.getId(), "VOCABULARY"))
            .thenReturn(1L);

        CustomCurriculumRequest request = CustomCurriculumRequest.builder()
            .title("Another Vocab")
            .cefrLevel(CefrLevel.A2)
            .curriculumType("VOCABULARY")
            .content("word1, word2")
            .build();

        assertThrows(QuotaExceededException.class, () ->
            customCurriculumService.createCustomCurriculum(request, teacher)
        );
    }

    @Test
    void testCreateCustomCurriculum_UnsupportedType() {
        when(subscriptionRepository.findByUser(teacher))
            .thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));

        CustomCurriculumRequest request = CustomCurriculumRequest.builder()
            .title("Invalid")
            .cefrLevel(CefrLevel.B1)
            .curriculumType("UNKNOWN")
            .content("content")
            .build();

        assertThrows(IllegalArgumentException.class, () ->
            customCurriculumService.createCustomCurriculum(request, teacher)
        );
    }

    @Test
    void testDeleteCustomCurriculum_Success() {
        UUID id = UUID.randomUUID();
        CustomCurriculumEntry entry = CustomCurriculumEntry.builder()
            .id(id)
            .user(teacher)
            .title("To Delete")
            .build();

        when(customCurriculumRepository.findById(id)).thenReturn(Optional.of(entry));

        customCurriculumService.deleteCustomCurriculum(id, teacher);
        verify(customCurriculumRepository).delete(entry);
    }

    @Test
    void testDeleteCustomCurriculum_NotFound() {
        UUID id = UUID.randomUUID();
        when(customCurriculumRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            customCurriculumService.deleteCustomCurriculum(id, teacher)
        );
    }

    @Test
    void testDeleteCustomCurriculum_ForbiddenForNonOwner() {
        UUID id = UUID.randomUUID();
        CustomCurriculumEntry entry = CustomCurriculumEntry.builder()
            .id(id)
            .user(teacher)
            .title("Protected")
            .build();

        when(customCurriculumRepository.findById(id)).thenReturn(Optional.of(entry));

        assertThrows(ForbiddenException.class, () ->
            customCurriculumService.deleteCustomCurriculum(id, otherTeacher)
        );
    }
}
