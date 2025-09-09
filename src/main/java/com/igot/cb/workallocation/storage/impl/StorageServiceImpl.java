package com.igot.cb.workallocation.storage.impl;

import com.igot.cb.workallocation.authentication.util.AccessTokenValidator;
import com.igot.cb.workallocation.storage.service.StorageService;
import com.igot.cb.workallocation.util.ApiResponse;
import com.igot.cb.workallocation.util.CbServerProperties;
import com.igot.cb.workallocation.util.Constants;
import com.igot.cb.workallocation.util.ProjectUtil;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import scala.Option;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Service
public class StorageServiceImpl implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageServiceImpl.class);

    @Autowired
    private AccessTokenValidator accessTokenValidator;

    private BaseStorageService storageService;

    @Autowired
    private CbServerProperties cbServerProperties;

    @Value("${html.store.path}")
    private String htmlFolderPath;

    @Value("${pdf.store.path}")
    private String pdfFolderPath;

    private VelocityEngine velocityEngine;

    @PostConstruct
    public void init() {
        if (storageService == null) {
            storageService = StorageServiceFactory.getStorageService(
                    new StorageConfig(
                            cbServerProperties.getCloudStorageTypeName(),
                            cbServerProperties.getCloudStorageKey(),
                            cbServerProperties.getCloudStorageSecret().replace("\\n", "\n"),
                            Option.apply(cbServerProperties.getCloudStorageEndpoint()),
                            Option.empty()
                    )
            );
        }
        Properties props = new Properties();
        props.setProperty("resource.loader", "class");
        props.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine = new VelocityEngine(props);
        velocityEngine.init();
    }

    /**
     * Upload file to GCP container after generating PDF from HTML templates.
     *
     * @param requestBody   Map containing request data including work order details.
     * @param authUserToken Authentication token of the user making the request.
     * @return ApiResponse containing the upload status and file URL if successful.
     */
    @Override
    public ApiResponse uploadFileToGCPContainer(Map<String, Object> requestBody, String authUserToken) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.PDF_UPLOAD_GCP_CONTAINER);
        String userId = accessTokenValidator.fetchUserIdFromAccessToken(authUserToken);
        if (StringUtils.isEmpty(userId)) {
            updateErrorDetails(response, Constants.USER_ID_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
            return response;
        }
        try {
            requestBody.put(Constants.USER_ID, userId);
            VelocityContext context = new VelocityContext();
            requestBody.forEach(context::put);
            String headerHtml = renderTemplate(Constants.TEMPLATES_HEADER_VM, context);
            String footerHtml = renderTemplate(Constants.TEMPLATES_FOOTER_VM, context);
            String contentHtml = renderTemplate(Constants.TEMPLATES_CONTENT_VM, context);
            String headerHtmlPath = createHTMLFile(Constants.HEADER, headerHtml);
            String footerHtmlPath = createHTMLFile(Constants.FOOTER, footerHtml);
            String contentHtmlPath = createHTMLFile("content", contentHtml);
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put(Constants.UD_HTML_FILE_PATH, contentHtmlPath);
            paramMap.put(Constants.UD_HTML_HEADER_FILE_PATH, headerHtmlPath);
            paramMap.put(Constants.UD_HTML_FOOTER_FILE_PATH, footerHtmlPath);
            String workOrderId = requestBody.getOrDefault("workOrderId", "default").toString();
            paramMap.put(Constants.UD_FILE_NAME, "WorkOrder_" + workOrderId + Constants.PDF);
            String pdfFilePath = makePdf(paramMap);
            return uploadFile(new File(pdfFilePath), cbServerProperties.getCloudFolderName(), cbServerProperties.getCloudContainerName());
        } catch (IOException e) {
            logger.error("Failed to generate or upload PDF", e);
            updateErrorDetails(response, "Failed to generate or upload PDF", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    /**
     * Render a Velocity template with the given context.
     *
     * @param templatePath Path to the Velocity template.
     * @param context      VelocityContext containing data for the template.
     * @return Rendered template as a String.
     * @throws IOException If an I/O error occurs during template rendering.
     */
    private String renderTemplate(String templatePath, VelocityContext context) throws IOException {
        StringWriter writer = new StringWriter();
        Template template = velocityEngine.getTemplate(templatePath, "UTF-8");
        template.merge(context, writer);
        return writer.toString();
    }

    /**
     * Update the ApiResponse object with error details.
     *
     * @param response     The ApiResponse object to be updated.
     * @param errorMessage The error message to be set.
     * @param status       The HTTP status code to be set.
     */
    public void updateErrorDetails(ApiResponse response, String errorMessage, HttpStatus status) {
        if (response != null && response.getParams() != null) {
            response.getParams().setErrMsg(errorMessage);
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(status);
        }
    }

    /**
     * Create an HTML file with the given content.
     *
     * @param fName       Name of the HTML file to be created.
     * @param htmlContent Content to be written into the HTML file.
     * @return Path to the created HTML file.
     * @throws IOException If an I/O error occurs during file creation or writing.
     */
    public String createHTMLFile(String fName, String htmlContent) throws IOException {
        String prefix = UUID.randomUUID().toString().toUpperCase() + "-" + System.currentTimeMillis();
        Path dirPath = Path.of(htmlFolderPath);
        Files.createDirectories(dirPath);
        String htmlFilePath = dirPath.resolve(prefix + "_" + fName + Constants.HTML).toString();
        try (BufferedWriter out = new BufferedWriter(new FileWriter(htmlFilePath))) {
            out.write(htmlContent);
        }
        logger.info("Html written successfully on: {}", htmlFilePath);
        return htmlFilePath;
    }

    /**
     * Generate PDF from HTML using wkhtmltopdf command line tool.
     *
     * @param paramMap Map containing parameters like HTML file path, header/footer paths, and output PDF file name.
     * @return Path to the generated PDF file.
     * @throws IOException If an I/O error occurs during PDF generation.
     */
    public String makePdf(Map<String, String> paramMap) throws IOException {
        String htmlFilePath = paramMap.get(Constants.UD_HTML_FILE_PATH);
        if (htmlFilePath == null) {
            logger.warn("HTML file path is null, cannot create PDF.");
            return null;
        }
        String pdfFileName = paramMap.get(Constants.UD_FILE_NAME);
        if (pdfFileName == null || !pdfFileName.endsWith(".pdf")) {
            pdfFileName = (pdfFileName == null ? "output" : pdfFileName) + ".pdf";
        }
        Path pdfDirPath = Path.of(pdfFolderPath);
        Files.createDirectories(pdfDirPath);
        String pdfFilePath = pdfDirPath.resolve(pdfFileName).toString();

        StringBuilder commandLine = new StringBuilder();
        commandLine.append("wkhtmltopdf --enable-local-file-access --margin-top 20.0 --margin-left 10.0 --margin-right 10.0 --footer-spacing 5 ");
        commandLine.append("--header-spacing 5 --footer-font-size 8 --orientation Portrait --page-size A4 ");
        commandLine.append("--load-media-error-handling ignore --no-header-line --no-footer-line --enable-forms ");
        commandLine.append("--load-error-handling ignore --header-right [page]/[toPage] ");
        commandLine.append("--minimum-font-size 11 --footer-html ").append(paramMap.get(Constants.UD_HTML_FOOTER_FILE_PATH));
        commandLine.append(" --header-html ").append(paramMap.get(Constants.UD_HTML_HEADER_FILE_PATH)).append(" ");
        commandLine.append(htmlFilePath).append(" ").append(pdfFilePath);
        logger.info("Saving the file content as PDF");
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(commandLine.toString());
            try (BufferedReader brCleanUp = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = brCleanUp.readLine()) != null) {
                    logger.info("PDF generation output: {}", line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("PDF generation failed with exit code {}", exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Exception occurred while writing the pdf file", e);
            Thread.currentThread().interrupt();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return pdfFilePath;
    }

    /**
     * Upload file to cloud storage and delete local file after successful upload.
     *
     * @param file            File to be uploaded
     * @param cloudFolderName Cloud folder name where the file will be uploaded
     * @param containerName   Cloud container name
     * @return ApiResponse containing the upload status and file URL if successful
     */
    public ApiResponse uploadFile(File file, String cloudFolderName, String containerName) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_FILE_UPLOAD);
        boolean uploadSuccess = false;
        try {
            if (file == null || !file.exists()) {
                response.getParams().setStatus(Constants.FAILED);
                response.getParams().setErrMsg("File does not exist.");
                response.setResponseCode(HttpStatus.BAD_REQUEST);
                return response;
            }
            String objectKey = String.format("%s/%s", cloudFolderName, file.getName());
            String url = storageService.upload(containerName, file.getAbsolutePath(),
                    objectKey, Option.apply(false), Option.apply(1), Option.apply(5), Option.empty());
            Map<String, String> uploadedFile = new HashMap<>();
            uploadedFile.put(Constants.NAME, file.getName());
            uploadedFile.put(Constants.URL, url);
            response.getResult().putAll(uploadedFile);
            uploadSuccess = true;
        } catch (Exception e) {
            logger.error("Failed to upload file. Exception: ", e);
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErrMsg("Failed to upload file. Exception: " + e.getMessage());
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (file != null && file.exists() && uploadSuccess) {
                try {
                    Files.delete(file.toPath());
                } catch (IOException ex) {
                    logger.warn("Failed to delete file: {}. Reason: {}", file.getAbsolutePath(), ex.getMessage());
                }
            }
        }
        return response;
    }
}
