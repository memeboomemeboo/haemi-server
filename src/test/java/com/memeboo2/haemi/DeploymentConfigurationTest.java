package com.memeboo2.haemi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 배포 매니페스트 배선 가드 (#92).
 *
 * <p>#92는 Java 코드가 아니라 compose·워크플로에 있던 버그였다. 앱은 정상 기동하고 API도 200을
 * 돌려주는데 자격증명만 도달하지 않아, 어떤 단위 테스트로도 잡히지 않았다.
 *
 * <p>그래서 매니페스트를 직접 읽어 검증한다. application.yaml이 요구하는 환경변수가 운영까지
 * 실제로 전달되는지 확인하는 것이 목적이다.
 */
class DeploymentConfigurationTest {

    private static final Path COMPOSE = Path.of("compose.yaml");
    private static final Path DEPLOY_WORKFLOW = Path.of(".github/workflows/deploy-prod.yml");

    @ParameterizedTest(name = "compose.yaml이 {0}를 앱에 전달한다")
    @ValueSource(strings = {"FIREBASE_CREDENTIALS", "FIREBASE_PROJECT_ID", "GEMINI_API_KEY",
            "INSTITUTION_ADMIN_EMAILS"})
    @DisplayName("application.yaml이 읽는 외부 자격증명은 compose에서 앱 컨테이너로 전달된다")
    void composePassesCredentialsToApp(String variable) throws Exception {
        String appService = appServiceSection();

        assertThat(appService)
                .as("compose.yaml의 app.environment에 %s가 없으면 앱은 빈 값을 읽는다", variable)
                .contains(variable + ":");
    }

    @ParameterizedTest(name = "배포 워크플로가 {0}를 EC2로 전달한다")
    @ValueSource(strings = {"FIREBASE_CREDENTIALS", "FIREBASE_PROJECT_ID", "INSTITUTION_ADMIN_EMAILS"})
    @DisplayName("배포 워크플로가 자격증명을 .env까지 실어 나른다")
    void deployWorkflowCarriesCredentialsToEnvFile(String variable) throws Exception {
        String workflow = Files.readString(DEPLOY_WORKFLOW);

        assertThat(workflow)
                .as("%s가 ssh-action의 envs 목록에 없으면 원격 셸에 전달되지 않는다", variable)
                .containsPattern("envs:[^\\n]*" + variable);
        assertThat(workflow)
                .as("%s를 save_env로 .env에 쓰지 않으면 compose가 읽을 수 없다", variable)
                .contains("save_env " + variable);
    }

    @Test
    @DisplayName("재배포에도 남아야 하는 데이터는 볼륨에 마운트된다")
    void persistentDataDirectoriesAreMounted() throws Exception {
        String appService = appServiceSection();

        // 컨테이너 레이어에 쓰면 docker compose up 한 번에 사라진다.
        // 파일은 없는데 DB에는 경로가 남아 다운로드가 깨진다. (#93)
        assertThat(appService)
                .as("사진 업로드 경로가 볼륨에 없으면 재배포마다 원본이 사라진다")
                .contains("HAEMI_STORAGE_UPLOAD_PATH: /data/haemi/photos")
                .contains(":/data/haemi/photos");
        assertThat(appService)
                .as("리포트 PDF 경로가 볼륨에 없으면 재배포 후 다운로드가 실패한다")
                .contains("HAEMI_REPORT_PDF_OUTPUT_DIR: /data/haemi/reports")
                .contains(":/data/haemi/reports");
    }

    @Test
    @DisplayName("서비스 계정 JSON은 .env가 아니라 파일로 전달한다")
    void serviceAccountJsonIsDeliveredAsFileNotEnvLine() throws Exception {
        String workflow = Files.readString(DEPLOY_WORKFLOW);

        // 여러 줄로 저장된 Secret이 .env 한 줄 쓰기를 깨뜨리면, 뒤따르는 IMAGE_TAG 같은
        // 항목까지 오염돼 배포 전체가 조용히 망가진다.
        assertThat(workflow)
                .as("JSON 원문을 .env에 직접 쓰면 여러 줄 Secret에서 깨진다")
                .doesNotContain("save_env FIREBASE_CREDENTIALS \"$FIREBASE_CREDENTIALS\"")
                .contains("secrets/firebase-service-account.json");

        assertThat(appServiceSection())
                .as("자격증명 파일이 컨테이너 안에서 읽히려면 마운트돼야 한다")
                .contains("/run/secrets");
    }

    /**
     * compose.yaml에서 app 서비스 블록만 잘라낸다. postgres 쪽 설정과 섞이지 않게 한다.
     *
     * <p>경계는 줄 시작 기준으로 찾는다. 단순 부분 문자열로 찾으면 app 안의
     * {@code depends_on: postgres:}가 서비스 경계로 오인된다.
     */
    private String appServiceSection() throws Exception {
        List<String> lines = Files.readAllLines(COMPOSE);
        int appStart = indexOfLine(lines, "  app:");
        assertThat(appStart).as("compose.yaml에 app 서비스가 있어야 한다").isNotNegative();

        int end = lines.size();
        for (int line = appStart + 1; line < lines.size(); line++) {
            String text = lines.get(line);
            boolean siblingService = text.matches("^ {2}\\S.*:.*") && !text.startsWith("   ");
            boolean topLevelKey = text.matches("^\\S.*:.*");
            if (siblingService || topLevelKey) {
                end = line;
                break;
            }
        }
        return String.join("\n", lines.subList(appStart, end));
    }

    private int indexOfLine(List<String> lines, String exact) {
        for (int line = 0; line < lines.size(); line++) {
            if (lines.get(line).equals(exact)) {
                return line;
            }
        }
        return -1;
    }
}
