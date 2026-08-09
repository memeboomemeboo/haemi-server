package com.memeboo2.haemi.m1.application.service;

import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.NotificationPreference;
import com.memeboo2.haemi.m0.domain.port.ElderAccessPort;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import com.memeboo2.haemi.m1.application.command.CreateMemoryCommand;
import com.memeboo2.haemi.m1.application.dto.MemoryFeedResult;
import com.memeboo2.haemi.m1.application.dto.MemoryResult;
import com.memeboo2.haemi.m1.domain.model.memory.*;
import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;
import com.memeboo2.haemi.m1.domain.repository.MemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryApplicationServiceTest {

    @Mock private FamilyGroupRepository groups;
    @Mock private MemberRepository members;
    @Mock private MemoryRepository memories;
    @Mock private PhotoStoragePort storage;
    @Mock private ElderAccessPort elderAccess;

    private MemoryApplicationService service;
    private UUID groupId;
    private UUID ownerId;
    private FamilyGroup group;

    @BeforeEach
    void setUp() {
        service = new MemoryApplicationService(groups, members, memories, storage,
                new MemoryModerationService(), elderAccess);
        ownerId = UUID.randomUUID();
        group = FamilyGroup.create(ownerId, FamilyRelation.GRANDDAUGHTER, NotificationPreference.ALL);
        groupId = group.getId();
        lenient().when(groups.findById(groupId)).thenReturn(Optional.of(group));
        lenient().when(memories.save(any(Memory.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsSingleMemoryWithTextAndImage() throws Exception {
        given(storage.store(any(), eq("memory.jpg"), eq("image/jpeg"))).willReturn("image-key");
        given(storage.getAccessUrl(anyString())).willAnswer(invocation -> "/media/" + invocation.getArgument(0));

        MemoryResult result = service.create(new CreateMemoryCommand(groupId, ownerId, "1982년 여름의 바다예요.",
                MemoryVisibility.GROUP_ALL, List.of(new CreateMemoryCommand.MediaAttachment(
                MemoryMediaType.IMAGE, new ByteArrayInputStream("image".getBytes()), "memory.jpg", "image/jpeg",
                5, null))));

        assertThat(result.textContent()).isEqualTo("1982년 여름의 바다예요.");
        assertThat(result.visibility()).isEqualTo(MemoryVisibility.GROUP_ALL);
        assertThat(result.media()).singleElement().satisfies(media -> {
            assertThat(media.type()).isEqualTo(MemoryMediaType.IMAGE);
            assertThat(media.accessUrl()).isEqualTo("/media/image-key");
        });
        ArgumentCaptor<Memory> saved = ArgumentCaptor.forClass(Memory.class);
        verify(memories).save(saved.capture());
        assertThat(saved.getValue().getAuthorUserId()).isEqualTo(ownerId);
    }

    @Test
    void blocksAbusiveTextBeforeItCanBeStored() {
        assertThatThrownBy(() -> service.create(new CreateMemoryCommand(groupId, ownerId, "너는 죽어", 
                MemoryVisibility.GROUP_ALL, List.of())))
                .isInstanceOf(MemoryModerationException.class);

        verifyNoInteractions(storage);
        verify(memories, never()).save(any());
    }

    @Test
    void familyOnlyMemoryIsRejectedEvenIfRepositoryReturnsItForElderFeed() {
        UUID elderId = UUID.randomUUID();
        Memory familyOnly = Memory.create(groupId, ownerId, "가족만 보는 기록", "손녀", "GRANDDAUGHTER",
                MemoryVisibility.FAMILY_ONLY, MemoryModerationStatus.CLEAR);
        given(elderAccess.getRequired(elderId)).willReturn(new ElderAccessPort.ElderAccessSnapshot(elderId, groupId,
                com.memeboo2.haemi.m0.domain.model.ElderStatus.ACTIVE,
                com.memeboo2.haemi.m0.domain.model.ElderAccessMode.A, 2));
        given(memories.findElderFeed(eq(groupId), any())).willReturn(new PageImpl<>(List.of(familyOnly)));

        MemoryFeedResult result = service.getElderFeed(elderId, 0, 20);

        assertThat(result.memories()).isEmpty();
        verify(memories).findElderFeed(eq(groupId), any());
    }
}
