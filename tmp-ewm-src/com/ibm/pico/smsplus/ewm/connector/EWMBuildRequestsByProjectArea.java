package com.ibm.pico.smsplus.ewm.connector;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.team.build.client.ITeamBuildClient;
import com.ibm.team.build.common.model.IBuildRequest;
import com.ibm.team.build.common.model.IBuildResult;
import com.ibm.team.build.common.model.IBuildResultContribution;
import com.ibm.team.build.common.model.IBuildResultHandle;
import com.ibm.team.build.internal.common.ITeamBuildService;
import com.ibm.team.build.internal.common.model.dto.IBuildResultRecord;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.client.internal.TeamRepository;
import com.ibm.team.repository.common.IContent;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.query.IItemQueryPage;
import com.ibm.team.repository.common.query.IQueryPage;

public class EWMBuildRequestsByProjectArea {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        String projectAreaName = args.length > 0 ? args[0] : "Vendita IT";
        int pageSize = args.length > 1 ? Integer.parseInt(args[1]) : 100;

        TeamPlatform.startup();

        try {
            ITeamRepository repo = TeamPlatform.getTeamRepositoryService()
                    .getTeamRepository(EwmConfiguration.REPOSITORY_ADDRESS);

            repo.registerLoginHandler(new ILoginHandler() {
                @Override
                public ILoginInfo challenge(ITeamRepository repository) {
                    return new ILoginInfo() {
                        @Override
                        public String getUserId() {
                            return EwmConfiguration.ADMIN_USER;
                        }

                        @Override
                        public String getPassword() {
                            return EwmConfiguration.ADMIN_PASSWORD;
                        }
                    };
                }
            });

            repo.login(null);

            IProjectArea projectArea = findProjectAreaByName(repo, projectAreaName);
            if (projectArea == null) {
                throw new IllegalArgumentException("ProjectArea non trovata: " + projectAreaName);
            }

            List<BuildRequestInfo> buildRequests = listBuildRequests(repo, projectAreaName, pageSize, new NullProgressMonitor());

            System.out.println("ProjectArea: " + projectArea.getName());
            System.out.println("BuildRequest trovate: " + buildRequests.size());
            System.out.println();

            buildRequests.stream()
                    .sorted(Comparator.comparing(BuildRequestInfo::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .forEach(EWMBuildRequestsByProjectArea::print);

            BuildRequestInfo latestBuildRequest = buildRequests.stream()
                    .max(Comparator.comparing(BuildRequestInfo::createdAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);

            if (latestBuildRequest != null) {
                System.out.println();
                System.out.println("Dettaglio ultima BuildRequest:");
                printDetail(latestBuildRequest);

                Path extractedLog = extractAttachedLog(repo, latestBuildRequest, new NullProgressMonitor());
                System.out.println("Log estratto: " + (extractedLog != null ? extractedLog.toAbsolutePath() : "nessun log allegato trovato"));
            }
        } finally {
            TeamPlatform.shutdown();
        }
    }

    public static List<BuildRequestInfo> listBuildRequests(
            ITeamRepository repo,
            String projectAreaName,
            int pageSize,
            IProgressMonitor monitor) throws Exception {

        IProjectArea projectArea = findProjectAreaByName(repo, projectAreaName);
        if (projectArea == null) {
            throw new IllegalArgumentException("ProjectArea non trovata: " + projectAreaName);
        }

        return loadBuildRequests(repo, projectArea, pageSize, monitor);
    }

    private static IProjectArea findProjectAreaByName(ITeamRepository repo, String projectAreaName) throws Exception {
        IProcessClientService processClient = (IProcessClientService) repo.getClientLibrary(IProcessClientService.class);
        java.net.URI uri = java.net.URI.create(java.net.URLEncoder.encode(projectAreaName, "UTF-8").replace("+", "%20"));
        IProjectArea projectArea = (IProjectArea) processClient.findProcessArea(uri, null, null);
        return projectArea != null && !projectArea.isArchived() ? projectArea : null;
    }

    private static List<BuildRequestInfo> loadBuildRequests(
            ITeamRepository repo,
            IProjectArea projectArea,
            int pageSize,
            IProgressMonitor monitor) throws TeamRepositoryException {

        ITeamBuildService buildService = (ITeamBuildService) ((TeamRepository) repo)
                .getServiceInterface(ITeamBuildService.class);
        ITeamBuildClient buildClient = (ITeamBuildClient) repo.getClientLibrary(ITeamBuildClient.class);

        IItemQueryPage currentPage = buildService.getQueryPageForBuildResultsForProjectArea((IProjectAreaHandle) projectArea, null);
        Map<String, BuildRequestInfo> requestsById = new LinkedHashMap<>();

        while (currentPage != null && currentPage.getSize() > 0) {
            collectPage(repo, buildService, currentPage, requestsById, projectArea.getName(), monitor);

            if (!currentPage.hasNext()) {
                break;
            }

            IQueryPage nextPage = buildClient.fetchPage(currentPage.getToken(), currentPage.getNextStartPosition(), pageSize, monitor);
            if (!(nextPage instanceof IItemQueryPage itemQueryPage)) {
                break;
            }
            currentPage = itemQueryPage;
        }

        return new ArrayList<>(requestsById.values());
    }

    private static void collectPage(
            ITeamRepository repo,
            ITeamBuildService buildService,
            IItemQueryPage page,
            Map<String, BuildRequestInfo> requestsById,
            String projectAreaName,
            IProgressMonitor monitor) throws TeamRepositoryException {

        List<IBuildResultHandle> resultHandleList = new ArrayList<>();
        for (Object handle : page.getItemHandles()) {
            if (handle instanceof IBuildResultHandle buildResultHandle) {
                resultHandleList.add(buildResultHandle);
            }
        }
        IBuildResultHandle[] resultHandles = resultHandleList.toArray(new IBuildResultHandle[0]);

        if (resultHandles.length == 0) {
            return;
        }

        IBuildResultRecord[] records = buildService.getBuildResultRecordsForBuildResults(resultHandles, null);
        for (IBuildResultRecord record : records) {
            IBuildResult buildResult = record.getBuildResult();
            IContributor requestor = record.getRequestor();

            for (IBuildRequest request : record.getBuildRequests()) {
                String requestUuid = request.getItemId().getUuidValue();

                BuildRequestInfo info = requestsById.computeIfAbsent(requestUuid, ignored -> new BuildRequestInfo());
                info.requestUuid = requestUuid;
                info.projectAreaName = projectAreaName;
                info.buildDefinitionId = request.getBuildDefinitionInstance() != null
                        ? request.getBuildDefinitionInstance().getBuildDefinitionId()
                        : null;
                info.createdAt = request.getCreated();
                info.processed = request.isProcessed();
                info.requestorUserId = requestor != null ? requestor.getUserId() : null;

                if (buildResult != null) {
                    info.buildResultHandle = (IBuildResultHandle) buildResult.getItemHandle();
                    info.buildResultUuid = buildResult.getItemId().getUuidValue();
                    info.buildLabel = buildResult.getLabel();
                    info.buildState = buildResult.getState() != null ? buildResult.getState().toString() : null;
                    info.buildStatus = buildResult.getStatus() != null ? buildResult.getStatus().toString() : null;
                    info.buildStartTime = buildResult.getBuildStartTime() > 0
                            ? new java.util.Date(buildResult.getBuildStartTime())
                            : null;
                    info.buildTimeTakenMs = buildResult.getBuildTimeTaken();
                }

                if (request.getHandler() != null) {
                    info.buildEngineUuid = request.getHandler().getItemId().getUuidValue();
                }
                if (request.getHandlingContributor() != null) {
                    IContributor handlingContributor = (IContributor) repo.itemManager().fetchCompleteItem(
                            request.getHandlingContributor(),
                            IItemManager.DEFAULT,
                            monitor);
                    info.handlingUserId = handlingContributor.getUserId();
                }
            }
        }
    }

    private static void print(BuildRequestInfo info) {
        System.out.printf(
                "request=%s | definition=%s | created=%s | processed=%s | status=%s | state=%s | label=%s | requestor=%s | handler=%s%n",
                nvl(info.requestUuid),
                nvl(info.buildDefinitionId),
                format(info.createdAt),
                info.processed,
                nvl(info.buildStatus),
                nvl(info.buildState),
                nvl(info.buildLabel),
                nvl(info.requestorUserId),
                nvl(firstNonNull(info.handlingUserId, info.buildEngineUuid)));
    }

    private static void printDetail(BuildRequestInfo info) {
        System.out.println("requestUuid=" + nvl(info.requestUuid));
        System.out.println("projectAreaName=" + nvl(info.projectAreaName));
        System.out.println("buildDefinitionId=" + nvl(info.buildDefinitionId));
        System.out.println("createdAt=" + format(info.createdAt));
        System.out.println("processed=" + info.processed);
        System.out.println("requestorUserId=" + nvl(info.requestorUserId));
        System.out.println("handlingUserId=" + nvl(info.handlingUserId));
        System.out.println("buildEngineUuid=" + nvl(info.buildEngineUuid));
        System.out.println("buildResultUuid=" + nvl(info.buildResultUuid));
        System.out.println("buildLabel=" + nvl(info.buildLabel));
        System.out.println("buildState=" + nvl(info.buildState));
        System.out.println("buildStatus=" + nvl(info.buildStatus));
        System.out.println("buildStartTime=" + format(info.buildStartTime));
        System.out.println("buildTimeTakenMs=" + info.buildTimeTakenMs);
    }

    private static Path extractAttachedLog(
            ITeamRepository repo,
            BuildRequestInfo buildRequestInfo,
            IProgressMonitor monitor) throws TeamRepositoryException, IOException {

        if (buildRequestInfo.buildResultHandle == null) {
            return null;
        }

        ITeamBuildClient buildClient = (ITeamBuildClient) repo.getClientLibrary(ITeamBuildClient.class);
        IBuildResultContribution[] contributions = buildClient.getBuildResultContributions(buildRequestInfo.buildResultHandle, (String[]) null, monitor);

        for (IBuildResultContribution contribution : contributions) {
            if (!IBuildResultContribution.LOG_EXTENDED_CONTRIBUTION_ID.equals(contribution.getExtendedContributionTypeId())) {
                continue;
            }

            IContent content = contribution.getExtendedContributionData();
            if (content == null) {
                continue;
            }

            String baseName = firstNonBlank(
                    contribution.getExtendedContributionProperty(IBuildResultContribution.PROPERTY_NAME_FILE_NAME),
                    contribution.getLabel(),
                    "build-log-" + buildRequestInfo.requestUuid + ".log");
            String sanitizedBaseName = sanitizeFileName(baseName);
            Path targetFile = Path.of(System.getProperty("java.io.tmpdir"))
                    .resolve(sanitizedBaseName);

            try (InputStream inputStream = repo.contentManager().retrieveContentStream(content, monitor)) {
                Files.copy(inputStream, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return targetFile;
        }

        return null;
    }

    private static String format(java.util.Date date) {
        return date == null ? "-" : DATE_FORMAT.format(date);
    }

    private static String nvl(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static final class BuildRequestInfo {
        private String requestUuid;
        private String projectAreaName;
        private String buildDefinitionId;
        private java.util.Date createdAt;
        private boolean processed;
        private String requestorUserId;
        private String handlingUserId;
        private String buildEngineUuid;
        private String buildResultUuid;
        private String buildLabel;
        private String buildState;
        private String buildStatus;
        private java.util.Date buildStartTime;
        private long buildTimeTakenMs;
        private IBuildResultHandle buildResultHandle;

        public java.util.Date createdAt() {
            return Objects.requireNonNullElse(buildStartTime, createdAt);
        }

        public String getRequestUuid() {
            return requestUuid;
        }

        public String getProjectAreaName() {
            return projectAreaName;
        }

        public String getBuildDefinitionId() {
            return buildDefinitionId;
        }

        public java.util.Date getCreatedAt() {
            return createdAt;
        }

        public boolean isProcessed() {
            return processed;
        }

        public String getRequestorUserId() {
            return requestorUserId;
        }

        public String getHandlingUserId() {
            return handlingUserId;
        }

        public String getBuildEngineUuid() {
            return buildEngineUuid;
        }

        public String getBuildResultUuid() {
            return buildResultUuid;
        }

        public String getBuildLabel() {
            return buildLabel;
        }

        public String getBuildState() {
            return buildState;
        }

        public String getBuildStatus() {
            return buildStatus;
        }

        public java.util.Date getBuildStartTime() {
            return buildStartTime;
        }

        public long getBuildTimeTakenMs() {
            return buildTimeTakenMs;
        }

        public IBuildResultHandle getBuildResultHandle() {
            return buildResultHandle;
        }
    }
}
