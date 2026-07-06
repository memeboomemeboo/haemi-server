package com.memeboo2.haemi.m3.infrastructure.ai;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.PersonTag;
import com.memeboo2.haemi.m1.domain.model.album.PhotoFile;
import com.memeboo2.haemi.m1.domain.model.album.PhotoMetadata;
import com.memeboo2.haemi.m3.domain.model.training.QuestionType;
import com.memeboo2.haemi.m3.domain.model.training.TrainingQuestion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlbumCognitiveQuestionGeneratorAdapterTest {

    private final AlbumCognitiveQuestionGeneratorAdapter generator =
            new AlbumCognitiveQuestionGeneratorAdapter();

    @Test
    void generatesThreeToFiveQuestionsFromAlbumAndDifficulty() {
        Album album = albumWithPhotos(5);

        assertThat(generator.generate(album, 1)).hasSize(3);
        assertThat(generator.generate(album, 3)).hasSize(4);
        assertThat(generator.generate(album, 5)).hasSize(5);
    }

    @Test
    void usesAllSixTypesWithoutAdjacentDuplicatesAndIncludesAlbumPhotos() {
        Album album = albumWithPhotos(5);
        EnumSet<QuestionType> generatedTypes = EnumSet.noneOf(QuestionType.class);

        for (int level = 1; level <= 5; level++) {
            List<TrainingQuestion> questions = generator.generate(album, level);
            generatedTypes.addAll(questions.stream().map(TrainingQuestion::getType).toList());

            for (int index = 1; index < questions.size(); index++) {
                assertThat(questions.get(index).getType())
                        .isNotEqualTo(questions.get(index - 1).getType());
            }
        }

        List<TrainingQuestion> hardest = generator.generate(album, 5);
        assertThat(generatedTypes).containsExactlyInAnyOrder(QuestionType.values());
        assertThat(hardest).anyMatch(question -> question.getPhotoId() != null);
        assertThat(hardest)
                .noneMatch(question -> question.getPrompt().contains("UUID"));
    }

    @Test
    void generatesQuestionsWhenPhotoMetadataIsMissing() {
        Album album = Album.create("elder-profile-1", "group-1", "owner-1");
        for (int index = 0; index < 5; index++) {
            album.addPhoto(
                    PhotoFile.of("photos/" + index, "photo.jpg", "image/jpeg", 1024),
                    null,
                    "hash-" + index,
                    "owner-1"
            );
        }

        assertThat(generator.generate(album, 2))
                .hasSize(3)
                .allMatch(question -> !question.getPrompt().isBlank());
    }

    private Album albumWithPhotos(int count) {
        Album album = Album.create("elder-profile-1", "group-1", "owner-1");
        for (int index = 0; index < count; index++) {
            var photo = album.addPhoto(
                    PhotoFile.of(
                            "photos/" + index,
                            "photo-" + index + ".jpg",
                            "image/jpeg",
                            1024
                    ),
                    PhotoMetadata.of(LocalDateTime.of(1980 + index, 5, 1, 12, 0), null, null),
                    "hash-" + index,
                    "owner-1"
            );
            album.updatePhotoMemo(photo.getPhotoId(), (1980 + index) + "년", "서울", "가족 나들이");
            album.tagPersonsOnPhoto(photo.getPhotoId(), List.of(PersonTag.of("family-1", "해미")));
        }
        return album;
    }
}
